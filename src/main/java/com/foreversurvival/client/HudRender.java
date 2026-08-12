package com.foreversurvival.client;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.util.math.MatrixStack;

/**
 * Shared transform helper for the HUD elements.
 *
 * Item rendering in 1.18.2 goes through {@code RenderSystem.getModelViewStack()}
 * rather than the MatrixStack handed to the render callback, so scaling has to
 * be applied there for the quest icon to scale along with the text.
 */
public final class HudRender {

	private HudRender() {
	}

	/** Translates to (x, y) and scales, so callers can draw from the origin. */
	public static void push(double x, double y, double scale) {
		MatrixStack model = RenderSystem.getModelViewStack();
		model.push();
		model.translate(x, y, 0.0D);
		model.scale((float) scale, (float) scale, 1.0F);
		RenderSystem.applyModelViewMatrix();
	}

	public static void pop() {
		RenderSystem.getModelViewStack().pop();
		RenderSystem.applyModelViewMatrix();
	}
}
