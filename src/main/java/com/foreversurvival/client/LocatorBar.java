package com.foreversurvival.client;

import java.util.List;

import com.foreversurvival.network.PlayerLocation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

/**
 * A compass-style bar showing where the other players are.
 *
 * Each player is a tick mark placed by the angle between where you are looking
 * and where they actually are. Look straight at someone and their name (and
 * distance) appears under the bar. Players outside the bar's arc are clamped to
 * the nearest edge and drawn dimmer, so you always know which way to turn.
 *
 * Only players in your own dimension are shown - a direction to someone in the
 * Nether while you are in the Overworld would be meaningless.
 */
public final class LocatorBar extends DrawableHelper {

	private static final int BAR_HEIGHT = 9;
	private static final int MARKER_WIDTH = 3;
	/** Within this many degrees of centre, the player's name is shown. */
	private static final double NAME_ANGLE = 8.0D;

	private static final int RGB_BAR = 0x101018;
	private static final int RGB_BORDER = 0x3B3B4A;
	private static final int RGB_CENTRE = 0xFFD24A;
	private static final int RGB_NAME = 0xFFFFFF;
	private static final int RGB_DISTANCE = 0xA0A0B0;

	/** Distinct, readable marker colours, picked per player by name. */
	private static final int[] PALETTE = {
			0xFF5555, 0x55FF55, 0x5599FF, 0xFFFF55,
			0xFF55FF, 0x55FFFF, 0xFFAA00, 0xAA77FF
	};

	private static final LocatorBar INSTANCE = new LocatorBar();

	private LocatorBar() {
	}

	public static LocatorBar get() {
		return INSTANCE;
	}

	public void render(MatrixStack matrices) {
		if (!HudConfig.locatorEnabled) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity self = client.player;
		if (self == null || client.options.hudHidden || client.options.debugEnabled) {
			return;
		}

		List<PlayerLocation> locations = ClientLocatorState.get();
		if (locations.isEmpty()) {
			return;
		}

		String selfName = self.getGameProfile().getName();
		String selfDimension = self.world.getRegistryKey().getValue().toString();

		int width = HudConfig.locatorWidth;
		int screenWidth = client.getWindow().getScaledWidth();
		int screenHeight = client.getWindow().getScaledHeight();

		double originX = HudConfig.locatorX * screenWidth - (width * HudConfig.locatorScale) / 2.0D;
		double originY = HudConfig.locatorY * screenHeight;

		HudRender.push(originX, originY, HudConfig.locatorScale);
		try {
			drawBar(matrices, client.textRenderer, self, selfName, selfDimension, locations, width);
		} finally {
			HudRender.pop();
		}
	}

	/** Unscaled size of the bar, as {width, height}. */
	public int[] measure() {
		return new int[] { HudConfig.locatorWidth, BAR_HEIGHT };
	}

	/**
	 * Static stand-in used by the layout editor, where there is no live player
	 * data to draw. Same geometry as the real bar so dragging it is accurate.
	 */
	public void renderPreviewAt(MatrixStack matrices, double originX, double originY, double scale) {
		HudRender.push(originX, originY, scale);
		try {
			TextRenderer font = MinecraftClient.getInstance().textRenderer;
			int width = HudConfig.locatorWidth;

			fill(matrices, 0, 0, width, BAR_HEIGHT, HudConfig.backgroundColor(RGB_BAR));
			int border = HudConfig.backgroundColor(RGB_BORDER);
			fill(matrices, 0, 0, width, 1, border);
			fill(matrices, 0, BAR_HEIGHT - 1, width, BAR_HEIGHT, border);
			fill(matrices, 0, 0, 1, BAR_HEIGHT, border);
			fill(matrices, width - 1, 0, width, BAR_HEIGHT, border);

			int centre = width / 2;
			fill(matrices, centre, 1, centre + 1, BAR_HEIGHT - 1, HudConfig.applyTextAlpha(RGB_CENTRE));

			// Three sample markers so the bar reads as a bar while editing.
			int[] offsets = { -width / 3, 4, width / 4 };
			for (int i = 0; i < offsets.length; i++) {
				int x = MathHelper.clamp(centre + offsets[i], 1, width - MARKER_WIDTH - 1);
				fill(matrices, x, 2, x + MARKER_WIDTH, BAR_HEIGHT - 2,
						HudConfig.applyTextAlpha(PALETTE[i]));
			}

			String label = "Player";
			font.draw(matrices, label, centre - font.getWidth(label) / 2, BAR_HEIGHT + 2,
					HudConfig.applyTextAlpha(RGB_NAME));
			if (HudConfig.locatorShowDistance) {
				String distance = "128m";
				font.draw(matrices, distance, centre - font.getWidth(distance) / 2, BAR_HEIGHT + 12,
						HudConfig.applyTextAlpha(RGB_DISTANCE));
			}
		} finally {
			HudRender.pop();
		}
	}

	private void drawBar(MatrixStack matrices, TextRenderer font, ClientPlayerEntity self,
			String selfName, String selfDimension, List<PlayerLocation> locations, int width) {

		// Backing strip
		fill(matrices, 0, 0, width, BAR_HEIGHT, HudConfig.backgroundColor(RGB_BAR));
		int border = HudConfig.backgroundColor(RGB_BORDER);
		fill(matrices, 0, 0, width, 1, border);
		fill(matrices, 0, BAR_HEIGHT - 1, width, BAR_HEIGHT, border);
		fill(matrices, 0, 0, 1, BAR_HEIGHT, border);
		fill(matrices, width - 1, 0, width, BAR_HEIGHT, border);

		// Centre notch - the direction you are actually facing.
		int centre = width / 2;
		fill(matrices, centre, 1, centre + 1, BAR_HEIGHT - 1, HudConfig.applyTextAlpha(RGB_CENTRE));

		double selfYaw = MathHelper.wrapDegrees(self.getYaw());
		double halfFov = HudConfig.locatorFov;
		int half = width / 2 - 2;

		String hoveredName = null;
		double hoveredDistance = 0.0D;
		double bestAngle = Double.MAX_VALUE;

		for (PlayerLocation other : locations) {
			if (other.getName().equals(selfName) || !other.getDimension().equals(selfDimension)) {
				continue;
			}

			double dx = other.getX() - self.getX();
			double dz = other.getZ() - self.getZ();

			// Minecraft yaw: 0 is +Z (south) and increases clockwise from above.
			double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
			double relative = MathHelper.wrapDegrees(targetYaw - selfYaw);

			boolean offEdge = Math.abs(relative) > halfFov;
			double clamped = MathHelper.clamp(relative, -halfFov, halfFov);
			int markerX = centre + (int) Math.round(clamped / halfFov * half);
			markerX = MathHelper.clamp(markerX, 1, width - MARKER_WIDTH - 1);

			int rgb = PALETTE[Math.floorMod(other.getName().hashCode(), PALETTE.length)];
			int colour = offEdge
					? (HudConfig.applyTextAlpha(rgb) & 0x60FFFFFF)
					: HudConfig.applyTextAlpha(rgb);

			fill(matrices, markerX, 2, markerX + MARKER_WIDTH, BAR_HEIGHT - 2, colour);

			double angle = Math.abs(relative);
			if (!offEdge && angle <= NAME_ANGLE && angle < bestAngle) {
				bestAngle = angle;
				hoveredName = other.getName();

				double dy = other.getY() - self.getY();
				hoveredDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);
			}
		}

		// Name of whoever you are looking closest to.
		if (hoveredName != null) {
			String label = hoveredName;
			int labelX = centre - font.getWidth(label) / 2;
			font.draw(matrices, label, labelX, BAR_HEIGHT + 2, HudConfig.applyTextAlpha(RGB_NAME));

			if (HudConfig.locatorShowDistance) {
				String distance = Math.round(hoveredDistance) + "m";
				int distanceX = centre - font.getWidth(distance) / 2;
				font.draw(matrices, distance, distanceX, BAR_HEIGHT + 12,
						HudConfig.applyTextAlpha(RGB_DISTANCE));
			}
		}
	}
}
