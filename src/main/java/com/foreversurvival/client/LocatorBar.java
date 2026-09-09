package com.foreversurvival.client;

import java.util.List;

import com.foreversurvival.network.PlayerLocation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/**
 * A compass-style bar showing where the other players are.
 *
 * Each player is a tick mark placed by the angle between where you are looking
 * and where they actually are. Look straight at someone and their name and
 * distance appear. Players outside the bar's arc clamp to the nearest edge and
 * draw dimmer, so you always know which way to turn.
 *
 * Two placements, chosen in Config:
 *
 *  - XP BAR SLOT (default). The bar shares the experience bar's slot above the
 *    hotbar and the two alternate every few seconds, the way the vanilla 1.21.6
 *    locator bar and most locator mods do it. When nobody else is online - or
 *    nobody is in your dimension - it never takes the slot at all, so single
 *    player looks exactly like vanilla.
 *  - FREE. Floats wherever you drag it in the layout editor.
 *
 * Only players in your own dimension are shown; a bearing to someone in the
 * Nether while you are in the Overworld would mean nothing.
 */
public final class LocatorBar {

	private static final int MARKER_WIDTH = 3;
	/** Within this many degrees of centre, the player's name is shown. */
	private static final double NAME_ANGLE = 8.0D;

	private static final int RGB_BAR = 0x101018;
	private static final int RGB_BORDER = 0x3B3B4A;
	private static final int RGB_CENTRE = 0xFFD24A;
	private static final int RGB_NAME = 0xFFFFFF;
	private static final int RGB_DISTANCE = 0xA0A0B0;

	private static final int[] PALETTE = {
			0xFF5555, 0x55FF55, 0x5599FF, 0xFFFF55,
			0xFF55FF, 0x55FFFF, 0xFFAA00, 0xAA77FF
	};

	private static final LocatorBar INSTANCE = new LocatorBar();

	/** Set by the mixin each frame the vanilla XP bar method actually runs. */
	private boolean xpSlotOffered;

	// Distance readout smoothing. A player in a boat rocks back and forth by
	// most of a block, which makes a plain rounded distance flicker between two
	// numbers. Only move the shown value once it is a full metre out.
	private String shownFor;
	private double shownDistance;

	private LocatorBar() {
	}

	public static LocatorBar get() {
		return INSTANCE;
	}

	// ------------------------------------------------------------------
	// Visibility
	// ------------------------------------------------------------------

	private boolean hudUsable(Minecraft client) {
		return HudConfig.locatorEnabled
				&& client.player != null
				&& !client.options.hudHidden
				&& !client.options.debugEnabled;
	}

	/** True when at least one other player is in the same dimension as you. */
	private boolean hasCompany(LocalPlayer self) {
		String selfName = self.getGameProfile().getName();
		String dimension = self.level().getRegistryKey().getValue().toString();

		for (PlayerLocation other : ClientLocatorState.get()) {
			if (!other.getName().equals(selfName) && other.getDimension().equals(dimension)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The alternation. Each of the XP bar and the locator bar holds the slot for
	 * {@code locatorSwapSeconds}, so a full cycle is twice that.
	 */
	private boolean rotationFavoursLocator() {
		long period = Math.max(2, HudConfig.locatorSwapSeconds) * 1000L;
		return ((System.currentTimeMillis() / period) & 1L) == 1L;
	}

	/** Asked by the mixin: should the locator bar replace the XP bar right now? */
	public boolean shouldTakeXpSlot() {
		Minecraft client = Minecraft.getInstance();
		if (!HudConfig.locatorXpBarMode || !hudUsable(client)) {
			return false;
		}
		if (!hasCompany(client.player)) {
			// Nobody to point at - leave the XP bar completely alone.
			return false;
		}
		return rotationFavoursLocator();
	}

	public void markXpSlotOffered() {
		this.xpSlotOffered = true;
	}

	// ------------------------------------------------------------------
	// Entry points
	// ------------------------------------------------------------------

	/**
	 * Free-position rendering, driven by the HUD callback.
	 *
	 * Also covers the case where the vanilla XP bar was never drawn this frame -
	 * creative mode, or while riding a mount, where vanilla swaps the slot for
	 * the jump/health bar - so XP-slot mode still shows something there.
	 */
	public void renderFree(GuiGraphicsExtractor graphics) {
		Minecraft client = Minecraft.getInstance();

		if (HudConfig.locatorXpBarMode) {
			boolean offered = xpSlotOffered;
			xpSlotOffered = false;

			if (!offered && shouldTakeXpSlot()) {
				int screenWidth = client.getWindow().getGuiScaledWidth();
				renderXpSlot(graphics, screenWidth / 2 - 91);
			}
			return;
		}

		if (!hudUsable(client)) {
			return;
		}

		int screenWidth = client.getWindow().getGuiScaledWidth();
		int screenHeight = client.getWindow().getGuiScaledHeight();
		int width = HudConfig.locatorWidth;

		double originX = HudConfig.locatorX * screenWidth - (width * HudConfig.locatorScale) / 2.0D;
		double originY = HudConfig.locatorY * screenHeight;

		HudRender.push(graphics, originX, originY, HudConfig.locatorScale);
		try {
			drawBar(graphics, client, 0, 0, width, HudConfig.locatorHeight);
		} finally {
			HudRender.pop(graphics);
		}
	}

	/**
	 * Draws into the experience bar's slot. {@code xpBarX} is the left edge
	 * vanilla would have used, so the bar lines up with the hotbar.
	 */
	public void renderXpSlot(GuiGraphicsExtractor graphics, int xpBarX) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}

		int screenHeight = client.getWindow().getGuiScaledHeight();
		int width = HudConfig.locatorWidth;
		int height = HudConfig.locatorHeight;

		// Vanilla puts the XP bar at scaledHeight - 32 + 3.
		int x = xpBarX + (182 - width) / 2;
		int y = screenHeight - 32 + 3;

		drawBar(graphics, client, x, y, width, height);
	}

	// ------------------------------------------------------------------
	// Drawing
	// ------------------------------------------------------------------

	private void drawBar(GuiGraphicsExtractor graphics, Minecraft client, int x, int y,
			int width, int height) {
		LocalPlayer self = client.player;
		if (self == null) {
			return;
		}

		Font font = client.font;
		String selfName = self.getGameProfile().getName();
		String selfDimension = self.level().getRegistryKey().getValue().toString();

		graphics.fill(x, y, x + width, y + height, HudConfig.backgroundColor(RGB_BAR));
		int border = HudConfig.backgroundColor(RGB_BORDER);
		graphics.fill(x, y, x + width, y + 1, border);
		graphics.fill(x, y + height - 1, x + width, y + height, border);
		graphics.fill(x, y, x + 1, y + height, border);
		graphics.fill(x + width - 1, y, x + width, y + height, border);

		int centre = x + width / 2;
		graphics.fill(centre, y + 1, centre + 1, y + height - 1, HudConfig.applyTextAlpha(RGB_CENTRE));

		double selfYaw = Mth.wrapDegrees(self.getYRot());
		double halfFov = HudConfig.locatorFov;
		int half = width / 2 - 2;

		String hoveredName = null;
		double hoveredDistance = 0.0D;
		double bestAngle = Double.MAX_VALUE;

		List<PlayerLocation> locations = ClientLocatorState.get();
		for (PlayerLocation other : locations) {
			if (other.getName().equals(selfName) || !other.getDimension().equals(selfDimension)) {
				continue;
			}

			double dx = other.getX() - self.getX();
			double dz = other.getZ() - self.getZ();

			// Minecraft yaw: 0 is +Z (south) and increases clockwise from above.
			double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
			double relative = Mth.wrapDegrees(targetYaw - selfYaw);

			boolean offEdge = Math.abs(relative) > halfFov;
			double clamped = Mth.clamp(relative, -halfFov, halfFov);
			int markerX = centre + (int) Math.round(clamped / halfFov * half);
			markerX = Mth.clamp(markerX, x + 1, x + width - MARKER_WIDTH - 1);

			int rgb = PALETTE[Math.floorMod(other.getName().hashCode(), PALETTE.length)];
			int colour = offEdge
					? (HudConfig.applyTextAlpha(rgb) & 0x60FFFFFF)
					: HudConfig.applyTextAlpha(rgb);

			graphics.fill(markerX, y + 2, markerX + MARKER_WIDTH, y + height - 2, colour);

			double angle = Math.abs(relative);
			if (!offEdge && angle <= NAME_ANGLE && angle < bestAngle) {
				bestAngle = angle;
				hoveredName = other.getName();

				double dy = other.getY() - self.getY();
				hoveredDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);
			}
		}

		if (hoveredName != null) {
			// In the XP slot there is no room underneath, so labels go above.
			boolean above = HudConfig.locatorXpBarMode;
			int labelY = above ? y - 20 : y + height + 2;
			int distanceY = above ? y - 10 : y + height + 12;

			graphics.text(font, hoveredName, centre - font.width(hoveredName) / 2, labelY,
					HudConfig.applyTextAlpha(RGB_NAME));

			if (HudConfig.locatorShowDistance) {
				if (!hoveredName.equals(shownFor) || Math.abs(hoveredDistance - shownDistance) >= 1.0D) {
					shownFor = hoveredName;
					shownDistance = hoveredDistance;
				}

				String distance = Math.round(shownDistance) + "m";
				graphics.text(font, distance, centre - font.width(distance) / 2, distanceY,
						HudConfig.applyTextAlpha(RGB_DISTANCE));
			}
		} else {
			shownFor = null;
		}
	}

	// ------------------------------------------------------------------
	// Layout editor support
	// ------------------------------------------------------------------

	/** Unscaled size of the bar, as {width, height}. */
	public int[] measure() {
		return new int[] { HudConfig.locatorWidth, HudConfig.locatorHeight };
	}

	/** Static stand-in used by the layout editor, where there is no live data. */
	public void renderPreviewAt(GuiGraphicsExtractor graphics, double originX, double originY, double scale) {
		HudRender.push(graphics, originX, originY, scale);
		try {
			Font font = Minecraft.getInstance().font;
			int width = HudConfig.locatorWidth;
			int height = HudConfig.locatorHeight;

			graphics.fill(0, 0, width, height, HudConfig.backgroundColor(RGB_BAR));
			int border = HudConfig.backgroundColor(RGB_BORDER);
			graphics.fill(0, 0, width, 1, border);
			graphics.fill(0, height - 1, width, height, border);
			graphics.fill(0, 0, 1, height, border);
			graphics.fill(width - 1, 0, width, height, border);

			int centre = width / 2;
			graphics.fill(centre, 1, centre + 1, height - 1, HudConfig.applyTextAlpha(RGB_CENTRE));

			int[] offsets = { -width / 3, 4, width / 4 };
			for (int i = 0; i < offsets.length; i++) {
				int px = Mth.clamp(centre + offsets[i], 1, width - MARKER_WIDTH - 1);
				graphics.fill(px, 2, px + MARKER_WIDTH, height - 2,
						HudConfig.applyTextAlpha(PALETTE[i]));
			}

			String label = "Player";
			graphics.text(font, label, centre - font.width(label) / 2, height + 2,
					HudConfig.applyTextAlpha(RGB_NAME));
		} finally {
			HudRender.pop(graphics);
		}
	}
}
