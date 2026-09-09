package com.foreversurvival.quest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.foreversurvival.ForeverSurvival;
import com.foreversurvival.data.PlayerQuestData;
import com.foreversurvival.data.QuestDataHolder;
import com.foreversurvival.network.ModNetworking;
import com.foreversurvival.quest.task.QuestTask;
import com.foreversurvival.quest.task.StructureTask;
import com.foreversurvival.quest.task.TaskContext;

import net.minecraft.world.level.block.Block;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.stats.Stats;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.text.LiteralText;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;

/**
 * The single source of truth for quest definitions and for evaluating progress.
 *
 * Everything is polled on a one-second cadence rather than hooked into a dozen
 * different events. That keeps detection consistent (a quest unlocked today
 * still credits what you did last week) and avoids mixing in mod-fragile hooks.
 */
public final class QuestManager {

	private static final QuestManager INSTANCE = new QuestManager();

	/** How often progress is re-evaluated, in ticks. */
	private static final int EVAL_INTERVAL = 20;
	/** Half-size of the block scan box used by StructureTask. */
	private static final int SCAN_RADIUS_HORIZONTAL = 12;
	private static final int SCAN_RADIUS_VERTICAL = 6;

	private final Map<String, Quest> quests = new LinkedHashMap<>();
	private final List<Quest> mainQuests = new ArrayList<>();
	private final List<Quest> sideQuests = new ArrayList<>();

	private int tickCounter;

	private QuestManager() {
	}

	public static QuestManager get() {
		return INSTANCE;
	}

	// ------------------------------------------------------------------
	// Registry
	// ------------------------------------------------------------------

	public void register(Quest quest) {
		if (quests.containsKey(quest.getId())) {
			throw new IllegalStateException("Duplicate quest id: " + quest.getId());
		}

		quests.put(quest.getId(), quest);
		if (quest.getPhase().isSide()) {
			sideQuests.add(quest);
		} else {
			mainQuests.add(quest);
		}
	}

	/**
	 * Adds a quest, replacing any existing one with the same id. Used by the
	 * JSON loader so a config file can override a built-in quest.
	 *
	 * Overriding a MAIN quest must declare its own "parent", otherwise the
	 * strict chain loses a link.
	 */
	public void registerOrReplace(Quest quest) {
		Quest existing = quests.remove(quest.getId());
		if (existing != null) {
			mainQuests.remove(existing);
			sideQuests.remove(existing);
			ForeverSurvival.LOGGER.info("Custom quest '{}' overrides a built-in quest", quest.getId());
		}

		register(quest);
	}

	/** Wipes every registered quest. Only used when reloading definitions. */
	public void clear() {
		quests.clear();
		mainQuests.clear();
		sideQuests.clear();
	}

	@Nullable
	public Quest getQuest(String id) {
		return quests.get(id);
	}

	public Collection<Quest> getAllQuests() {
		return quests.values();
	}

	public List<Quest> getMainQuests() {
		return mainQuests;
	}

	public List<Quest> getSideQuests() {
		return sideQuests;
	}

	// ------------------------------------------------------------------
	// Locking
	// ------------------------------------------------------------------

	/**
	 * Strict linear locking: a quest is only reachable once its parent is
	 * completely finished. Side quests have no parent and are always open.
	 * This is enforced on the server too, so nothing can be skipped.
	 */
	public boolean isUnlocked(PlayerQuestData data, Quest quest) {
		String parentId = quest.getParentId();
		if (parentId == null) {
			return true;
		}
		return data.isCompleted(parentId);
	}

	public boolean isTaskComplete(PlayerQuestData data, Quest quest, QuestTask task) {
		return data.getProgress(quest.getId(), task.getId()) >= task.getRequired();
	}

	public boolean areAllTasksComplete(PlayerQuestData data, Quest quest) {
		for (QuestTask task : quest.getTasks()) {
			if (!isTaskComplete(data, quest, task)) {
				return false;
			}
		}
		return true;
	}

	// ------------------------------------------------------------------
	// Evaluation loop
	// ------------------------------------------------------------------

	public void tick(MinecraftServer server) {
		tickCounter++;
		if (tickCounter % EVAL_INTERVAL != 0) {
			return;
		}

		for (ServerPlayer player : server.getPlayerManager().getPlayerList()) {
			try {
				evaluate(player);
			} catch (Exception e) {
				ForeverSurvival.LOGGER.error("Failed to evaluate quests for {}", player.getGameProfile().getName(), e);
			}

			PlayerQuestData data = QuestDataHolder.get(player);
			if (data.isDirty()) {
				ModNetworking.syncToClient(player);
			}
		}
	}

	private void evaluate(ServerPlayer player) {
		PlayerQuestData data = QuestDataHolder.get(player);

		// 1. Collect the quests actually in play. The main line is a strict chain,
		// so exactly one main quest is ever active - the first incomplete one,
		// whose parent is by definition already done. Everything after it is
		// locked, so stop there instead of walking all 127. Side quests carry no
		// parent, so every incomplete one is live.
		List<Quest> active = new ArrayList<>();
		for (Quest quest : mainQuests) {
			if (!data.isCompleted(quest.getId())) {
				active.add(quest);
				break;
			}
		}
		for (Quest quest : sideQuests) {
			if (!data.isCompleted(quest.getId())) {
				active.add(quest);
			}
		}
		if (active.isEmpty()) {
			return;
		}

		// 2. One shared world scan for every StructureTask that needs one.
		Set<Block> wanted = new HashSet<>();
		for (Quest quest : active) {
			for (QuestTask task : quest.getTasks()) {
				if (task instanceof StructureTask structureTask) {
					wanted.addAll(structureTask.getBlocks());
				}
			}
		}
		Set<Block> nearby = wanted.isEmpty() ? new HashSet<>() : scanNearbyBlocks(player, wanted);

		// 3. Evaluate, then complete anything that just finished.
		TaskContext ctx = new TaskContext(player, data, nearby);
		for (Quest quest : active) {
			for (QuestTask task : quest.getTasks()) {
				int stored = data.getProgress(quest.getId(), task.getId());
				if (stored >= task.getRequired()) {
					continue;
				}

				ctx.setStoredProgress(stored);
				int computed = Math.min(task.getRequired(), task.computeProgress(ctx));
				if (computed > stored) {
					data.setProgress(quest.getId(), task.getId(), computed);
				}
			}

			if (areAllTasksComplete(data, quest)) {
				completeQuest(player, data, quest);
			}
		}
	}

	/**
	 * Walks a small box around the player and returns which of the wanted
	 * blocks are present. Runs at most once per second per player.
	 */
	private Set<Block> scanNearbyBlocks(ServerPlayer player, Set<Block> wanted) {
		Set<Block> found = new HashSet<>();
		BlockPos origin = player.getBlockPos();
		BlockPos.Mutable cursor = new BlockPos.Mutable();

		int minY = Math.max(player.world.getBottomY(), origin.getY() - SCAN_RADIUS_VERTICAL);
		int maxY = Math.min(player.world.getTopY() - 1, origin.getY() + SCAN_RADIUS_VERTICAL);

		for (int dx = -SCAN_RADIUS_HORIZONTAL; dx <= SCAN_RADIUS_HORIZONTAL; dx++) {
			for (int dz = -SCAN_RADIUS_HORIZONTAL; dz <= SCAN_RADIUS_HORIZONTAL; dz++) {
				for (int y = minY; y <= maxY; y++) {
					cursor.set(origin.getX() + dx, y, origin.getZ() + dz);
					Block block = player.world.getBlockState(cursor).getBlock();
					if (wanted.contains(block)) {
						found.add(block);
						if (found.size() == wanted.size()) {
							return found;
						}
					}
				}
			}
		}

		return found;
	}

	// ------------------------------------------------------------------
	// Completion / rewards / celebration
	// ------------------------------------------------------------------

	private void completeQuest(ServerPlayer player, PlayerQuestData data, Quest quest) {
		if (data.isCompleted(quest.getId())) {
			return;
		}

		data.setCompleted(quest.getId());
		data.setCompletionPlayTicks(quest.getId(),
				player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME)));

		// No items, no XP: the mod tells you what to do, vanilla does the rest.
		player.sendMessage(new Component("[ForeverSurvival] ").formatted(ChatFormatting.DARK_AQUA)
				.append(new Component("Quest complete: ").formatted(ChatFormatting.GREEN))
				.append(new Component(quest.getTitle()).formatted(ChatFormatting.YELLOW)), false);

		// Small celebration: action bar line plus a brief puff of sparks.
		player.sendMessage(new Component("✔ ").formatted(ChatFormatting.GREEN)
				.append(new Component(quest.getTitle()).formatted(ChatFormatting.WHITE)), true);
		spawnSparks(player);

		checkPhaseCompletion(player, data, quest.getPhase());
	}

	/** Fires once per phase, the moment its last quest is signed off. */
	private void checkPhaseCompletion(ServerPlayer player, PlayerQuestData data, QuestPhase phase) {
		if (phase.isSide() || data.hasCelebrated(phase.getId())) {
			return;
		}

		for (Quest quest : quests.values()) {
			if (quest.getPhase() == phase && !data.isCompleted(quest.getId())) {
				return;
			}
		}

		data.setCelebrated(phase.getId());

		player.sendMessage(new Component("=== " + phase.getDisplayName() + " COMPLETE ===")
				.formatted(phase.getColor(), ChatFormatting.BOLD), false);

		// Big celebration: a title card, and fireworks for the major phases.
		player.networkHandler.sendPacket(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
		player.networkHandler.sendPacket(new ClientboundSetSubtitleTextPacket(
				new Component(phase.getDisplayName()).formatted(phase.getColor())));
		player.networkHandler.sendPacket(new ClientboundSetTitleTextPacket(
				new Component("PHASE COMPLETE").formatted(ChatFormatting.GOLD, ChatFormatting.BOLD)));

		if (phase.isMajor()) {
			spawnCelebration(player);
		}
	}

	/** A handful of sparks for a single quest - deliberately understated. */
	private void spawnSparks(ServerPlayer player) {
		if (!(player.world instanceof ServerLevel world)) {
			return;
		}

		world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
				player.getX(), player.getY() + 1.2D, player.getZ(),
				12, 0.6D, 0.6D, 0.6D, 0.02D);
	}

	/**
	 * Pure visual celebration: firework particles around the player, no sound,
	 * no entity, no lasting marker of any kind.
	 */
	private void spawnCelebration(ServerPlayer player) {
		if (!(player.world instanceof ServerLevel world)) {
			return;
		}

		for (int burst = 0; burst < 4; burst++) {
			double offsetY = 0.5D + burst * 0.6D;
			world.spawnParticles(ParticleTypes.FIREWORK,
					player.getX(), player.getY() + offsetY, player.getZ(),
					60, 1.6D, 0.8D, 1.6D, 0.12D);
		}
	}

	// ------------------------------------------------------------------
	// Manual objectives
	// ------------------------------------------------------------------

	/**
	 * Ticks or un-ticks a manual objective. Re-validates everything server side
	 * so a crafted packet cannot tick off a locked quest.
	 *
	 * @param set true to mark done, false to undo a misclick
	 */
	public void handleCheckmark(ServerPlayer player, String questId, String taskId, boolean set) {
		Quest quest = getQuest(questId);
		if (quest == null) {
			return;
		}

		PlayerQuestData data = QuestDataHolder.get(player);

		// A finished quest is final: undoing part of it would strand the chain.
		if (data.isCompleted(questId) || !isUnlocked(data, quest)) {
			return;
		}

		for (QuestTask task : quest.getTasks()) {
			if (task.getId().equals(taskId) && task.isManual()) {
				if (set) {
					data.setProgress(questId, taskId, task.getRequired());
					if (areAllTasksComplete(data, quest)) {
						completeQuest(player, data, quest);
					}
				} else {
					data.clearProgress(questId, taskId);
				}

				ModNetworking.syncToClient(player);
				return;
			}
		}
	}
}
