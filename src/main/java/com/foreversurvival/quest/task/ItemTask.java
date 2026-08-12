package com.foreversurvival.quest.task;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * "Collect" task. Counts the matching items currently carried by the player
 * (inventory + armour + offhand).
 *
 * Because {@code PlayerQuestData} keeps the highest value ever recorded, the
 * objective stays complete even after the items are spent - you are never
 * punished for using what you gathered.
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
		ServerPlayerEntity player = ctx.getPlayer();
		int count = 0;

		for (int slot = 0; slot < player.getInventory().size(); slot++) {
			ItemStack stack = player.getInventory().getStack(slot);
			if (!stack.isEmpty() && items.contains(stack.getItem())) {
				count += stack.getCount();
			}
		}

		return count;
	}

	@Override
	public String getTypeLabel() {
		return "Collect";
	}
}
