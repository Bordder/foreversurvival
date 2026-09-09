package com.foreversurvival.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shared transform helper for the HUD elements.
 *
 * 26.2 draws the GUI through a retained render state, so there is no global
 * model-view stack to poke: the transform belongs to the extractor being drawn
 * into, and is a 2D Matrix3x2fStack rather than a full 4x4.
 */
public final class HudRender {

	private HudRender() {
	}

	/** Translates to (x, y) and scales, so callers can draw from the origin. */
	public static void push(GuiGraphicsExtractor graphics, double x, double y, double scale) {
		graphics.pose().pushMatrix();
		graphics.pose().translate((float) x, (float) y);
		graphics.pose().scale((float) scale, (float) scale);
	}

	public static void pop(GuiGraphicsExtractor graphics) {
		graphics.pose().popMatrix();
	}
}
