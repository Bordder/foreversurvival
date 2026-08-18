package com.foreversurvival.quest.task;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.registry.Registry;

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
		ServerPlayerEntity player = ctx.getPlayer();
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
			NbtCompound nbt = stack.getNbt();
			if (nbt != null && nbt.contains("StoredEnchantments", NbtElement.LIST_TYPE)) {
				NbtList stored = nbt.getList("StoredEnchantments", NbtElement.COMPOUND_TYPE);
				for (int i = 0; i < stored.size(); i++) {
					NbtCompound entry = stored.getCompound(i);
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
