package com.foreversurvival.client;

import org.jetbrains.annotations.Nullable;

import com.foreversurvival.data.PlayerQuestData;
import com.foreversurvival.quest.Quest;
import com.foreversurvival.quest.QuestManager;

import net.minecraft.nbt.CompoundTag;

/**
 * Client-side mirror of the player's server data, refreshed by the sync packet.
 * The quest definitions themselves are already present on the client (they are
 * plain static code), so only progress ever travels over the wire.
 */
public final class ClientQuestState {

	private static final PlayerQuestData DATA = new PlayerQuestData();

	private ClientQuestState() {
	}

	public static PlayerQuestData get() {
		return DATA;
	}

	private static CompoundTag stats = new CompoundTag();

	public static void accept(CompoundTag root) {
		DATA.readNbt(root.getCompound("Data"));
		stats = root.getCompound("Stats");
	}

	/** Vanilla stats snapshot that rode along with the last sync. */
	public static CompoundTag getStats() {
		return stats;
	}

	/**
	 * First unlocked, incomplete main quest - the one thing you can do next.
	 * Returns null once the whole main line is finished.
	 */
	@Nullable
	public static Quest getCurrentMainQuest() {
		for (Quest quest : QuestManager.get().getMainQuests()) {
			if (!DATA.isCompleted(quest.getId()) && QuestManager.get().isUnlocked(DATA, quest)) {
				return quest;
			}
		}
		return null;
	}
}
