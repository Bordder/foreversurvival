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
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;

/**
 * The on-screen objective overlay.
 *
 * The panel is laid out to an explicit width from {@link HudConfig}: everything
 * WRAPS to that width and nothing is ever truncated, so a long objective is
 * always readable in full. Height grows to fit the content, and can be padded
 * out to a configured minimum.
 *
 * A pinned side challenge gets its own objective list; a pinned main-line quest
 * is only a line of text, since the main line is already on screen above it.
 *
 * This is a UI panel, not a world marker: it never points at anything, never
 * draws in the world, and never reveals a locked quest.
 */
public final class QuestHud extends DrawableHelper {

	private static final int PADDING = 5;
	private static final int LINE = 10;
	private static final int ICON = 20;
	private static final int CHECKBOX = 12;

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
			LocatorBar.get().renderFree(matrices);
		});
	}

	/** One laid-out line. A checkbox is drawn on the first line of a task only. */
	private static final class Row {

		final OrderedText text;
		final int rgb;
		final int indent;
		/** -1 none, 0 empty box, 1 ticked. */
		final int checkbox;

		Row(OrderedText text, int rgb, int indent, int checkbox) {
			this.text = text;
			this.rgb = rgb;
			this.indent = indent;
			this.checkbox = checkbox;
		}
	}

	// ------------------------------------------------------------------
	// Content
	// ------------------------------------------------------------------

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

	private void addWrapped(List<Row> out, TextRenderer font, String text, int width, int rgb,
			int indent, int checkbox) {
		if (text == null || text.isEmpty()) {
			return;
		}

		List<OrderedText> lines = font.wrapLines(new LiteralText(text), Math.max(20, width));
		for (int i = 0; i < lines.size(); i++) {
			// Only the first line of a block carries the checkbox.
			out.add(new Row(lines.get(i), rgb, indent, i == 0 ? checkbox : -1));
		}
	}

	private void addTaskRows(List<Row> out, TextRenderer font, Quest quest, int textWidth) {
		PlayerQuestData data = ClientQuestState.get();

		for (QuestTask task : quest.getTasks()) {
			int progress = Math.min(task.getRequired(), data.getProgress(quest.getId(), task.getId()));
			boolean done = progress >= task.getRequired();

			String label = task.getDescription();
			if (task.getRequired() > 1) {
				label = label + "  " + progress + "/" + task.getRequired();
			}

			addWrapped(out, font, label, textWidth - CHECKBOX, done ? RGB_TASK_DONE : RGB_TASK,
					CHECKBOX, done ? 1 : 0);
		}
	}

	/** The whole panel as a flat list of laid-out lines. */
	private List<Row> layout(@Nullable Quest quest) {
		TextRenderer font = MinecraftClient.getInstance().textRenderer;
		int textWidth = HudConfig.hudWidth - PADDING * 2;
		int headerWidth = textWidth - (HudConfig.showIcon ? ICON : 0);

		List<Row> rows = new ArrayList<>();

		int headerIndent = HudConfig.showIcon ? ICON : 0;
		addWrapped(rows, font, "Current Objective", headerWidth, RGB_HEADER, headerIndent, -1);
		addWrapped(rows, font, quest == null ? "All quests complete" : quest.getTitle(),
				headerWidth, RGB_TITLE, headerIndent, -1);

		if (quest != null) {
			addWrapped(rows, font, quest.getPhase().getDisplayName(), textWidth, RGB_PHASE, 0, -1);

			if (HudConfig.showObjectives) {
				addTaskRows(rows, font, quest, textWidth);
			}
		}

		Quest pinned = getPinnedQuest(quest);
		if (pinned != null) {
			// Blank spacer row stands in for the separator line.
			rows.add(new Row(null, 0, 0, -1));
			addWrapped(rows, font, "Pinned: " + pinned.getTitle(), textWidth, RGB_PINNED, 0, -1);

			// Only a real side challenge lists its objectives; a pinned main
			// quest stays one line, because the main line is already above.
			if (pinned.getPhase().isSide() && HudConfig.showObjectives) {
				addTaskRows(rows, font, pinned, textWidth);
			}
		}

		return rows;
	}

	/** Unscaled panel size as {width, height}. */
	public int[] measure(@Nullable Quest quest) {
		List<Row> rows = layout(quest);

		int textHeight = rows.size() * LINE;
		// The icon block is 20px tall; make sure the header never overlaps it.
		int minimum = HudConfig.showIcon ? ICON : 0;
		int height = PADDING * 2 + Math.max(textHeight, minimum);

		return new int[] { HudConfig.hudWidth, Math.max(height, HudConfig.hudMinHeight) };
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

		List<Row> rows = layout(quest);
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

		if (HudConfig.showIcon && quest != null) {
			client.getItemRenderer().renderInGuiWithOverrides(quest.getIconStack(), PADDING, PADDING + 1);
		}

		int y = PADDING;
		for (Row row : rows) {
			if (row.text == null) {
				// Spacer: draw the separator rule in the middle of the gap.
				fill(matrices, PADDING, y + LINE / 2, width - PADDING, y + LINE / 2 + 1,
						HudConfig.backgroundColor(RGB_BORDER));
				y += LINE;
				continue;
			}

			int x = PADDING + row.indent;
			font.draw(matrices, row.text, x, y, HudConfig.applyTextAlpha(row.rgb));

			if (row.checkbox >= 0) {
				drawCheckbox(matrices, PADDING, y - 1, row.checkbox == 1);
			}

			y += LINE;
		}
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
}
