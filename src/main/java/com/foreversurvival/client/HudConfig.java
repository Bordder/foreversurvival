package com.foreversurvival.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.foreversurvival.ForeverSurvival;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Client-side display settings for the objective overlay and the locator bar.
 *
 * Positions are stored as a fraction of the screen (0.0 - 1.0) rather than in
 * pixels, so a layout set up on one resolution still looks right on another.
 *
 * Purely cosmetic and purely local - none of this is synced or saved into the
 * world. Written to config/foreversurvival.properties whenever a value changes.
 */
public final class HudConfig {

	// ------------------------------------------------------------------
	// Objective overlay
	// ------------------------------------------------------------------

	public static boolean enabled = true;
	/** Top-left corner of the panel, as a fraction of the screen. */
	public static double hudX = 0.72D;
	public static double hudY = 0.02D;
	public static double hudScale = 1.0D;
	/** Panel background alpha, 0-100. 0 means text only, no panel. */
	public static int backgroundOpacity = 60;
	/** Text alpha, 20-100. Floored so it can never become invisible. */
	public static int textOpacity = 100;
	public static boolean showObjectives = true;
	public static boolean showIcon = true;
	/** Quest pinned to the overlay in addition to the current objective. */
	public static String pinnedQuestId = "";

	/** Swallow the vanilla "Advancement Made!" pop-up. */
	public static boolean hideAdvancementToasts = true;

	/** Quest browser layout: false = list + detail, true = per-phase tree grid. */
	public static boolean treeView = false;

	// ------------------------------------------------------------------
	// Locator bar
	// ------------------------------------------------------------------

	public static boolean locatorEnabled = true;
	/** Centre of the bar, as a fraction of the screen. */
	public static double locatorX = 0.5D;
	public static double locatorY = 0.04D;
	public static double locatorScale = 1.0D;
	/** Bar width in GUI pixels before scaling. */
	public static int locatorWidth = 180;
	/** Half-angle of the arc the bar covers, in degrees. */
	public static int locatorFov = 90;
	public static boolean locatorShowDistance = true;

	private HudConfig() {
	}

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(ForeverSurvival.MOD_ID + ".properties");
	}

	public static void load() {
		Path path = configPath();
		if (!Files.exists(path)) {
			return;
		}

		Properties p = new Properties();
		try (InputStream in = Files.newInputStream(path)) {
			p.load(in);
		} catch (IOException e) {
			ForeverSurvival.LOGGER.warn("Could not read HUD config, using defaults", e);
			return;
		}

		enabled = bool(p, "enabled", true);
		showObjectives = bool(p, "showObjectives", true);
		showIcon = bool(p, "showIcon", true);
		hudX = clamp(dbl(p, "hudX", 0.72D), 0.0D, 1.0D);
		hudY = clamp(dbl(p, "hudY", 0.02D), 0.0D, 1.0D);
		hudScale = clamp(dbl(p, "hudScale", 1.0D), 0.5D, 2.0D);
		backgroundOpacity = clamp(integer(p, "backgroundOpacity", 60), 0, 100);
		textOpacity = clamp(integer(p, "textOpacity", 100), 20, 100);
		pinnedQuestId = p.getProperty("pinnedQuestId", "");
		hideAdvancementToasts = bool(p, "hideAdvancementToasts", true);
		treeView = bool(p, "treeView", false);

		locatorEnabled = bool(p, "locatorEnabled", true);
		locatorX = clamp(dbl(p, "locatorX", 0.5D), 0.0D, 1.0D);
		locatorY = clamp(dbl(p, "locatorY", 0.04D), 0.0D, 1.0D);
		locatorScale = clamp(dbl(p, "locatorScale", 1.0D), 0.5D, 2.0D);
		locatorWidth = clamp(integer(p, "locatorWidth", 180), 80, 400);
		locatorFov = clamp(integer(p, "locatorFov", 90), 30, 180);
		locatorShowDistance = bool(p, "locatorShowDistance", true);
	}

	public static void save() {
		Properties p = new Properties();
		p.setProperty("enabled", Boolean.toString(enabled));
		p.setProperty("hudX", Double.toString(round(hudX)));
		p.setProperty("hudY", Double.toString(round(hudY)));
		p.setProperty("hudScale", Double.toString(round(hudScale)));
		p.setProperty("backgroundOpacity", Integer.toString(backgroundOpacity));
		p.setProperty("textOpacity", Integer.toString(textOpacity));
		p.setProperty("showObjectives", Boolean.toString(showObjectives));
		p.setProperty("showIcon", Boolean.toString(showIcon));
		p.setProperty("pinnedQuestId", pinnedQuestId == null ? "" : pinnedQuestId);
		p.setProperty("hideAdvancementToasts", Boolean.toString(hideAdvancementToasts));
		p.setProperty("treeView", Boolean.toString(treeView));

		p.setProperty("locatorEnabled", Boolean.toString(locatorEnabled));
		p.setProperty("locatorX", Double.toString(round(locatorX)));
		p.setProperty("locatorY", Double.toString(round(locatorY)));
		p.setProperty("locatorScale", Double.toString(round(locatorScale)));
		p.setProperty("locatorWidth", Integer.toString(locatorWidth));
		p.setProperty("locatorFov", Integer.toString(locatorFov));
		p.setProperty("locatorShowDistance", Boolean.toString(locatorShowDistance));

		try {
			Path path = configPath();
			Files.createDirectories(path.getParent());
			try (OutputStream out = Files.newOutputStream(path)) {
				p.store(out, "ForeverSurvival HUD settings");
			}
		} catch (IOException e) {
			ForeverSurvival.LOGGER.warn("Could not save HUD config", e);
		}
	}

	public static void resetToDefaults() {
		enabled = true;
		hudX = 0.72D;
		hudY = 0.02D;
		hudScale = 1.0D;
		backgroundOpacity = 60;
		textOpacity = 100;
		showObjectives = true;
		showIcon = true;
		pinnedQuestId = "";
		hideAdvancementToasts = true;
		treeView = false;

		locatorEnabled = true;
		locatorX = 0.5D;
		locatorY = 0.04D;
		locatorScale = 1.0D;
		locatorWidth = 180;
		locatorFov = 90;
		locatorShowDistance = true;
		save();
	}

	/** Applies the text opacity setting to an 0xRRGGBB colour. */
	public static int applyTextAlpha(int rgb) {
		int alpha = clamp((int) (textOpacity * 2.55D), 20, 255);
		return (alpha << 24) | (rgb & 0xFFFFFF);
	}

	/** Background colour with the configured opacity baked in. */
	public static int backgroundColor(int rgb) {
		int alpha = clamp((int) (backgroundOpacity * 2.55D), 0, 255);
		return (alpha << 24) | (rgb & 0xFFFFFF);
	}

	// ------------------------------------------------------------------

	private static boolean bool(Properties p, String key, boolean fallback) {
		String v = p.getProperty(key);
		return v == null ? fallback : Boolean.parseBoolean(v.trim());
	}

	private static int integer(Properties p, String key, int fallback) {
		try {
			String v = p.getProperty(key);
			return v == null ? fallback : Integer.parseInt(v.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static double dbl(Properties p, String key, double fallback) {
		try {
			String v = p.getProperty(key);
			return v == null ? fallback : Double.parseDouble(v.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static double round(double value) {
		return Math.round(value * 10000.0D) / 10000.0D;
	}

	public static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	public static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}
