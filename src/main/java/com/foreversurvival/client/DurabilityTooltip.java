package com.foreversurvival.client;

import java.util.List;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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

	private static void appendDurability(ItemStack stack, TooltipContext context, List<Text> lines) {
		if (!HudConfig.showDurability || stack.isEmpty() || !stack.isDamageable()) {
			return;
		}

		// Vanilla already prints this in advanced mode.
		if (context.isAdvanced()) {
			return;
		}

		int max = stack.getMaxDamage();
		int remaining = max - stack.getDamage();
		if (max <= 0) {
			return;
		}

		double fraction = remaining / (double) max;
		Formatting colour;
		if (fraction > 0.5D) {
			colour = Formatting.GREEN;
		} else if (fraction > 0.25D) {
			colour = Formatting.YELLOW;
		} else if (fraction > 0.1D) {
			colour = Formatting.GOLD;
		} else {
			colour = Formatting.RED;
		}

		lines.add(new LiteralText("Durability: ").formatted(Formatting.GRAY)
				.append(new LiteralText(remaining + " / " + max).formatted(colour)));
	}
}
