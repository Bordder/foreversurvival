package com.foreversurvival.quest.task;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.level().item.Item;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

/**
 * Tracks the vanilla "used" statistic for an item.
 *
 * Vanilla counts a use whenever an item is right-clicked to do its job, which
 * makes this the honest way to detect a lot of actions that otherwise needed a
 * manual tick: tilling with a hoe, planting a sapling, saddling a horse,
 * striking a flint and steel, emptying a bucket.
 *
 * Several items may be supplied and their counts are summed, so "till with any
 * hoe" works whichever tier you are holding.
 */
public class UseTask extends QuestTask {

	private final Set<Item> items;

	public UseTask(String id, String description, int required, Item... items) {
		super(id, description, required);
		this.items = new LinkedHashSet<>(Arrays.asList(items));
	}

	@Override
	public int computeProgress(TaskContext ctx) {
		ServerPlayer player = ctx.getPlayer();
		int total = 0;

		for (Item item : items) {
			total += player.getStats().getStat(Stats.USED.get(item));
		}
		return total;
	}

	@Override
	public String getTypeLabel() {
		return "Use";
	}
}
