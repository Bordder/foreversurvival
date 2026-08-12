package com.foreversurvival.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.foreversurvival.data.PlayerQuestData;
import com.foreversurvival.quest.Quest;
import com.foreversurvival.quest.QuestManager;
import com.foreversurvival.quest.task.QuestTask;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

/**
 * The on-screen objective overlay.
 *
 * Shows the current main quest and its outstanding objectives with a checkbox
 * each. A pinned quest can be appended: a side challenge gets its own objective
 * list, while a pinned main-line quest is only ever a single line of text, since
 * the main line is already on screen above it.
 *
 * This is a UI panel, not a world marker: it never points at anything, never
 * draws in the world, and never reveals a locked quest.
 */
public final class QuestHud extends DrawableHelper {

	private static final int PADDING = 5;
	private static final int LINE_HEIGHT = 11;
	private static final int MIN_WIDTH = 110;
	private static final int MAX_WIDTH = 220;
	private static final int MAX_TASK_ROWS = 6;

	private static final int RGB_HEADER = 0xFFD24A;
	private static final int RGB_TITLE = 0xFFFFFF;
	private static final int RGB_PHASE = 0xA0A0B0;
	private static final int RGB_TASK = 0xCFCFDA;
	private static final int RGB_TASK_DONE = 0x6BD97A;
	private static final int RGB_PINNED = 0x9FD3FF;
	private static final int RGB_BORDER = 0x3B3B4A;
	private static final int RGB_PANEL = 0x121218;

	private static final QuestHud INSTANCE = new QuestHud();

	private QuestHud() {
	}

	public static QuestHud get() {
		return INSTANCE;
	}

	public static void register() {
		HudRenderCallback.EVENT.register((matrices, tickDelta) -> {
			INSTANCE.render(matrices);
			LocatorBar.get().render(matrices);
		});
	}

	// ------------------------------------------------------------------
	// Content
	// ------------------------------------------------------------------

	/** The pinned quest, or null if nothing is pinned or it is already shown. */
	@Nullable
	public static Quest getPinnedQuest(@Nullable Quest current) {
		String id = HudConfig.pinnedQuestId;
		if (id == null || id.isEmpty()) {
			return null;
		}

		Quest pinned = QuestManager.get().getQuest(id);
		if (pinned == null) {
			return null;
		}
		if (current != null && pinned.getId().equals(current.getId())) {
			return null;
		}
		return pinned;
	}

	private List<String> taskLabels(@Nullable Quest quest, List<Boolean> doneOut) {
		List<String> labels = new ArrayList<>();
		if (quest == null) {
			return labels;
		}

		PlayerQuestData data = ClientQuestState.get();
		for (QuestTask task : quest.getTasks()) {
			int progress = Math.min(task.getRequired(), data.getProgress(quest.getId(), task.getId()));
			boolean done = progress >= task.getRequired();

			String label = task.getDescription();
			if (task.getRequired() > 1) {
				label = label + "  " + progress + "/" + task.getRequired();
			}

			labels.add(label);
			doneOut.add(done);

			if (labels.size() >= MAX_TASK_ROWS) {
				break;
			}
		}
		return labels;
	}

	// ------------------------------------------------------------------
	// Layout
	// ------------------------------------------------------------------

	/** Unscaled panel size for the current quest, as {width, height}. */
	public int[] measure(@Nullable Quest quest) {
		TextRenderer font = MinecraftClient.getInstance().textRenderer;

		String header = "Current Objective";
		String title = quest == null ? "All quests complete" : quest.getTitle();
		String phase = quest == null ? "" : quest.getPhase().getDisplayName();

		List<Boolean> done = new ArrayList<>();
		List<String> labels = HudConfig.showObjectives ? taskLabels(quest, done) : List.of();

		int textLeftOffset = HudConfig.showIcon ? 20 : 0;
		int width = Math.max(MIN_WIDTH, font.getWidth(header) + textLeftOffset);
		width = Math.max(width, font.getWidth(title) + textLeftOffset);
		width = Math.max(width, font.getWidth(phase));
		for (String label : labels) {
			width = Math.max(width, font.getWidth(label) + 14);
		}

		int headerHeight = HudConfig.showIcon ? 20 : LINE_HEIGHT * 2;
		int height = PADDING * 2 + headerHeight + LINE_HEIGHT;
		if (!labels.isEmpty()) {
			height += 3 + labels.size() * LINE_HEIGHT;
		}

		// Pinned block
		Quest pinned = getPinnedQuest(quest);
		if (pinned != null) {
			String pinnedTitle = "Pinned: " + pinned.getTitle();
			width = Math.max(width, font.getWidth(pinnedTitle));
			height += 5 + LINE_HEIGHT;

			if (pinned.getPhase().isSide() && HudConfig.showObjectives) {
				List<Boolean> pinnedDone = new ArrayList<>();
				List<String> pinnedLabels = taskLabels(pinned, pinnedDone);
				for (String label : pinnedLabels) {
					width = Math.max(width, font.getWidth(label) + 14);
				}
				height += pinnedLabels.size() * LINE_HEIGHT;
			}
		}

		width = Math.min(MAX_WIDTH, width) + PADDING * 2;
		return new int[] { width, height };
	}

	// ------------------------------------------------------------------
	// Rendering
	// ------------------------------------------------------------------

	private void render(MatrixStack matrices) {
		if (!HudConfig.enabled) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.options.hudHidden || client.options.debugEnabled) {
			return;
		}

		Quest quest = ClientQuestState.getCurrentMainQuest();
		if (quest == null && getPinnedQuest(null) == null) {
			return;
		}

		int screenWidth = client.getWindow().getScaledWidth();
		int screenHeight = client.getWindow().getScaledHeight();

		renderAt(matrices, HudConfig.hudX * screenWidth, HudConfig.hudY * screenHeight,
				HudConfig.hudScale, quest);
	}

	/** Draws the panel with its top-left corner at (originX, originY). */
	public void renderAt(MatrixStack matrices, double originX, double originY, double scale,
			@Nullable Quest quest) {
		HudRender.push(originX, originY, scale);
		try {
			drawPanel(matrices, quest);
		} finally {
			HudRender.pop();
		}
	}

	private void drawPanel(MatrixStack matrices, @Nullable Quest quest) {
		MinecraftClient client = MinecraftClient.getInstance();
		TextRenderer font = client.textRenderer;

		String header = "Current Objective";
		String title = quest == null ? "All quests complete" : quest.getTitle();
		String phase = quest == null ? "" : quest.getPhase().getDisplayName();

		List<Boolean> done = new ArrayList<>();
		List<String> labels = HudConfig.showObjectives ? taskLabels(quest, done) : List.of();

		int[] size = measure(quest);
		int width = size[0];
		int height = size[1];

		if (HudConfig.backgroundOpacity > 0) {
			fill(matrices, 0, 0, width, height, HudConfig.backgroundColor(RGB_PANEL));

			int border = HudConfig.backgroundColor(RGB_BORDER);
			fill(matrices, 0, 0, width, 1, border);
			fill(matrices, 0, height - 1, width, height, border);
			fill(matrices, 0, 0, 1, height, border);
			fill(matrices, width - 1, 0, width, height, border);
		}

		int textLeftOffset = HudConfig.showIcon ? 20 : 0;
		int textX = PADDING + textLeftOffset;
		int cursorY = PADDING;

		if (HudConfig.showIcon && quest != null) {
			client.getItemRenderer().renderInGuiWithOverrides(quest.getIconStack(), PADDING, cursorY + 1);
		}

		font.draw(matrices, header, textX, cursorY, HudConfig.applyTextAlpha(RGB_HEADER));
		font.draw(matrices, trim(font, title, width - PADDING * 2 - textLeftOffset),
				textX, cursorY + 10, HudConfig.applyTextAlpha(RGB_TITLE));
		cursorY += HudConfig.showIcon ? 20 : LINE_HEIGHT * 2;

		if (!phase.isEmpty()) {
			font.draw(matrices, trim(font, phase, width - PADDING * 2), PADDING, cursorY,
					HudConfig.applyTextAlpha(RGB_PHASE));
		}
		cursorY += LINE_HEIGHT;

		cursorY = drawTaskRows(matrices, font, labels, done, width, cursorY);

		// ---- Pinned quest ----
		Quest pinned = getPinnedQuest(quest);
		if (pinned == null) {
			return;
		}

		cursorY += 2;
		fill(matrices, PADDING, cursorY, width - PADDING, cursorY + 1,
				HudConfig.backgroundColor(RGB_BORDER));
		cursorY += 3;

		String pinnedTitle = "Pinned: " + pinned.getTitle();
		font.draw(matrices, trim(font, pinnedTitle, width - PADDING * 2), PADDING, cursorY,
				HudConfig.applyTextAlpha(RGB_PINNED));
		cursorY += LINE_HEIGHT;

		// Only a real side challenge gets its objectives listed; a pinned main
		// quest stays a single line, because the main line is already above.
		if (pinned.getPhase().isSide() && HudConfig.showObjectives) {
			List<Boolean> pinnedDone = new ArrayList<>();
			List<String> pinnedLabels = taskLabels(pinned, pinnedDone);
			drawTaskRows(matrices, font, pinnedLabels, pinnedDone, width, cursorY - 3);
		}
	}

	private int drawTaskRows(MatrixStack matrices, TextRenderer font, List<String> labels,
			List<Boolean> done, int width, int cursorY) {
		if (labels.isEmpty()) {
			return cursorY;
		}

		cursorY += 3;
		int checkboxX = width - PADDING - 9;

		for (int i = 0; i < labels.size(); i++) {
			boolean complete = done.get(i);
			String label = trim(font, labels.get(i), width - PADDING * 2 - 14);

			font.draw(matrices, label, PADDING, cursorY,
					HudConfig.applyTextAlpha(complete ? RGB_TASK_DONE : RGB_TASK));
			drawCheckbox(matrices, checkboxX, cursorY - 1, complete);

			cursorY += LINE_HEIGHT;
		}
		return cursorY;
	}

	/** Small square, filled green once the objective is satisfied. */
	private void drawCheckbox(MatrixStack matrices, int x, int y, boolean checked) {
		int border = HudConfig.applyTextAlpha(checked ? RGB_TASK_DONE : RGB_BORDER);
		int size = 9;

		fill(matrices, x, y, x + size, y + 1, border);
		fill(matrices, x, y + size - 1, x + size, y + size, border);
		fill(matrices, x, y, x + 1, y + size, border);
		fill(matrices, x + size - 1, y, x + size, y + size, border);

		if (checked) {
			fill(matrices, x + 2, y + 2, x + size - 2, y + size - 2,
					HudConfig.applyTextAlpha(RGB_TASK_DONE));
		}
	}

	private static String trim(TextRenderer font, String text, int maxWidth) {
		if (font.getWidth(text) <= maxWidth) {
			return text;
		}
		return font.trimToWidth(text, Math.max(0, maxWidth - font.getWidth("..."))) + "...";
	}
}
