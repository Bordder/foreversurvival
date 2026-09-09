package com.foreversurvival.network;

import java.util.ArrayList;
import java.util.List;

import com.foreversurvival.ForeverSurvival;
import com.foreversurvival.data.PlayerQuestData;
import com.foreversurvival.data.QuestDataHolder;
import com.foreversurvival.quest.QuestManager;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.resources.Identifier;

/**
 * Three packets:
 *  - S2C sync: the player's whole {@link PlayerQuestData} as NBT,
 *  - S2C locations: every online player's position, for the locator bar,
 *  - C2S checkmark: "I finished this manual objective".
 */
public final class ModNetworking {

	private ModNetworking() {
	}

	/**
	 * Payload types must be registered on both sides before any send, so this
	 * runs from the common initialiser rather than the server one.
	 */
	public static void registerPayloads() {
		PayloadTypeRegistry.clientboundPlay().register(
				ModPayloads.SyncData.TYPE, ModPayloads.SyncData.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(
				ModPayloads.PlayerLocations.TYPE, ModPayloads.PlayerLocations.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(
				ModPayloads.Checkmark.TYPE, ModPayloads.Checkmark.CODEC);
	}

	public static void registerServerReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(ModPayloads.Checkmark.TYPE,
				(payload, context) ->
						// The handler already runs on the server thread in 26.2,
						// but the lock check inside handleCheckmark still applies.
						context.server().execute(() -> QuestManager.get().handleCheckmark(
								context.player(), payload.questId(), payload.taskId(),
								payload.set())));
	}

	/** Pushes the full quest state to its owner. Progress stays per-player. */
	public static void syncToClient(ServerPlayer player) {
		PlayerQuestData data = QuestDataHolder.get(player);

		// Vanilla stats only reach the client when it explicitly asks for them,
		// so the handful the Stats tab shows ride along with the quest sync.
		CompoundTag root = new CompoundTag();
		root.put("Data", data.writeNbt());
		root.put("Stats", buildStats(player));

		ServerPlayNetworking.send(player, new ModPayloads.SyncData(root));
		data.clearDirty();
	}

	private static CompoundTag buildStats(ServerPlayer player) {
		var handler = player.getStats();
		CompoundTag stats = new CompoundTag();

		stats.putInt("PlayTime", handler.getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)));
		stats.putInt("SinceDeath", handler.getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH)));
		stats.putInt("WalkCm", handler.getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM)));
		stats.putInt("SprintCm", handler.getValue(Stats.CUSTOM.get(Stats.SPRINT_ONE_CM)));
		stats.putInt("FlyCm", handler.getValue(Stats.CUSTOM.get(Stats.FLY_ONE_CM)));
		stats.putInt("Deaths", handler.getValue(Stats.CUSTOM.get(Stats.DEATHS)));
		stats.putInt("MobKills", handler.getValue(Stats.CUSTOM.get(Stats.MOB_KILLS)));
		stats.putInt("Jumps", handler.getValue(Stats.CUSTOM.get(Stats.JUMP)));
		stats.putInt("DamageTaken", handler.getValue(Stats.CUSTOM.get(Stats.DAMAGE_TAKEN)));
		stats.putInt("Slept", handler.getValue(Stats.CUSTOM.get(Stats.SLEEP_IN_BED)));

		return stats;
	}

	/**
	 * Broadcasts every online player's position to every online player.
	 *
	 * Clients filter themselves out. Sent a few times a second - the bar only
	 * needs a direction, not a smooth interpolation.
	 */
	public static void syncPlayerLocations(MinecraftServer server) {
		List<ServerPlayer> players = server.getPlayerList().getPlayers();
		if (players.size() < 2) {
			// Nothing worth drawing when you are the only one online.
			return;
		}

		// One immutable payload now serves every recipient: unlike a byte buffer
		// a record is not released on send, so it is safe to reuse.
		List<ModPayloads.PlayerLocations.Entry> entries = new ArrayList<>(players.size());
		for (ServerPlayer player : players) {
			entries.add(new ModPayloads.PlayerLocations.Entry(
					player.getGameProfile().getName(),
					player.getX(), player.getY(), player.getZ(),
					player.level().dimension().identifier().toString()));
		}

		ModPayloads.PlayerLocations payload = new ModPayloads.PlayerLocations(entries);
		for (ServerPlayer recipient : players) {
			ServerPlayNetworking.send(recipient, payload);
		}
	}
}
