package com.foreversurvival.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.foreversurvival.data.PlayerQuestData;
import com.foreversurvival.ForeverSurvival;
import com.foreversurvival.quest.Quest;
import com.foreversurvival.quest.QuestManager;
import com.foreversurvival.quest.task.QuestTask;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;

/**
 * The on-screen objective overlay.
 *
 * The panel is laid out to an explicit width from {@link HudConfig}: everything
 * WRAPS to that width and nothing is ever truncated, so a long objective is
 * always readable in full. Height is either automatic (sized to the content) or
 * pinned to an explicit value, in which case surplus rows are dropped whole and
 * counted as "+N more..." rather than being clipped mid-line.
 *
 * A pinned side challenge gets its own objective list; a pinned main-line quest
 * is only a line of text, since the main line is already on screen above it.
 *
 * This is a UI panel, not a world marker: it never points at anything, never
 * draws in the world, and never reveals a locked quest.
 */
public final class QuestHud {

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
		// A registered element rather than a mixin: supported API, and it draws
		// after the vanilla elements so the panel sits on top.
		HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath(ForeverSurvival.MOD_ID, "quest_hud"),
				(graphics, deltaTracker) -> {
					INSTANCE.render(graphics);
					LocatorBar.get().renderFree(graphics);
				});
	}

	/** One laid-out line. A checkbox is drawn on the first line of a task only. */
	private static final class Row {

		final FormattedCharSequence text;
		final int rgb;
		final int indent;
		/** -1 none, 0 empty box, 1 ticked. */
		final int checkbox;

		Row(FormattedCharSequence text, int rgb, int indent, int checkbox) {
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

	private void addWrapped(List<Row> out, Font font, String text, int width, int rgb,
			int indent, int checkbox) {
		if (text == null || text.isEmpty()) {
			return;
		}

		List<FormattedCharSequence> lines = font.split(Component.literal(text), Math.max(20, width));
		for (int i = 0; i < lines.size(); i++) {
			// Only the first line of a block carries the checkbox.
			out.add(new Row(lines.get(i), rgb, indent, i == 0 ? checkbox : -1));
		}
	}

	private void addTaskRows(List<Row> out, Font font, Quest quest, int textWidth) {
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
		Font font = Minecraft.getInstance().font;
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

		// An explicit height caps how many rows are shown. Lines are dropped
		// whole - a line is never half-drawn and text is never truncated - and
		// the overflow is counted so nothing silently disappears.
		if (HudConfig.hudHeight > 0) {
			int maxRows = Math.max(1, (HudConfig.hudHeight - PADDING * 2) / LINE);
			if (rows.size() > maxRows) {
				Font font2 = Minecraft.getInstance().font;
				int hidden = rows.size() - (maxRows - 1);
				List<Row> trimmed = new ArrayList<>(rows.subList(0, Math.max(0, maxRows - 1)));
				addWrapped(trimmed, font2, "+" + hidden + " more...",
						HudConfig.hudWidth - PADDING * 2, RGB_PHASE, 0, -1);
				return trimmed;
			}
		}

		return rows;
	}

	/**
	 * Width the panel actually needs to hold its content, so the border hugs the
	 * text instead of floating out at the full configured width. {@code hudWidth}
	 * is the cap (and the wrap point); a short objective shrinks the box, a long
	 * one fills it.
	 */
	private int fittedWidth(List<Row> rows) {
		Font font = Minecraft.getInstance().font;

		int widest = 0;
		for (Row row : rows) {
			if (row.text == null) {
				continue;
			}
			// Checkbox rows reserve a fixed strip on the right for the box.
			int reserve = row.checkbox >= 0 ? CHECKBOX : 0;
			widest = Math.max(widest, row.indent + font.width(row.text) + reserve);
		}

		// Never narrower than the icon, or a sane floor, and never past the cap.
		int floor = HudConfig.showIcon ? ICON + 40 : 60;
		int needed = PADDING * 2 + Math.max(widest, floor);
		return Math.min(HudConfig.hudWidth, needed);
	}

	/**
	 * Unscaled panel size as {width, height}.
	 *
	 * Width hugs the content up to {@code hudWidth}. With an explicit height the
	 * panel is exactly that tall (drag the bottom edge one-for-one); at height 0
	 * it sizes to its content.
	 */
	public int[] measure(@Nullable Quest quest) {
		ensureLayout(quest);
		return cachedSize;
	}

	private int[] sizeFor(List<Row> rows) {
		int width = fittedWidth(rows);

		if (HudConfig.hudHeight > 0) {
			return new int[] { width, HudConfig.hudHeight };
		}

		int textHeight = rows.size() * LINE;
		// The icon block is 20px tall; make sure the header never overlaps it.
		int minimum = HudConfig.showIcon ? ICON : 0;

		return new int[] { width, PADDING * 2 + Math.max(textHeight, minimum) };
	}

	// ------------------------------------------------------------------
	// Layout cache
	//
	// render() runs every frame (60-144 fps) and both measure() and drawPanel()
	// used to rebuild the wrapped-text layout each call. Component is re-shaped only
	// when something it depends on actually changes - quest progress, the pinned
	// quest, or the panel's size/toggles - which is at most a few times a second.
	// ------------------------------------------------------------------

	private List<Row> cachedRows;
	private int[] cachedSize;
	private String cacheKey;

	private void ensureLayout(@Nullable Quest quest) {
		String key = layoutKey(quest);
		if (cachedRows != null && key.equals(cacheKey)) {
			return;
		}
		cacheKey = key;
		cachedRows = layout(quest);
		cachedSize = sizeFor(cachedRows);
	}

	private String layoutKey(@Nullable Quest quest) {
		StringBuilder key = new StringBuilder(64);
		key.append(HudConfig.hudWidth).append(',').append(HudConfig.hudHeight)
				.append(HudConfig.showIcon ? 'I' : 'i')
				.append(HudConfig.showObjectives ? 'O' : 'o').append('|');
		appendQuestSignature(key, quest);
		key.append('|');
		appendQuestSignature(key, getPinnedQuest(quest));
		return key.toString();
	}

	private void appendQuestSignature(StringBuilder key, @Nullable Quest quest) {
		if (quest == null) {
			key.append('~');
			return;
		}
		PlayerQuestData data = ClientQuestState.get();
		key.append(quest.getId());
		for (QuestTask task : quest.getTasks()) {
			key.append(':').append(data.getProgress(quest.getId(), task.getId()));
		}
	}

	// ------------------------------------------------------------------
	// Rendering
	// ------------------------------------------------------------------

	private void render(GuiGraphicsExtractor graphics) {
		if (!HudConfig.enabled) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.options.hudHidden || client.options.debugEnabled) {
			return;
		}

		Quest quest = ClientQuestState.getCurrentMainQuest();
		if (quest == null && getPinnedQuest(null) == null) {
			return;
		}

		int screenWidth = client.getWindow().getGuiScaledWidth();
		int screenHeight = client.getWindow().getGuiScaledHeight();

		renderAt(graphics, HudConfig.hudX * screenWidth, HudConfig.hudY * screenHeight,
				HudConfig.hudScale, quest);
	}

	/** Draws the panel with its top-left corner at (originX, originY). */
	public void renderAt(GuiGraphicsExtractor graphics, double originX, double originY, double scale,
			@Nullable Quest quest) {
		HudRender.push(graphics, originX, originY, scale);
		try {
			drawPanel(graphics, quest);
		} finally {
			HudRender.pop(graphics);
		}
	}

	private void drawPanel(GuiGraphicsExtractor graphics, @Nullable Quest quest) {
		Minecraft client = Minecraft.getInstance();
		Font font = client.font;

		ensureLayout(quest);
		List<Row> rows = cachedRows;
		int width = cachedSize[0];
		int height = cachedSize[1];

		if (HudConfig.backgroundOpacity > 0) {
			graphics.fill(0, 0, width, height, HudConfig.backgroundColor(RGB_PANEL));

			int border = HudConfig.backgroundColor(RGB_BORDER);
			graphics.fill(0, 0, width, 1, border);
			graphics.fill(0, height - 1, width, height, border);
			graphics.fill(0, 0, 1, height, border);
			graphics.fill(width - 1, 0, width, height, border);
		}

		if (HudConfig.showIcon && quest != null) {
			client.getItemRenderer().renderInGuiWithOverrides(quest.getIconStack(), PADDING, PADDING + 1);
		}

		int y = PADDING;
		for (Row row : rows) {
			if (row.text == null) {
				// Spacer: draw the separator rule in the middle of the gap.
				graphics.fill(PADDING, y + LINE / 2, width - PADDING, y + LINE / 2 + 1,
						HudConfig.backgroundColor(RGB_BORDER));
				y += LINE;
				continue;
			}

			int x = PADDING + row.indent;
			graphics.text(font, row.text, x, y, HudConfig.applyTextAlpha(row.rgb));

			if (row.checkbox >= 0) {
				drawCheckbox(graphics, PADDING, y - 1, row.checkbox == 1);
			}

			y += LINE;
		}
	}

	/** Small square, filled green once the objective is satisfied. */
	private void drawCheckbox(GuiGraphicsExtractor graphics, int x, int y, boolean checked) {
		int border = HudConfig.applyTextAlpha(checked ? RGB_TASK_DONE : RGB_BORDER);
		int size = 9;

		graphics.fill(x, y, x + size, y + 1, border);
		graphics.fill(x, y + size - 1, x + size, y + size, border);
		graphics.fill(x, y, x + 1, y + size, border);
		graphics.fill(x + size - 1, y, x + size, y + size, border);

		if (checked) {
			graphics.fill(x + 2, y + 2, x + size - 2, y + size - 2,
					HudConfig.applyTextAlpha(RGB_TASK_DONE));
		}
	}
}
