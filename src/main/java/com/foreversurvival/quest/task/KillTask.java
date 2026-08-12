package com.foreversurvival.quest.task;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;

/**
 * "Kill" task, backed by the vanilla {@code minecraft.killed} statistic so only
 * kills actually credited to the player count (no leeching off other players'
 * mobs, no counting mobs that died to fall damage on their own).
 */
public class KillTask extends QuestTask {

	private final Set<EntityType<?>> types;

	public KillTask(String id, String description, int required, EntityType<?>... types) {
		super(id, description, required);
		this.types = new LinkedHashSet<>(Arrays.asList(types));
	}

	@Override
	public int computeProgress(TaskContext ctx) {
		ServerPlayerEntity player = ctx.getPlayer();
		int total = 0;

		for (EntityType<?> type : types) {
			total += player.getStatHandler().getStat(Stats.KILLED.getOrCreateStat(type));
		}

		return total;
	}

	@Override
	public String getTypeLabel() {
		return "Kill";
	}
}
