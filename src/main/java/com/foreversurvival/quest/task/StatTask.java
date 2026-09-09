package com.foreversurvival.quest.task;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.resources.Identifier;

/**
 * Tracks one of the vanilla "custom" statistics - the counters behind the
 * Statistics screen's General tab.
 *
 * This is what replaced most of the old manual checkmarks: sleeping in a bed,
 * breeding animals, trading with a villager, winning a raid, enchanting an item,
 * using an anvil or a brewing stand, and so on are all things the game already
 * counts, so the mod can just read them instead of asking you to confirm.
 *
 * The divisor exists for the distance counters, which vanilla stores in
 * centimetres - pass 100 to work in blocks.
 */
public class StatTask extends QuestTask {

	private final Identifier stat;
	private final int divisor;

	public StatTask(String id, String description, int required, Identifier stat) {
		this(id, description, required, stat, 1);
	}

	public StatTask(String id, String description, int required, Identifier stat, int divisor) {
		super(id, description, required);
		this.stat = stat;
		this.divisor = Math.max(1, divisor);
	}

	@Override
	public int computeProgress(TaskContext ctx) {
		ServerPlayer player = ctx.getPlayer();
		return player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(stat)) / divisor;
	}

	@Override
	public String getTypeLabel() {
		return "Do";
	}
}
