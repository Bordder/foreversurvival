package com.foreversurvival.client;

import java.util.List;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

/**
 * Adds a plain "Durability: 57 / 100" line to any damageable item's tooltip.
 *
 * Vanilla only shows this with advanced tooltips turned on (F3+H), and even
 * then it is easy to miss - the durability bar alone does not tell you how many
 * swings you have left. The number is coloured by how much life is left so a
 * failing tool is obvious at a glance.
 *
 * Skipped entirely when advanced tooltips are on, so the line is never doubled.
 */
public final class DurabilityTooltip {

	private DurabilityTooltip() {
	}

	public static void register() {
		ItemTooltipCallback.EVENT.register(DurabilityTooltip::appendDurability);
	}

	private static void appendDurability(ItemStack stack, Item.TooltipContext tooltipContext,
			TooltipFlag context, List<Component> lines) {
		if (!HudConfig.showDurability || stack.isEmpty() || !stack.isDamageableItem()) {
			return;
		}

		// Vanilla already prints this in advanced mode.
		if (context.isAdvanced()) {
			return;
		}

		int max = stack.getMaxDamage();
		int remaining = max - stack.getDamageValue();
		if (max <= 0) {
			return;
		}

		double fraction = remaining / (double) max;
		ChatFormatting colour;
		if (fraction > 0.5D) {
			colour = ChatFormatting.GREEN;
		} else if (fraction > 0.25D) {
			colour = ChatFormatting.YELLOW;
		} else if (fraction > 0.1D) {
			colour = ChatFormatting.GOLD;
		} else {
			colour = ChatFormatting.RED;
		}

		lines.add(Component.literal("Durability: ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(remaining + " / " + max).withStyle(colour)));
	}
}
