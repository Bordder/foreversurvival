package com.foreversurvival.quest.task;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Registry;

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

	private final Set<Enchantment> enchants;
	private final int minLevel;

	public EnchantTask(String id, String description, int required, int minLevel,
			Enchantment... enchants) {
		super(id, description, required);
		this.minLevel = Math.max(1, minLevel);
		this.enchants = new LinkedHashSet<>(Arrays.asList(enchants));
	}

	@Override
	public int computeProgress(TaskContext ctx) {
		ServerPlayer player = ctx.getPlayer();
		int count = 0;

		for (int slot = 0; slot < player.getInventory().size(); slot++) {
			ItemStack stack = player.getInventory().getStack(slot);
			if (!stack.isEmpty() && hasWantedEnchant(stack)) {
				count++;
			}
		}
		return count;
	}

	private boolean hasWantedEnchant(ItemStack stack) {
		for (Enchantment enchant : enchants) {
			if (EnchantmentHelper.getLevel(enchant, stack) >= minLevel) {
				return true;
			}
		}

		// Enchanted books keep their enchantments under StoredEnchantments, which
		// EnchantmentHelper.getLevel does not read - check that list directly.
		if (stack.isOf(Items.ENCHANTED_BOOK) && stack.hasNbt()) {
			CompoundTag nbt = stack.getNbt();
			if (nbt != null && nbt.contains("StoredEnchantments", Tag.LIST_TYPE)) {
				ListTag stored = nbt.getList("StoredEnchantments", Tag.COMPOUND_TYPE);
				for (int i = 0; i < stored.size(); i++) {
					CompoundTag entry = stored.getCompound(i);
					int level = entry.getInt("lvl");
					for (Enchantment enchant : enchants) {
						String id = String.valueOf(Registry.ENCHANTMENT.getId(enchant));
						if (id.equals(entry.getString("id")) && level >= minLevel) {
							return true;
						}
					}
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
