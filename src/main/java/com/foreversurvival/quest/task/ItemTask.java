package com.foreversurvival.quest.task;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

/**
 * "Collect" task. Progress is the higher of what the player is carrying
 * (inventory + armour + offhand) and what they have ever picked up.
 *
 * The carried count covers things that never touch the floor - smelting output,
 * chest loot - while the picked-up statistic covers everything gathered and then
 * spent. Between them you can never be stranded partway through a gathering
 * objective by using the materials as you go.
 *
 * When several items are supplied they are summed, so "any 16 logs" works with
 * a mixed stack of oak and birch.
 */
public class ItemTask extends QuestTask {

	private final Set<Item> items;

	public ItemTask(String id, String description, int required, Item... items) {
		super(id, description, required);
		this.items = new HashSet<>(Arrays.asList(items));
	}

	@Override
	public int computeProgress(TaskContext ctx) {
		ServerPlayer player = ctx.getPlayer();

		int carried = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (!stack.isEmpty() && items.contains(stack.getItem())) {
				carried += stack.getCount();
			}
		}

		// Carrying alone is not enough: spending the items as you gather them
		// (harvesting wheat while baking bread, say) means you might never hold
		// the full amount at once. The vanilla "picked up" statistic is
		// cumulative and never falls, so whichever is higher wins.
		int gathered = 0;
		for (Item item : items) {
			gathered += player.getStats().getValue(Stats.ITEM_PICKED_UP.get(item));
		}

		return Math.max(carried, gathered);
	}

	@Override
	public String getTypeLabel() {
		return "Collect";
	}
}
