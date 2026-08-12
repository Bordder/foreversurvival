package com.foreversurvival.quest.task;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Tracks where the player has actually been.
 *
 * Replaces the manual ticks for things the world can answer on its own: getting
 * down to bedrock, climbing to a mountain peak, or travelling a given distance
 * from world spawn. Progress is sticky, so the deepest or furthest you have
 * ever been is what counts.
 */
public class PositionTask extends QuestTask {

	public enum Kind {
		/** Done once the player's Y is at or below the target. */
		BELOW_Y,
		/** Done once the player's Y is at or above the target. */
		ABOVE_Y,
		/** Progress is the straight-line distance from world spawn, in blocks. */
		FROM_SPAWN
	}

	private final Kind kind;
	private final int target;

	public PositionTask(String id, String description, Kind kind, int target) {
		super(id, description, kind == Kind.FROM_SPAWN ? target : 1);
		this.kind = kind;
		this.target = target;
	}

	@Override
	public int computeProgress(TaskContext ctx) {
		ServerPlayerEntity player = ctx.getPlayer();

		switch (kind) {
			case BELOW_Y:
				return player.getBlockY() <= target ? 1 : 0;
			case ABOVE_Y:
				return player.getBlockY() >= target ? 1 : 0;
			default:
				break;
		}

		if (!(player.world instanceof ServerWorld world)) {
			return 0;
		}

		// Horizontal distance only - height should not count towards a journey.
		BlockPos spawn = world.getSpawnPos();
		double dx = player.getX() - spawn.getX();
		double dz = player.getZ() - spawn.getZ();

		return (int) Math.sqrt(dx * dx + dz * dz);
	}

	@Override
	public String getTypeLabel() {
		return "Reach";
	}
}
