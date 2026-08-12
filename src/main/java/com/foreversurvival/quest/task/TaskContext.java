package com.foreversurvival.quest.task;

import java.util.Set;

import com.foreversurvival.data.PlayerQuestData;

import net.minecraft.block.Block;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Everything a task needs in order to evaluate itself, gathered once per scan
 * tick so 50+ quests do not each re-walk the world.
 */
public class TaskContext {

	private final ServerPlayerEntity player;
	private final PlayerQuestData data;
	/** Distinct blocks found in a small box around the player this scan. */
	private final Set<Block> nearbyBlocks;
	/** Progress value already stored for the task being evaluated. */
	private int storedProgress;

	public TaskContext(ServerPlayerEntity player, PlayerQuestData data, Set<Block> nearbyBlocks) {
		this.player = player;
		this.data = data;
		this.nearbyBlocks = nearbyBlocks;
	}

	public ServerPlayerEntity getPlayer() {
		return player;
	}

	public PlayerQuestData getData() {
		return data;
	}

	public Set<Block> getNearbyBlocks() {
		return nearbyBlocks;
	}

	public int getStoredProgress() {
		return storedProgress;
	}

	public void setStoredProgress(int storedProgress) {
		this.storedProgress = storedProgress;
	}
}
