package com.foreversurvival.quest.task;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.world.item.Item;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

/**
 * "Craft" task.
 *
 * Reads the vanilla {@code minecraft.crafted} statistic, which the game already
 * increments for every crafting-table, inventory-grid, stonecutter and smithing
 * output. Using the stat instead of a bespoke event means the detection cannot
 * be desynced by other mods and works retroactively for anything crafted before
 * the quest unlocked.
 */
public class CraftTask extends QuestTask {

	private final Set<Item> items;

	public CraftTask(String id, String description, int required, Item... items) {
		super(id, description, required);
		this.items = new LinkedHashSet<>(Arrays.asList(items));
	}

	@Override
	public int computeProgress(TaskContext ctx) {
		ServerPlayer player = ctx.getPlayer();
		int total = 0;

		for (Item item : items) {
			total += player.getStats().getValue(Stats.ITEM_CRAFTED.get(item));
		}

		return total;
	}

	@Override
	public String getTypeLabel() {
		return "Craft";
	}
}
