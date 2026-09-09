package com.foreversurvival.quest.task;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Counts items the player is carrying that bear one of the wanted enchantments
 * at or above a minimum level. Any of the listed enchantments counts, so
 * "Mending or Unbreaking" works with one task.
 *
 * Detects both APPLIED enchantments (on a tool, weapon or armour) and STORED
 * enchantments (on an enchanted book), so "obtain a Mending book" and "get a
 * Fortune III pickaxe" both resolve without a manual tick.
 */
public class EnchantTask extends QuestTask {

	private final Set<ResourceKey<Enchantment>> enchants;
	private final int minLevel;

	@SafeVarargs
	public EnchantTask(String id, String description, int required, int minLevel,
			ResourceKey<Enchantment>... enchants) {
		super(id, description, required);
		this.minLevel = Math.max(1, minLevel);
		this.enchants = new LinkedHashSet<>(Arrays.asList(enchants));
	}

	@Override
	public int computeProgress(TaskContext ctx) {
		ServerPlayer player = ctx.getPlayer();
		int count = 0;

		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (!stack.isEmpty() && hasWantedEnchant(stack)) {
				count++;
			}
		}
		return count;
	}

	/**
	 * 26.2 keeps enchantments in item components rather than NBT, and an
	 * enchanted book still keeps its own under STORED_ENCHANTMENTS - so both
	 * components are checked rather than special-casing the book item.
	 */
	private boolean hasWantedEnchant(ItemStack stack) {
		return matches(stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY))
				|| matches(stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS,
						ItemEnchantments.EMPTY));
	}

	private boolean matches(ItemEnchantments present) {
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : present.entrySet()) {
			if (entry.getIntValue() < minLevel) {
				continue;
			}
			for (ResourceKey<Enchantment> wanted : enchants) {
				// Comparing by key avoids needing registry access here.
				if (entry.getKey().is(wanted)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String getTypeLabel() {
		return "Enchant";
	}
}
