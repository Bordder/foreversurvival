package com.foreversurvival.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

/**
 * Everything the mod remembers about a single player.
 *
 * This object lives on the {@code ServerPlayer} (injected by
 * {@code ServerPlayerEntityMixin}) and is written straight into the player's
 * own NBT, so it survives logout, death and dimension changes without any
 * extra world save data.
 *
 * The exact same class is reused on the client, where it is filled from the
 * sync packet instead of from disk.
 */
public class PlayerQuestData {

	public static final int MAX_DEATH_RECORDS = 200;

	/**
	 * Only the most recent deaths keep their inventory snapshot. Coordinates are
	 * a few bytes; 41 slots of item NBT is not, and this all lives in player NBT.
	 */
	public static final int MAX_INVENTORY_SNAPSHOTS = 25;

	/** IDs of quests that are 100% finished (and whose rewards were paid out). */
	private final Set<String> completedQuests = new HashSet<>();

	/**
	 * questId -> the player's PLAY_TIME (ticks played) at the moment it was
	 * completed. Play-time, not wall-clock, so idle/offline time never inflates
	 * how long a quest "took". The gap to the previous quest in the chain is the
	 * real time spent on it.
	 */
	private final Map<String, Integer> completionPlayTicks = new HashMap<>();

	/** questId -> (taskId -> progress). Progress is sticky: it never goes down. */
	private final Map<String, Map<String, Integer>> progress = new HashMap<>();

	/** Phases whose firework celebration has already been played once. */
	private final Set<String> celebratedPhases = new HashSet<>();

	/** Death log, oldest first. */
	private final List<DeathRecord> deaths = new ArrayList<>();

	/** entityTypeId -> total raw damage this player has dealt to that mob type. */
	private final Map<String, Float> damageDealt = new HashMap<>();

	/** Server-side only: set when something changed and the client needs a resync. */
	private transient boolean dirty = true;

	// ------------------------------------------------------------------
	// Quest completion
	// ------------------------------------------------------------------

	public boolean isCompleted(String questId) {
		return completedQuests.contains(questId);
	}

	public void setCompleted(String questId) {
		if (completedQuests.add(questId)) {
			markDirty();
		}
	}

	public Set<String> getCompletedQuests() {
		return completedQuests;
	}

	/** Records the play-time stamp for a quest the first time it completes. */
	public void setCompletionPlayTicks(String questId, int playTicks) {
		if (!completionPlayTicks.containsKey(questId)) {
			completionPlayTicks.put(questId, playTicks);
			markDirty();
		}
	}

	/** @return play-ticks at completion, or -1 if never recorded. */
	public int getCompletionPlayTicks(String questId) {
		return completionPlayTicks.getOrDefault(questId, -1);
	}

	// ------------------------------------------------------------------
	// Task progress
	// ------------------------------------------------------------------

	public int getProgress(String questId, String taskId) {
		Map<String, Integer> tasks = progress.get(questId);
		if (tasks == null) {
			return 0;
		}
		return tasks.getOrDefault(taskId, 0);
	}

	/**
	 * Wipes a single task's progress. Only used to un-tick a manual objective -
	 * automatic tasks would just re-detect on the next poll anyway.
	 */
	public void clearProgress(String questId, String taskId) {
		Map<String, Integer> tasks = progress.get(questId);
		if (tasks != null && tasks.remove(taskId) != null) {
			markDirty();
		}
	}

	/**
	 * Stores progress but never lets it shrink. This is what makes item based
	 * tasks fair: once you have held 16 logs, spending them again does not undo
	 * the objective.
	 */
	public void setProgress(String questId, String taskId, int value) {
		Map<String, Integer> tasks = progress.computeIfAbsent(questId, k -> new LinkedHashMap<>());
		int old = tasks.getOrDefault(taskId, 0);
		if (value > old) {
			tasks.put(taskId, value);
			markDirty();
		}
	}

	// ------------------------------------------------------------------
	// Phase celebrations
	// ------------------------------------------------------------------

	public boolean hasCelebrated(String phaseId) {
		return celebratedPhases.contains(phaseId);
	}

	public void setCelebrated(String phaseId) {
		if (celebratedPhases.add(phaseId)) {
			markDirty();
		}
	}

	// ------------------------------------------------------------------
	// Death log
	// ------------------------------------------------------------------

	public List<DeathRecord> getDeaths() {
		return deaths;
	}

	public void addDeath(DeathRecord record) {
		deaths.add(record);

		// Keep the list bounded so a very long world does not bloat player NBT.
		while (deaths.size() > MAX_DEATH_RECORDS) {
			deaths.remove(0);
		}

		// Older deaths keep their coordinates but lose the inventory snapshot.
		int dropBefore = deaths.size() - MAX_INVENTORY_SNAPSHOTS;
		for (int i = 0; i < dropBefore; i++) {
			deaths.get(i).clearInventory();
		}

		markDirty();
	}

	// ------------------------------------------------------------------
	// Combat log
	// ------------------------------------------------------------------

	public void addDamageDealt(String entityTypeId, float amount) {
		if (amount <= 0) {
			return;
		}
		damageDealt.merge(entityTypeId, amount, Float::sum);
		markDirty();
	}

	/** Live view: entityTypeId -> total raw damage dealt. */
	public Map<String, Float> getDamageDealt() {
		return damageDealt;
	}

	// ------------------------------------------------------------------
	// Dirty flag (server side sync throttling)
	// ------------------------------------------------------------------

	public boolean isDirty() {
		return dirty;
	}

	public void markDirty() {
		this.dirty = true;
	}

	public void clearDirty() {
		this.dirty = false;
	}

	// ------------------------------------------------------------------
	// Persistence
	// ------------------------------------------------------------------

	public CompoundTag writeNbt() {
		CompoundTag root = new CompoundTag();

		ListTag completedList = new ListTag();
		for (String id : completedQuests) {
			completedList.add(StringTag.of(id));
		}
		root.put("Completed", completedList);

		CompoundTag progressNbt = new CompoundTag();
		for (Map.Entry<String, Map<String, Integer>> questEntry : progress.entrySet()) {
			CompoundTag taskNbt = new CompoundTag();
			for (Map.Entry<String, Integer> taskEntry : questEntry.getValue().entrySet()) {
				taskNbt.putInt(taskEntry.getKey(), taskEntry.getValue());
			}
			progressNbt.put(questEntry.getKey(), taskNbt);
		}
		root.put("Progress", progressNbt);

		ListTag celebratedList = new ListTag();
		for (String id : celebratedPhases) {
			celebratedList.add(StringTag.of(id));
		}
		root.put("Celebrated", celebratedList);

		ListTag deathList = new ListTag();
		for (DeathRecord record : deaths) {
			deathList.add(record.writeNbt());
		}
		root.put("Deaths", deathList);

		CompoundTag timesNbt = new CompoundTag();
		for (Map.Entry<String, Integer> entry : completionPlayTicks.entrySet()) {
			timesNbt.putInt(entry.getKey(), entry.getValue());
		}
		root.put("CompletionTimes", timesNbt);

		CompoundTag damageNbt = new CompoundTag();
		for (Map.Entry<String, Float> entry : damageDealt.entrySet()) {
			damageNbt.putFloat(entry.getKey(), entry.getValue());
		}
		root.put("DamageDealt", damageNbt);

		return root;
	}

	public void readNbt(CompoundTag root) {
		completedQuests.clear();
		progress.clear();
		celebratedPhases.clear();
		deaths.clear();
		completionPlayTicks.clear();
		damageDealt.clear();

		ListTag completedList = root.getList("Completed", Tag.STRING_TYPE);
		for (int i = 0; i < completedList.size(); i++) {
			completedQuests.add(completedList.getString(i));
		}

		CompoundTag progressNbt = root.getCompound("Progress");
		for (String questId : progressNbt.getKeys()) {
			CompoundTag taskNbt = progressNbt.getCompound(questId);
			Map<String, Integer> tasks = new LinkedHashMap<>();
			for (String taskId : taskNbt.getKeys()) {
				tasks.put(taskId, taskNbt.getInt(taskId));
			}
			progress.put(questId, tasks);
		}

		ListTag celebratedList = root.getList("Celebrated", Tag.STRING_TYPE);
		for (int i = 0; i < celebratedList.size(); i++) {
			celebratedPhases.add(celebratedList.getString(i));
		}

		ListTag deathList = root.getList("Deaths", Tag.COMPOUND_TYPE);
		for (int i = 0; i < deathList.size(); i++) {
			deaths.add(DeathRecord.fromNbt(deathList.getCompound(i)));
		}

		CompoundTag timesNbt = root.getCompound("CompletionTimes");
		for (String questId : timesNbt.getKeys()) {
			completionPlayTicks.put(questId, timesNbt.getInt(questId));
		}

		CompoundTag damageNbt = root.getCompound("DamageDealt");
		for (String typeId : damageNbt.getKeys()) {
			damageDealt.put(typeId, damageNbt.getFloat(typeId));
		}

		markDirty();
	}

	/** Used by {@code ServerPlayerEvents.COPY_FROM} so nothing is lost on respawn. */
	public void copyFrom(PlayerQuestData other) {
		readNbt(other.writeNbt());
	}
}
