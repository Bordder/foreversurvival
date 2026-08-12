package com.foreversurvival.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import com.mojang.blaze3d.systems.RenderSystem;

import com.foreversurvival.data.DeathRecord;
import com.foreversurvival.data.PlayerQuestData;
import com.foreversurvival.network.ModNetworking;
import com.foreversurvival.quest.Quest;
import com.foreversurvival.quest.QuestManager;
import com.foreversurvival.quest.QuestPhase;
import com.foreversurvival.quest.task.QuestTask;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Formatting;

/**
 * The quest book, opened with 'U'.
 *
 * Six tabs: Main, Side, Deaths, Stats, Summary, Settings.
 *
 * Locked quests are fully previewable but never completable - the server
 * enforces that independently, so previewing can never turn into progress.
 */
public class QuestScreen extends Screen {

	private enum Tab {
		MAIN("Main"),
		SIDE("Side"),
		DEATHS("Deaths"),
		STATS("Stats"),
		SUMMARY("Summary"),
		SETTINGS("Config");

		final String label;

		Tab(String label) {
			this.label = label;
		}
	}

	private static class Entry {

		final QuestPhase header;
		final Quest quest;
		final int height;

		Entry(QuestPhase header) {
			this.header = header;
			this.quest = null;
			this.height = 14;
		}

		Entry(Quest quest) {
			this.header = null;
			this.quest = quest;
			this.height = 20;
		}
	}

	/** A clickable manual-objective checkbox recorded during render. */
	private static class ManualHit {

		final String taskId;
		final boolean done;
		final int y1;
		final int y2;

		ManualHit(String taskId, boolean done, int y1, int y2) {
			this.taskId = taskId;
			this.done = done;
			this.y1 = y1;
			this.y2 = y2;
		}
	}

	private static final int TAB_HEIGHT = 18;
	private static final int LIST_WIDTH = 172;
	private static final int SEARCH_HEIGHT = 13;
	private static final int FOOTER_HEIGHT = 22;
	private static final int PADDING = 4;
	private static final int SETTINGS_ROW_HEIGHT = 20;
	private static final int CONTROL_WIDTH = 120;

	private static final int COLOR_PANEL = 0xF0121218;
	private static final int COLOR_SUBPANEL = 0xFF1B1B24;
	private static final int COLOR_BORDER = 0xFF3B3B4A;
	private static final int COLOR_TAB_ACTIVE = 0xFF2E3450;
	private static final int COLOR_TAB_IDLE = 0xFF1A1A22;
	private static final int COLOR_ROW_HOVER = 0xFF2B2B38;
	private static final int COLOR_ROW_SELECTED = 0xFF39456B;
	private static final int COLOR_HEADER_ROW = 0xFF15151C;
	private static final int COLOR_SLOT = 0xFF2A2A34;
	private static final int COLOR_BAR_BG = 0xFF33333D;
	private static final int COLOR_BAR_DONE = 0xFF4CBB55;
	private static final int COLOR_BAR_PARTIAL = 0xFFC9A227;
	private static final int COLOR_TRACK = 0xFF2A2A34;
	private static final int COLOR_KNOB = 0xFFFFD24A;

	private static final int TEXT_TITLE = 0xFFFFD24A;
	private static final int TEXT_BODY = 0xFFCFCFDA;
	private static final int TEXT_DIM = 0xFF7A7A8A;
	private static final int TEXT_LOCKED = 0xFF6E6E7C;
	private static final int TEXT_DONE = 0xFF6BD97A;
	private static final int TEXT_GUIDE = 0xFF9FD3FF;
	private static final int TEXT_TOOLS = 0xFFFFB36B;
	private static final int TEXT_DEATH = 0xFFE06A6A;
	private static final int TEXT_WARN = 0xFFE0A85A;

	private Tab tab = Tab.MAIN;

	private String selectedMainQuest = null;
	private String selectedSideQuest = null;

	private double listScroll;
	private double detailScroll;
	private double deathScroll;
	private double settingsScroll;

	private int draggingSlider = -1;

	private boolean searchActive;
	private String searchQuery = "";

	// Tree view state
	private static final int NODE = 20;
	private static final int NODE_STEP = 26;
	private static final int TREE_HEADER = 18;
	private static final int TREE_FOOTER = 40;
	private int treePhaseIndex;
	private double treeScroll;
	private int viewToggleX1;
	private int viewToggleX2;
	private int viewToggleY1;
	private int viewToggleY2;

	/** Index into the death list being inspected, or -1 for the list view. */
	private int viewingDeath = -1;

	private final List<ManualHit> manualHits = new ArrayList<>();
	private int pinButtonX1;
	private int pinButtonY1;
	private int pinButtonX2;
	private int pinButtonY2;
	private boolean pinButtonShown;

	private int left;
	private int top;
	private int panelWidth;
	private int panelHeight;
	private int contentTop;
	private int contentBottom;
	private int listLeft;
	private int listRight;
	private int detailLeft;
	private int detailRight;

	private int checkmarkButtonX1;
	private int checkmarkButtonY1;
	private int checkmarkButtonX2;
	private int checkmarkButtonY2;
	private String checkmarkTaskId;

	public QuestScreen() {
		super(new LiteralText("ForeverSurvival"));
	}

	@Override
	protected void init() {
		this.panelWidth = Math.min(this.width - 20, 460);
		this.panelHeight = Math.min(this.height - 20, 260);
		this.left = (this.width - panelWidth) / 2;
		this.top = (this.height - panelHeight) / 2;

		this.contentTop = top + TAB_HEIGHT + PADDING;
		this.contentBottom = top + panelHeight - PADDING;
		this.listLeft = left + PADDING;
		this.listRight = listLeft + LIST_WIDTH;
		this.detailLeft = listRight + 6;
		this.detailRight = left + panelWidth - PADDING;

		if (selectedMainQuest == null) {
			Quest current = ClientQuestState.getCurrentMainQuest();
			selectedMainQuest = current == null ? null : current.getId();
		}
		if (selectedSideQuest == null && !QuestManager.get().getSideQuests().isEmpty()) {
			selectedSideQuest = QuestManager.get().getSideQuests().get(0).getId();
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	// ------------------------------------------------------------------
	// Rendering
	// ------------------------------------------------------------------

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		this.renderBackground(matrices);

		fill(matrices, left - 1, top - 1, left + panelWidth + 1, top + panelHeight + 1, COLOR_BORDER);
		fill(matrices, left, top, left + panelWidth, top + panelHeight, COLOR_PANEL);

		renderTabs(matrices, mouseX, mouseY);

		checkmarkTaskId = null;
		pinButtonShown = false;
		manualHits.clear();

		switch (tab) {
			case MAIN -> {
				if (HudConfig.treeView) {
					renderTree(matrices, mouseX, mouseY, false);
				} else {
					renderSearch(matrices, mouseX, mouseY);
					renderQuestList(matrices, mouseX, mouseY, buildMainEntries(), selectedMainQuest);
					renderQuestDetail(matrices, mouseX, mouseY, selectedMainQuest);
				}
			}
			case SIDE -> {
				if (HudConfig.treeView) {
					renderTree(matrices, mouseX, mouseY, true);
				} else {
					renderSearch(matrices, mouseX, mouseY);
					renderQuestList(matrices, mouseX, mouseY, buildSideEntries(), selectedSideQuest);
					renderQuestDetail(matrices, mouseX, mouseY, selectedSideQuest);
				}
			}
			case DEATHS -> renderDeaths(matrices, mouseX, mouseY);
			case STATS -> renderStats(matrices);
			case SUMMARY -> renderSummary(matrices);
			case SETTINGS -> renderSettings(matrices, mouseX, mouseY);
		}

		super.render(matrices, mouseX, mouseY, delta);
	}

	private void renderTabs(MatrixStack matrices, int mouseX, int mouseY) {
		Tab[] tabs = Tab.values();
		int tabWidth = panelWidth / tabs.length;

		for (int i = 0; i < tabs.length; i++) {
			int x1 = left + i * tabWidth;
			int x2 = (i == tabs.length - 1) ? left + panelWidth : x1 + tabWidth;
			boolean active = tabs[i] == tab;
			boolean hovered = mouseX >= x1 && mouseX < x2 && mouseY >= top && mouseY < top + TAB_HEIGHT;

			fill(matrices, x1, top, x2, top + TAB_HEIGHT, active ? COLOR_TAB_ACTIVE
					: (hovered ? COLOR_ROW_HOVER : COLOR_TAB_IDLE));
			fill(matrices, x1, top + TAB_HEIGHT - 1, x2, top + TAB_HEIGHT, COLOR_BORDER);

			String label = tabs[i].label;
			int textX = x1 + (x2 - x1 - textRenderer.getWidth(label)) / 2;
			textRenderer.draw(matrices, label, textX, top + 5, active ? TEXT_TITLE : TEXT_DIM);
		}
	}

	// ------------------------------------------------------------------
	// Search
	// ------------------------------------------------------------------

	private int listTop() {
		return contentTop + SEARCH_HEIGHT;
	}

	private void renderSearch(MatrixStack matrices, int mouseX, int mouseY) {
		int y1 = contentTop;
		int y2 = contentTop + SEARCH_HEIGHT;
		boolean hovered = mouseX >= listLeft && mouseX < listRight && mouseY >= y1 && mouseY < y2;

		fill(matrices, listLeft, y1, listRight, y2,
				searchActive ? COLOR_ROW_SELECTED : (hovered ? COLOR_ROW_HOVER : COLOR_HEADER_ROW));

		drawMagnifier(matrices, listLeft + 3, y1 + 3, searchActive ? TEXT_TITLE : TEXT_DIM);

		String text;
		int color;
		if (searchActive) {
			text = searchQuery + "_";
			color = TEXT_BODY;
		} else if (!searchQuery.isEmpty()) {
			text = searchQuery;
			color = TEXT_BODY;
		} else {
			text = "Search";
			color = TEXT_DIM;
		}
		textRenderer.draw(matrices, trim(text, LIST_WIDTH - 52), listLeft + 15, y1 + 3, color);

		drawViewToggle(matrices, listRight - 34, y1, listRight, y2, mouseX, mouseY);
	}

	/** Small List/Tree switch, shown in both layouts. */
	private void drawViewToggle(MatrixStack matrices, int x1, int y1, int x2, int y2,
			int mouseX, int mouseY) {
		viewToggleX1 = x1;
		viewToggleY1 = y1;
		viewToggleX2 = x2;
		viewToggleY2 = y2;

		boolean hovered = mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
		fill(matrices, x1, y1, x2, y2, hovered ? COLOR_ROW_SELECTED : COLOR_TAB_ACTIVE);
		drawBorder(matrices, x1, y1, x2, y2);

		String label = HudConfig.treeView ? "Tree" : "List";
		textRenderer.draw(matrices, label,
				x1 + (x2 - x1 - textRenderer.getWidth(label)) / 2, y1 + 3, TEXT_TITLE);
	}

	/** Magnifying glass drawn from blocks - no font glyph needed. */
	private void drawMagnifier(MatrixStack matrices, int x, int y, int color) {
		fill(matrices, x + 1, y, x + 5, y + 1, color);
		fill(matrices, x, y + 1, x + 1, y + 5, color);
		fill(matrices, x + 5, y + 1, x + 6, y + 5, color);
		fill(matrices, x + 1, y + 5, x + 5, y + 6, color);
		fill(matrices, x + 5, y + 5, x + 8, y + 8, color);
	}

	private boolean matchesSearch(Quest quest) {
		if (searchQuery.isEmpty()) {
			return true;
		}
		String needle = searchQuery.toLowerCase(Locale.ROOT);
		return quest.getTitle().toLowerCase(Locale.ROOT).contains(needle)
				|| quest.getDescription().toLowerCase(Locale.ROOT).contains(needle)
				|| quest.getGuideText().toLowerCase(Locale.ROOT).contains(needle);
	}

	// ------------------------------------------------------------------
	// Quest list
	// ------------------------------------------------------------------

	private List<Entry> buildMainEntries() {
		List<Entry> entries = new ArrayList<>();
		QuestPhase current = null;

		for (Quest quest : QuestManager.get().getMainQuests()) {
			if (!matchesSearch(quest)) {
				continue;
			}
			if (quest.getPhase() != current) {
				current = quest.getPhase();
				entries.add(new Entry(current));
			}
			entries.add(new Entry(quest));
		}
		return entries;
	}

	private List<Entry> buildSideEntries() {
		List<Entry> entries = new ArrayList<>();
		boolean any = false;
		for (Quest quest : QuestManager.get().getSideQuests()) {
			if (!matchesSearch(quest)) {
				continue;
			}
			if (!any) {
				entries.add(new Entry(QuestPhase.SIDE));
				any = true;
			}
			entries.add(new Entry(quest));
		}
		return entries;
	}

	private void renderQuestList(MatrixStack matrices, int mouseX, int mouseY, List<Entry> entries,
			String selectedId) {
		int top = listTop();
		fill(matrices, listLeft, top, listRight, contentBottom, COLOR_SUBPANEL);

		PlayerQuestData data = ClientQuestState.get();
		int totalHeight = 0;
		for (Entry entry : entries) {
			totalHeight += entry.height;
		}
		listScroll = clampScroll(listScroll, totalHeight, contentBottom - top);

		enableScissor(listLeft, top + 1, listRight, contentBottom - 1);

		int y = top - (int) listScroll;

		for (Entry entry : entries) {
			int rowTop = y;
			int rowBottom = y + entry.height;
			y = rowBottom;

			if (rowBottom <= top || rowTop >= contentBottom) {
				continue;
			}

			if (entry.header != null) {
				fill(matrices, listLeft, rowTop, listRight, rowBottom, COLOR_HEADER_ROW);
				textRenderer.draw(matrices, trim(entry.header.getDisplayName(), LIST_WIDTH - 8),
						listLeft + 4, rowTop + 3, colorOf(entry.header.getColor()));
				continue;
			}

			Quest quest = entry.quest;
			boolean unlocked = QuestManager.get().isUnlocked(data, quest);
			boolean completed = data.isCompleted(quest.getId());
			boolean selected = quest.getId().equals(selectedId);
			boolean hovered = mouseX >= listLeft && mouseX < listRight
					&& mouseY >= Math.max(rowTop, top) && mouseY < Math.min(rowBottom, contentBottom);

			if (selected) {
				fill(matrices, listLeft, rowTop, listRight, rowBottom, COLOR_ROW_SELECTED);
			} else if (hovered) {
				fill(matrices, listLeft, rowTop, listRight, rowBottom, COLOR_ROW_HOVER);
			}

			MinecraftClient.getInstance().getItemRenderer()
					.renderInGuiWithOverrides(quest.getIconStack(), listLeft + 3, rowTop + 2);
			if (!unlocked) {
				fill(matrices, listLeft + 3, rowTop + 2, listLeft + 19, rowTop + 18, 0x99161620);
			}

			int titleColor = !unlocked ? TEXT_LOCKED : (completed ? TEXT_DONE : TEXT_BODY);
			textRenderer.draw(matrices, trim(quest.getTitle(), LIST_WIDTH - 44), listLeft + 22,
					rowTop + 3, titleColor);

			if (quest.getId().equals(HudConfig.pinnedQuestId)) {
				drawPin(matrices, listRight - 24, rowTop + 5, TEXT_GUIDE);
			}

			if (completed) {
				drawTick(matrices, listRight - 12, rowTop + 6, TEXT_DONE);
				textRenderer.draw(matrices, "Complete", listLeft + 22, rowTop + 12, TEXT_DONE);
			} else if (unlocked) {
				drawProgressBar(matrices, listLeft + 22, rowTop + 13, LIST_WIDTH - 30,
						questProgress(data, quest), questTotal(quest));
			} else {
				drawLock(matrices, listRight - 12, rowTop + 5, TEXT_LOCKED);
				textRenderer.draw(matrices, "Locked - preview", listLeft + 22, rowTop + 12, TEXT_LOCKED);
			}
		}

		disableScissor();
		drawBorder(matrices, listLeft, top, listRight, contentBottom);
	}

	// ------------------------------------------------------------------
	// Tree view
	// ------------------------------------------------------------------

	private static final QuestPhase[] PHASE_PAGES = {
			QuestPhase.PHASE_1, QuestPhase.PHASE_2, QuestPhase.PHASE_3,
			QuestPhase.PHASE_4, QuestPhase.PHASE_5, QuestPhase.PHASE_6
	};

	/** Current page, clamped - the arrows stop at the ends rather than wrapping. */
	private int phaseIndex() {
		return HudConfig.clamp(treePhaseIndex, 0, PHASE_PAGES.length - 1);
	}

	/** The quests on the current tree page, in chain order. */
	private List<Quest> treeQuests(boolean side) {
		List<Quest> out = new ArrayList<>();

		if (side) {
			for (Quest quest : QuestManager.get().getSideQuests()) {
				if (matchesSearch(quest)) {
					out.add(quest);
				}
			}
			return out;
		}

		QuestPhase phase = PHASE_PAGES[phaseIndex()];
		for (Quest quest : QuestManager.get().getMainQuests()) {
			if (quest.getPhase() == phase && matchesSearch(quest)) {
				out.add(quest);
			}
		}
		return out;
	}

	private int treeCols() {
		return Math.max(1, (panelWidth - 24) / NODE_STEP);
	}

	private int treeGridTop() {
		return contentTop + TREE_HEADER + 4;
	}

	private int treeGridBottom() {
		return contentBottom - TREE_FOOTER;
	}

	/** Snake layout: rows alternate direction so the chain reads continuously. */
	private int[] nodePos(int index) {
		int cols = treeCols();
		int row = index / cols;
		int col = index % cols;
		if (row % 2 == 1) {
			col = cols - 1 - col;
		}
		return new int[] {
				left + PADDING + 8 + col * NODE_STEP,
				treeGridTop() + row * NODE_STEP - (int) treeScroll
		};
	}

	private int[] treeArrowLeft() {
		return new int[] { left + PADDING + 6, contentTop + 2, left + PADDING + 20, contentTop + 16 };
	}

	/** Sits clear of the List/Tree button, which owns the last 34px + a 6px gap. */
	private int[] treeArrowRight() {
		int x = left + panelWidth - PADDING - 58;
		return new int[] { x, contentTop + 2, x + 14, contentTop + 16 };
	}

	private int[] treeOpenButton() {
		int x2 = left + panelWidth - PADDING - 8;
		return new int[] { x2 - 44, contentBottom - 18, x2, contentBottom - 5 };
	}

	private void renderTree(MatrixStack matrices, int mouseX, int mouseY, boolean side) {
		int panelLeft = left + PADDING;
		int panelRight = left + panelWidth - PADDING;
		fill(matrices, panelLeft, contentTop, panelRight, contentBottom, COLOR_SUBPANEL);

		PlayerQuestData data = ClientQuestState.get();
		List<Quest> quests = treeQuests(side);
		Quest current = ClientQuestState.getCurrentMainQuest();

		// ---- Header ----
		int done = 0;
		for (Quest quest : quests) {
			if (data.isCompleted(quest.getId())) {
				done++;
			}
		}

		String title = side ? "Side Challenges" : PHASE_PAGES[phaseIndex()].getDisplayName();
		int titleColor = side ? colorOf(QuestPhase.SIDE.getColor())
				: colorOf(PHASE_PAGES[phaseIndex()].getColor());

		fill(matrices, panelLeft, contentTop, panelRight, contentTop + TREE_HEADER, COLOR_HEADER_ROW);

		// Right-to-left layout so nothing can ever overlap: the List/Tree button
		// owns the far right, then the next arrow, then the counter, and the
		// title takes whatever is left.
		drawViewToggle(matrices, panelRight - 38, contentTop + 2, panelRight - 4, contentTop + 16,
				mouseX, mouseY);

		int rightLimit = panelRight - 38 - 6;
		if (!side) {
			int[] leftArrow = treeArrowLeft();
			int[] rightArrow = treeArrowRight();
			drawArrow(matrices, leftArrow, true, mouseX, mouseY, phaseIndex() > 0);
			drawArrow(matrices, rightArrow, false, mouseX, mouseY,
					phaseIndex() < PHASE_PAGES.length - 1);
			rightLimit = rightArrow[0] - 6;
		}

		String counts = quests.size() + " quests - " + done + " done";
		int countsX = rightLimit - textRenderer.getWidth(counts);
		textRenderer.draw(matrices, counts, countsX, contentTop + 5, TEXT_DIM);

		int titleX = panelLeft + (side ? 8 : 26);
		textRenderer.draw(matrices, trim(title, Math.max(20, countsX - titleX - 8)),
				titleX, contentTop + 5, titleColor);

		// ---- Grid ----
		int cols = treeCols();
		int rows = (quests.size() + cols - 1) / cols;
		int contentHeight = rows * NODE_STEP + 6;
		treeScroll = clampScroll(treeScroll, contentHeight, treeGridBottom() - treeGridTop());

		enableScissor(panelLeft, treeGridTop() - 2, panelRight, treeGridBottom());

		// Connectors first so the nodes sit on top of them.
		for (int i = 0; i + 1 < quests.size(); i++) {
			int[] a = nodePos(i);
			int[] b = nodePos(i + 1);
			int ax = a[0] + NODE / 2;
			int ay = a[1] + NODE / 2;
			int bx = b[0] + NODE / 2;
			int by = b[1] + NODE / 2;

			int colour = data.isCompleted(quests.get(i).getId()) ? 0xFF3A6B42 : 0xFF33333D;

			if (ay == by) {
				fill(matrices, Math.min(ax, bx), ay - 1, Math.max(ax, bx), ay + 1, colour);
			} else {
				// Row wrap: drop straight down in the shared column.
				fill(matrices, ax - 1, Math.min(ay, by), ax + 1, Math.max(ay, by), colour);
			}
		}

		Quest hovered = null;
		for (int i = 0; i < quests.size(); i++) {
			Quest quest = quests.get(i);
			int[] pos = nodePos(i);
			if (pos[1] + NODE < treeGridTop() - 2 || pos[1] > treeGridBottom()) {
				continue;
			}

			boolean over = mouseX >= pos[0] && mouseX < pos[0] + NODE
					&& mouseY >= pos[1] && mouseY < pos[1] + NODE
					&& mouseY >= treeGridTop() - 2 && mouseY < treeGridBottom();
			if (over) {
				hovered = quest;
			}

			drawNode(matrices, pos[0], pos[1], quest, data, current, over, side);
		}

		disableScissor();

		// ---- Footer ----
		fill(matrices, panelLeft, contentBottom - TREE_FOOTER, panelRight, contentBottom - TREE_FOOTER + 1,
				COLOR_BORDER);

		String selectedId = side ? selectedSideQuest : selectedMainQuest;
		Quest focus = hovered != null ? hovered
				: (selectedId == null ? null : QuestManager.get().getQuest(selectedId));

		if (focus != null) {
			boolean unlocked = QuestManager.get().isUnlocked(data, focus);
			boolean complete = data.isCompleted(focus.getId());

			int textY = contentBottom - TREE_FOOTER + 6;
			int colour = complete ? TEXT_DONE : (unlocked ? TEXT_TITLE : TEXT_LOCKED);
			textRenderer.draw(matrices, trim(focus.getTitle(), panelRight - panelLeft - 70),
					panelLeft + 8, textY, colour);

			StringBuilder line = new StringBuilder();
			for (QuestTask task : focus.getTasks()) {
				if (line.length() > 0) {
					line.append("  -  ");
				}
				int progress = Math.min(task.getRequired(),
						data.getProgress(focus.getId(), task.getId()));
				line.append(task.getDescription());
				if (task.getRequired() > 1) {
					line.append(' ').append(progress).append('/').append(task.getRequired());
				}
				if (line.length() > 120) {
					break;
				}
			}
			textRenderer.draw(matrices, trim(line.toString(), panelRight - panelLeft - 70),
					panelLeft + 8, textY + 11, unlocked ? TEXT_BODY : TEXT_LOCKED);

			if (!unlocked) {
				textRenderer.draw(matrices, "Locked - preview only", panelLeft + 8, textY + 22,
						TEXT_WARN);
			}

			int[] open = treeOpenButton();
			boolean openHover = mouseX >= open[0] && mouseX <= open[2]
					&& mouseY >= open[1] && mouseY <= open[3];
			fill(matrices, open[0], open[1], open[2], open[3],
					openHover ? COLOR_ROW_SELECTED : COLOR_TAB_ACTIVE);
			drawBorder(matrices, open[0], open[1], open[2], open[3]);
			textRenderer.draw(matrices, "Open",
					open[0] + (44 - textRenderer.getWidth("Open")) / 2, open[1] + 3, TEXT_TITLE);
		} else {
			textRenderer.draw(matrices, "Hover or click a quest", panelLeft + 8,
					contentBottom - TREE_FOOTER + 6, TEXT_DIM);
		}

		drawBorder(matrices, panelLeft, contentTop, panelRight, contentBottom);
	}

	private void drawNode(MatrixStack matrices, int x, int y, Quest quest, PlayerQuestData data,
			Quest current, boolean hovered, boolean side) {
		boolean unlocked = QuestManager.get().isUnlocked(data, quest);
		boolean complete = data.isCompleted(quest.getId());
		boolean isCurrent = current != null && current.getId().equals(quest.getId());
		String selectedId = side ? selectedSideQuest : selectedMainQuest;
		boolean selected = quest.getId().equals(selectedId);

		int background;
		int border;
		if (complete) {
			background = 0xFF1E3A22;
			border = TEXT_DONE;
		} else if (isCurrent) {
			background = 0xFF3A3520;
			border = TEXT_TITLE;
		} else if (unlocked) {
			background = COLOR_SLOT;
			border = COLOR_BORDER;
		} else {
			background = 0xFF15151C;
			border = 0xFF2A2A33;
		}
		if (selected || hovered) {
			border = 0xFFFFFFFF;
		}

		fill(matrices, x, y, x + NODE, y + NODE, background);
		fill(matrices, x, y, x + NODE, y + 1, border);
		fill(matrices, x, y + NODE - 1, x + NODE, y + NODE, border);
		fill(matrices, x, y, x + 1, y + NODE, border);
		fill(matrices, x + NODE - 1, y, x + NODE, y + NODE, border);

		MinecraftClient.getInstance().getItemRenderer()
				.renderInGuiWithOverrides(quest.getIconStack(), x + 2, y + 2);

		if (!unlocked) {
			fill(matrices, x + 1, y + 1, x + NODE - 1, y + NODE - 1, 0xAA12121A);
			drawLock(matrices, x + 6, y + 5, TEXT_LOCKED);
		} else if (complete) {
			drawTick(matrices, x + NODE - 9, y + NODE - 9, TEXT_DONE);
		}

		if (quest.getId().equals(HudConfig.pinnedQuestId)) {
			drawPin(matrices, x + 1, y + 1, TEXT_GUIDE);
		}
	}

	private void drawArrow(MatrixStack matrices, int[] rect, boolean pointLeft, int mouseX, int mouseY,
			boolean enabled) {
		boolean hovered = enabled && mouseX >= rect[0] && mouseX <= rect[2]
				&& mouseY >= rect[1] && mouseY <= rect[3];

		fill(matrices, rect[0], rect[1], rect[2], rect[3],
				!enabled ? COLOR_TAB_IDLE : (hovered ? COLOR_ROW_SELECTED : COLOR_TAB_ACTIVE));
		drawBorder(matrices, rect[0], rect[1], rect[2], rect[3]);

		int colour = enabled ? TEXT_TITLE : TEXT_LOCKED;
		int cx = (rect[0] + rect[2]) / 2;
		int cy = (rect[1] + rect[3]) / 2;
		for (int i = 0; i < 4; i++) {
			int dx = pointLeft ? i : -i;
			fill(matrices, cx + dx - 1, cy - i, cx + dx, cy + i + 1, colour);
		}
	}

	// ------------------------------------------------------------------
	// Quest detail
	// ------------------------------------------------------------------

	private void renderQuestDetail(MatrixStack matrices, int mouseX, int mouseY, String selectedId) {
		fill(matrices, detailLeft, contentTop, detailRight, contentBottom, COLOR_SUBPANEL);

		Quest quest = selectedId == null ? null : QuestManager.get().getQuest(selectedId);
		PlayerQuestData data = ClientQuestState.get();

		if (quest == null) {
			textRenderer.draw(matrices, "Select a quest", detailLeft + 6, contentTop + 6, TEXT_DIM);
			drawBorder(matrices, detailLeft, contentTop, detailRight, contentBottom);
			return;
		}

		boolean unlocked = QuestManager.get().isUnlocked(data, quest);
		boolean completed = data.isCompleted(quest.getId());

		String pendingManualTaskId = null;
		if (unlocked && !completed) {
			for (QuestTask task : quest.getTasks()) {
				if (task.isManual() && !QuestManager.get().isTaskComplete(data, quest, task)) {
					pendingManualTaskId = task.getId();
					break;
				}
			}
		}
		boolean showFooter = pendingManualTaskId != null;

		int innerLeft = detailLeft + 6;
		int wrapWidth = detailRight - detailLeft - 12;
		int bodyBottom = showFooter ? contentBottom - FOOTER_HEIGHT : contentBottom;

		enableScissor(detailLeft, contentTop + 1, detailRight, bodyBottom - 1);

		int y = contentTop + 5 - (int) detailScroll;
		int startY = y;

		y = drawLine(matrices, quest.getTitle(), innerLeft, y, unlocked ? TEXT_TITLE : TEXT_LOCKED);
		y = drawLine(matrices, quest.getPhase().getDisplayName(), innerLeft, y,
				colorOf(quest.getPhase().getColor()));
		y += 3;

		// Pin toggle
		boolean pinned = quest.getId().equals(HudConfig.pinnedQuestId);
		pinButtonX1 = detailRight - 46;
		pinButtonY1 = contentTop + 4;
		pinButtonX2 = detailRight - 6;
		pinButtonY2 = contentTop + 17;
		pinButtonShown = true;
		boolean pinHover = mouseX >= pinButtonX1 && mouseX <= pinButtonX2
				&& mouseY >= pinButtonY1 && mouseY <= pinButtonY2;
		fill(matrices, pinButtonX1, pinButtonY1, pinButtonX2, pinButtonY2,
				pinHover ? COLOR_ROW_SELECTED : COLOR_TAB_ACTIVE);
		drawBorder(matrices, pinButtonX1, pinButtonY1, pinButtonX2, pinButtonY2);
		String pinLabel = pinned ? "Unpin" : "Pin";
		textRenderer.draw(matrices, pinLabel,
				pinButtonX1 + (pinButtonX2 - pinButtonX1 - textRenderer.getWidth(pinLabel)) / 2,
				pinButtonY1 + 3, pinned ? TEXT_GUIDE : TEXT_DIM);

		if (completed) {
			drawTick(matrices, innerLeft, y + 1, TEXT_DONE);
			y = drawLine(matrices, "   COMPLETED", innerLeft, y, TEXT_DONE);
			y += 2;
		} else if (!unlocked) {
			Quest parent = quest.getParentId() == null ? null
					: QuestManager.get().getQuest(quest.getParentId());
			String parentName = parent == null ? "the previous quest" : parent.getTitle();
			y = drawWrapped(matrices, "LOCKED - finish \"" + parentName + "\" first. "
					+ "Nothing here counts until then.", innerLeft, y, wrapWidth, TEXT_WARN);
			y += 4;
		}

		y = drawWrapped(matrices, quest.getDescription(), innerLeft, y, wrapWidth, TEXT_BODY);
		y += 5;

		if (!quest.getRequiredTools().isEmpty()) {
			y = drawLine(matrices, "REQUIRED:", innerLeft, y, TEXT_TOOLS);
			y = drawWrapped(matrices, String.join(", ", quest.getRequiredTools()),
					innerLeft, y, wrapWidth, TEXT_TOOLS);
			y += 5;
		}

		if (!quest.getGuideText().isEmpty()) {
			y = drawLine(matrices, "GUIDE:", innerLeft, y, TEXT_GUIDE);
			y = drawWrapped(matrices, quest.getGuideText(), innerLeft, y, wrapWidth, TEXT_GUIDE);
			y += 6;
		}

		y = drawLine(matrices, "OBJECTIVES:", innerLeft, y, unlocked ? TEXT_TITLE : TEXT_LOCKED);
		for (QuestTask task : quest.getTasks()) {
			int progress = Math.min(task.getRequired(), data.getProgress(quest.getId(), task.getId()));
			boolean done = progress >= task.getRequired();

			String label = task.getTypeLabel() + ": " + task.getDescription();
			if (task.getRequired() > 1) {
				label = label + "  " + progress + "/" + task.getRequired();
			}

			int color = !unlocked ? TEXT_LOCKED : (done ? TEXT_DONE : TEXT_BODY);
			if (done) {
				drawTick(matrices, innerLeft, y + 1, TEXT_DONE);
			} else {
				drawEmptyBox(matrices, innerLeft, y + 1, color);
			}

			// Manual objectives can be toggled straight from this checkbox.
			if (task.isManual() && unlocked && !completed
					&& y >= contentTop && y < bodyBottom - 8) {
				manualHits.add(new ManualHit(task.getId(), done, y - 1, y + 9));
			}

			y = drawWrapped(matrices, label, innerLeft + 12, y, wrapWidth - 12, color);

			if (task.getRequired() > 1 && unlocked) {
				drawProgressBar(matrices, innerLeft + 12, y + 1, wrapWidth - 16, progress,
						task.getRequired());
			}
			y += 8;
		}

		disableScissor();

		int documentHeight = y - startY + 6;
		detailScroll = clampScroll(detailScroll, documentHeight, bodyBottom - contentTop);

		if (showFooter) {
			checkmarkTaskId = pendingManualTaskId;
			checkmarkButtonX1 = detailLeft + 6;
			checkmarkButtonY1 = contentBottom - FOOTER_HEIGHT + 3;
			checkmarkButtonX2 = detailRight - 6;
			checkmarkButtonY2 = contentBottom - 4;

			boolean hovered = mouseX >= checkmarkButtonX1 && mouseX <= checkmarkButtonX2
					&& mouseY >= checkmarkButtonY1 && mouseY <= checkmarkButtonY2;

			fill(matrices, checkmarkButtonX1, checkmarkButtonY1, checkmarkButtonX2, checkmarkButtonY2,
					hovered ? COLOR_ROW_SELECTED : COLOR_TAB_ACTIVE);
			drawBorder(matrices, checkmarkButtonX1, checkmarkButtonY1, checkmarkButtonX2, checkmarkButtonY2);

			String label = "Mark objective complete";
			int textX = checkmarkButtonX1
					+ (checkmarkButtonX2 - checkmarkButtonX1 - textRenderer.getWidth(label)) / 2;
			textRenderer.draw(matrices, label, textX, checkmarkButtonY1 + 5, TEXT_TITLE);
		}

		drawBorder(matrices, detailLeft, contentTop, detailRight, contentBottom);
	}

	// ------------------------------------------------------------------
	// Deaths
	// ------------------------------------------------------------------

	private void renderDeaths(MatrixStack matrices, int mouseX, int mouseY) {
		int panelLeft = left + PADDING;
		int panelRight = left + panelWidth - PADDING;
		fill(matrices, panelLeft, contentTop, panelRight, contentBottom, COLOR_SUBPANEL);

		List<DeathRecord> deaths = ClientQuestState.get().getDeaths();

		if (viewingDeath >= 0 && viewingDeath < deaths.size()) {
			renderDeathInventory(matrices, deaths.get(viewingDeath), panelLeft, panelRight);
			drawBorder(matrices, panelLeft, contentTop, panelRight, contentBottom);
			return;
		}

		if (deaths.isEmpty()) {
			textRenderer.draw(matrices, "No deaths recorded. Yet.", panelLeft + 6, contentTop + 6, TEXT_DIM);
			drawBorder(matrices, panelLeft, contentTop, panelRight, contentBottom);
			return;
		}

		int rowHeight = 21;
		int headerHeight = 14;
		int totalHeight = headerHeight + deaths.size() * rowHeight + 6;
		deathScroll = clampScroll(deathScroll, totalHeight, contentBottom - contentTop);

		enableScissor(panelLeft, contentTop + 1, panelRight, contentBottom - 1);

		int y = contentTop + 5 - (int) deathScroll;
		textRenderer.draw(matrices, "Recorded deaths: " + deaths.size()
				+ " (newest first - click one to see your inventory)", panelLeft + 6, y, TEXT_DIM);
		y += headerHeight;

		for (int i = deaths.size() - 1; i >= 0; i--) {
			DeathRecord record = deaths.get(i);
			boolean hovered = mouseX >= panelLeft && mouseX < panelRight
					&& mouseY >= Math.max(y - 2, contentTop) && mouseY < Math.min(y + 18, contentBottom);

			if (hovered && record.hasInventory()) {
				fill(matrices, panelLeft + 1, y - 2, panelRight - 1, y + 18, COLOR_ROW_HOVER);
			}

			String line = "Death #" + (i + 1) + ": "
					+ record.getX() + ", " + record.getY() + ", " + record.getZ()
					+ " [" + record.getDimensionDisplayName() + "]";
			textRenderer.draw(matrices, trim(line, panelRight - panelLeft - 12), panelLeft + 6, y,
					TEXT_DEATH);

			String cause = record.getCause().isEmpty() ? "Unknown cause" : record.getCause();
			if (!record.hasInventory()) {
				cause = cause + "  (inventory not kept)";
			}
			textRenderer.draw(matrices, trim(cause, panelRight - panelLeft - 12), panelLeft + 6, y + 10,
					TEXT_DIM);

			y += rowHeight;
		}

		disableScissor();
		drawBorder(matrices, panelLeft, contentTop, panelRight, contentBottom);
	}

	private void renderDeathInventory(MatrixStack matrices, DeathRecord record, int panelLeft,
			int panelRight) {
		String header = "Death #" + (viewingDeath + 1) + " - "
				+ record.getX() + ", " + record.getY() + ", " + record.getZ()
				+ " [" + record.getDimensionDisplayName() + "]";
		textRenderer.draw(matrices, trim(header, panelRight - panelLeft - 60), panelLeft + 6,
				contentTop + 5, TEXT_DEATH);
		textRenderer.draw(matrices, trim(record.getCause(), panelRight - panelLeft - 60),
				panelLeft + 6, contentTop + 15, TEXT_DIM);

		// Back button
		int bx2 = panelRight - 6;
		int bx1 = bx2 - 44;
		fill(matrices, bx1, contentTop + 4, bx2, contentTop + 17, COLOR_TAB_ACTIVE);
		drawBorder(matrices, bx1, contentTop + 4, bx2, contentTop + 17);
		textRenderer.draw(matrices, "Back", bx1 + (44 - textRenderer.getWidth("Back")) / 2,
				contentTop + 7, TEXT_TITLE);

		ItemStack[] slots = readInventory(record);

		int gridLeft = panelLeft + 10;
		int gridTop = contentTop + 32;

		// Main inventory rows (slots 9-35) then the hotbar (slots 0-8).
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				drawSlot(matrices, gridLeft + col * 18, gridTop + row * 18, slots[9 + row * 9 + col]);
			}
		}
		for (int col = 0; col < 9; col++) {
			drawSlot(matrices, gridLeft + col * 18, gridTop + 3 * 18 + 4, slots[col]);
		}

		// Armour and offhand off to the right.
		int sideLeft = gridLeft + 9 * 18 + 12;
		textRenderer.draw(matrices, "Armour", sideLeft, gridTop - 10, TEXT_DIM);
		for (int i = 0; i < 4; i++) {
			drawSlot(matrices, sideLeft, gridTop + i * 18, slots[36 + i]);
		}
		textRenderer.draw(matrices, "Off", sideLeft + 22, gridTop - 10, TEXT_DIM);
		drawSlot(matrices, sideLeft + 22, gridTop, slots[40]);
	}

	/**
	 * Rebuilds the 41-slot layout from the stored vanilla inventory list.
	 * Vanilla writes main slots as 0-35, armour as 100+i and the offhand as 150.
	 */
	private ItemStack[] readInventory(DeathRecord record) {
		ItemStack[] slots = new ItemStack[41];
		for (int i = 0; i < slots.length; i++) {
			slots[i] = ItemStack.EMPTY;
		}

		NbtList list = record.getInventory();
		if (list == null) {
			return slots;
		}

		for (int i = 0; i < list.size(); i++) {
			NbtCompound entry = list.getCompound(i);
			int slot = entry.getByte("Slot") & 255;
			ItemStack stack = ItemStack.fromNbt(entry);
			if (stack.isEmpty()) {
				continue;
			}

			if (slot < 36) {
				slots[slot] = stack;
			} else if (slot >= 100 && slot < 104) {
				slots[36 + (slot - 100)] = stack;
			} else if (slot >= 150 && slot < 154) {
				slots[40] = stack;
			}
		}
		return slots;
	}

	private void drawSlot(MatrixStack matrices, int x, int y, ItemStack stack) {
		fill(matrices, x, y, x + 16, y + 16, COLOR_SLOT);
		if (stack == null || stack.isEmpty()) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		client.getItemRenderer().renderInGuiWithOverrides(stack, x, y);
		client.getItemRenderer().renderGuiItemOverlay(textRenderer, stack, x, y);
	}

	// ------------------------------------------------------------------
	// Stats
	// ------------------------------------------------------------------

	private void renderStats(MatrixStack matrices) {
		int panelLeft = left + PADDING;
		int panelRight = left + panelWidth - PADDING;
		fill(matrices, panelLeft, contentTop, panelRight, contentBottom, COLOR_SUBPANEL);

		NbtCompound stats = ClientQuestState.getStats();
		PlayerQuestData data = ClientQuestState.get();

		enableScissor(panelLeft, contentTop + 1, panelRight, contentBottom - 1);

		int x = panelLeft + 8;
		int y = contentTop + 7;

		textRenderer.draw(matrices, "This World", x, y, TEXT_TITLE);
		y += 14;

		int mainDone = 0;
		for (Quest quest : QuestManager.get().getMainQuests()) {
			if (data.isCompleted(quest.getId())) {
				mainDone++;
			}
		}
		int sideDone = 0;
		for (Quest quest : QuestManager.get().getSideQuests()) {
			if (data.isCompleted(quest.getId())) {
				sideDone++;
			}
		}

		int playTicks = stats.getInt("PlayTime");
		double hours = playTicks / 20.0D / 3600.0D;

		y = stat(matrices, x, y, "Playtime", formatDuration(playTicks));
		y = stat(matrices, x, y, "Quests completed", mainDone + " main, " + sideDone + " side");
		if (hours > 0.05D) {
			y = stat(matrices, x, y, "Quests per hour",
					String.format(Locale.ROOT, "%.1f", (mainDone + sideDone) / hours));
		}
		y = stat(matrices, x, y, "Since last death", formatDuration(stats.getInt("SinceDeath")));
		y += 6;

		y = stat(matrices, x, y, "Deaths", Integer.toString(stats.getInt("Deaths")));
		y = stat(matrices, x, y, "Deaths logged", Integer.toString(data.getDeaths().size()));
		y = stat(matrices, x, y, "Mobs killed", Integer.toString(stats.getInt("MobKills")));
		y = stat(matrices, x, y, "Damage taken",
				String.format(Locale.ROOT, "%.1f hearts", stats.getInt("DamageTaken") / 20.0D));
		y += 6;

		y = stat(matrices, x, y, "Distance walked", formatDistance(stats.getInt("WalkCm")));
		y = stat(matrices, x, y, "Distance sprinted", formatDistance(stats.getInt("SprintCm")));
		y = stat(matrices, x, y, "Distance flown", formatDistance(stats.getInt("FlyCm")));
		y = stat(matrices, x, y, "Jumps", Integer.toString(stats.getInt("Jumps")));
		stat(matrices, x, y, "Nights slept", Integer.toString(stats.getInt("Slept")));

		disableScissor();
		drawBorder(matrices, panelLeft, contentTop, panelRight, contentBottom);
	}

	private int stat(MatrixStack matrices, int x, int y, String label, String value) {
		textRenderer.draw(matrices, label, x, y, TEXT_DIM);
		textRenderer.draw(matrices, value, x + 150, y, TEXT_BODY);
		return y + 11;
	}

	private static String formatDuration(int ticks) {
		long seconds = ticks / 20L;
		long hours = seconds / 3600L;
		long minutes = (seconds % 3600L) / 60L;
		if (hours > 0) {
			return hours + "h " + minutes + "m";
		}
		return minutes + "m";
	}

	private static String formatDistance(int centimetres) {
		double metres = centimetres / 100.0D;
		if (metres >= 1000.0D) {
			return String.format(Locale.ROOT, "%.1f km", metres / 1000.0D);
		}
		return String.format(Locale.ROOT, "%.0f m", metres);
	}

	// ------------------------------------------------------------------
	// Summary
	// ------------------------------------------------------------------

	private void renderSummary(MatrixStack matrices) {
		int panelLeft = left + PADDING;
		int panelRight = left + panelWidth - PADDING;
		fill(matrices, panelLeft, contentTop, panelRight, contentBottom, COLOR_SUBPANEL);

		PlayerQuestData data = ClientQuestState.get();
		int x = panelLeft + 8;
		int y = contentTop + 7;

		int mainTotal = QuestManager.get().getMainQuests().size();
		int mainDone = 0;
		for (Quest quest : QuestManager.get().getMainQuests()) {
			if (data.isCompleted(quest.getId())) {
				mainDone++;
			}
		}

		int sideTotal = QuestManager.get().getSideQuests().size();
		int sideDone = 0;
		for (Quest quest : QuestManager.get().getSideQuests()) {
			if (data.isCompleted(quest.getId())) {
				sideDone++;
			}
		}

		int percent = mainTotal == 0 ? 0 : (mainDone * 100 / mainTotal);

		enableScissor(panelLeft, contentTop + 1, panelRight, contentBottom - 1);

		textRenderer.draw(matrices, "ForeverSurvival - Progress Summary", x, y, TEXT_TITLE);
		y += 15;
		textRenderer.draw(matrices, "Main line: " + mainDone + " / " + mainTotal + "  (" + percent + "%)",
				x, y, TEXT_BODY);
		y += 13;
		drawProgressBar(matrices, x, y, panelRight - x - 8, mainDone, Math.max(1, mainTotal));
		y += 12;

		for (QuestPhase phase : QuestPhase.values()) {
			if (phase.isSide()) {
				continue;
			}

			int total = 0;
			int done = 0;
			for (Quest quest : QuestManager.get().getMainQuests()) {
				if (quest.getPhase() == phase) {
					total++;
					if (data.isCompleted(quest.getId())) {
						done++;
					}
				}
			}
			if (total == 0) {
				continue;
			}

			String line = phase.getDisplayName() + ": " + done + " / " + total;
			textRenderer.draw(matrices, line, x, y, done == total ? TEXT_DONE : colorOf(phase.getColor()));
			if (done == total) {
				drawTick(matrices, x + textRenderer.getWidth(line) + 4, y + 1, TEXT_DONE);
			}
			y += 11;
		}

		y += 5;
		textRenderer.draw(matrices, "Side Challenges: " + sideDone + " / " + sideTotal, x, y, TEXT_BODY);
		y += 11;
		textRenderer.draw(matrices, "Deaths recorded: " + data.getDeaths().size(), x, y, TEXT_DEATH);
		y += 14;

		Quest current = ClientQuestState.getCurrentMainQuest();
		textRenderer.draw(matrices, "Current objective:", x, y, TEXT_DIM);
		y += 11;
		textRenderer.draw(matrices, current == null ? "All main quests complete." : current.getTitle(),
				x, y, TEXT_TITLE);

		disableScissor();
		drawBorder(matrices, panelLeft, contentTop, panelRight, contentBottom);
	}

	// ------------------------------------------------------------------
	// Settings
	// ------------------------------------------------------------------

	private abstract static class Setting {

		final String label;

		Setting(String label) {
			this.label = label;
		}
	}

	private static final class Header extends Setting {

		Header(String label) {
			super(label);
		}
	}

	private static final class Toggle extends Setting {

		final BooleanSupplier getter;
		final Runnable flip;

		Toggle(String label, BooleanSupplier getter, Runnable flip) {
			super(label);
			this.getter = getter;
			this.flip = flip;
		}
	}

	private static final class Slider extends Setting {

		final int min;
		final int max;
		final IntSupplier getter;
		final IntConsumer setter;
		final String suffix;

		Slider(String label, int min, int max, IntSupplier getter, IntConsumer setter, String suffix) {
			super(label);
			this.min = min;
			this.max = max;
			this.getter = getter;
			this.setter = setter;
			this.suffix = suffix;
		}
	}

	private static final class Action extends Setting {

		final Runnable run;

		Action(String label, Runnable run) {
			super(label);
			this.run = run;
		}
	}

	private List<Setting> buildSettings() {
		List<Setting> settings = new ArrayList<>();

		settings.add(new Header("Objective overlay"));
		settings.add(new Toggle("Show overlay", () -> HudConfig.enabled,
				() -> HudConfig.enabled = !HudConfig.enabled));
		settings.add(new Toggle("Show objectives", () -> HudConfig.showObjectives,
				() -> HudConfig.showObjectives = !HudConfig.showObjectives));
		settings.add(new Toggle("Show quest icon", () -> HudConfig.showIcon,
				() -> HudConfig.showIcon = !HudConfig.showIcon));
		settings.add(new Slider("Background opacity", 0, 100,
				() -> HudConfig.backgroundOpacity, v -> HudConfig.backgroundOpacity = v, "%"));
		settings.add(new Slider("Text opacity", 20, 100,
				() -> HudConfig.textOpacity, v -> HudConfig.textOpacity = v, "%"));
		settings.add(new Slider("Panel width", HudConfig.MIN_HUD_WIDTH, HudConfig.MAX_HUD_WIDTH,
				() -> HudConfig.hudWidth, v -> HudConfig.hudWidth = v, "px"));
		settings.add(new Slider("Panel min height", 0, HudConfig.MAX_HUD_HEIGHT,
				() -> HudConfig.hudMinHeight, v -> HudConfig.hudMinHeight = v, "px"));
		settings.add(new Slider("Panel zoom", 50, 200,
				() -> (int) Math.round(HudConfig.hudScale * 100),
				v -> HudConfig.hudScale = v / 100.0D, "%"));

		settings.add(new Header("Locator bar"));
		settings.add(new Toggle("Show locator bar", () -> HudConfig.locatorEnabled,
				() -> HudConfig.locatorEnabled = !HudConfig.locatorEnabled));
		settings.add(new Toggle("In XP bar slot (off = free)", () -> HudConfig.locatorXpBarMode,
				() -> HudConfig.locatorXpBarMode = !HudConfig.locatorXpBarMode));
		settings.add(new Slider("Swap every", 2, 60,
				() -> HudConfig.locatorSwapSeconds, v -> HudConfig.locatorSwapSeconds = v, "s"));
		settings.add(new Toggle("Show distance", () -> HudConfig.locatorShowDistance,
				() -> HudConfig.locatorShowDistance = !HudConfig.locatorShowDistance));
		settings.add(new Slider("Bar width", HudConfig.MIN_BAR_WIDTH, HudConfig.MAX_BAR_WIDTH,
				() -> HudConfig.locatorWidth, v -> HudConfig.locatorWidth = v, "px"));
		settings.add(new Slider("Bar height", HudConfig.MIN_BAR_HEIGHT, HudConfig.MAX_BAR_HEIGHT,
				() -> HudConfig.locatorHeight, v -> HudConfig.locatorHeight = v, "px"));
		settings.add(new Slider("Bar arc", 30, 180,
				() -> HudConfig.locatorFov, v -> HudConfig.locatorFov = v, " deg"));
		settings.add(new Slider("Bar zoom", 50, 200,
				() -> (int) Math.round(HudConfig.locatorScale * 100),
				v -> HudConfig.locatorScale = v / 100.0D, "%"));

		settings.add(new Header("Quest browser"));
		settings.add(new Toggle("Tree view (off = list)", () -> HudConfig.treeView,
				() -> {
					HudConfig.treeView = !HudConfig.treeView;
					searchQuery = "";
					searchActive = false;
				}));

		settings.add(new Header("Game"));
		settings.add(new Toggle("Hide vanilla advancement pop-ups",
				() -> HudConfig.hideAdvancementToasts,
				() -> HudConfig.hideAdvancementToasts = !HudConfig.hideAdvancementToasts));

		settings.add(new Header("Layout"));
		settings.add(new Action("Move and resize...",
				() -> this.client.setScreen(new HudEditScreen(this))));
		settings.add(new Action("Reset everything", HudConfig::resetToDefaults));

		return settings;
	}

	private int settingsRowTop(int index) {
		return contentTop + 6 + index * SETTINGS_ROW_HEIGHT - (int) settingsScroll;
	}

	private int controlLeft() {
		return left + panelWidth - PADDING - 8 - CONTROL_WIDTH;
	}

	private int controlRight() {
		return left + panelWidth - PADDING - 8;
	}

	private void renderSettings(MatrixStack matrices, int mouseX, int mouseY) {
		int panelLeft = left + PADDING;
		int panelRight = left + panelWidth - PADDING;
		fill(matrices, panelLeft, contentTop, panelRight, contentBottom, COLOR_SUBPANEL);

		List<Setting> settings = buildSettings();
		int totalHeight = settings.size() * SETTINGS_ROW_HEIGHT + 12;
		settingsScroll = clampScroll(settingsScroll, totalHeight, contentBottom - contentTop);

		enableScissor(panelLeft, contentTop + 1, panelRight, contentBottom - 1);

		for (int i = 0; i < settings.size(); i++) {
			Setting setting = settings.get(i);
			int rowTop = settingsRowTop(i);
			if (rowTop + SETTINGS_ROW_HEIGHT < contentTop || rowTop > contentBottom) {
				continue;
			}

			if (setting instanceof Header) {
				textRenderer.draw(matrices, setting.label, panelLeft + 8, rowTop + 5, TEXT_TITLE);
				fill(matrices, panelLeft + 8, rowTop + 16, panelRight - 8, rowTop + 17, COLOR_BORDER);
				continue;
			}

			textRenderer.draw(matrices, trim(setting.label, controlLeft() - panelLeft - 16),
					panelLeft + 8, rowTop + 5, TEXT_BODY);

			int cl = controlLeft();
			int cr = controlRight();
			boolean hovered = mouseX >= cl && mouseX <= cr
					&& mouseY >= rowTop + 1 && mouseY <= rowTop + 16;

			if (setting instanceof Toggle toggle) {
				boolean on = toggle.getter.getAsBoolean();
				fill(matrices, cl, rowTop + 2, cr, rowTop + 16,
						hovered ? COLOR_ROW_SELECTED : COLOR_TAB_ACTIVE);
				drawBorder(matrices, cl, rowTop + 2, cr, rowTop + 16);
				String value = on ? "On" : "Off";
				textRenderer.draw(matrices, value,
						cl + (cr - cl - textRenderer.getWidth(value)) / 2, rowTop + 5,
						on ? TEXT_DONE : TEXT_DIM);

			} else if (setting instanceof Slider slider) {
				int value = slider.getter.getAsInt();
				double fraction = (double) (value - slider.min) / (slider.max - slider.min);

				int trackY = rowTop + 8;
				fill(matrices, cl, trackY, cr, trackY + 2, COLOR_TRACK);
				int filled = cl + (int) Math.round((cr - cl) * fraction);
				fill(matrices, cl, trackY, filled, trackY + 2, COLOR_BAR_PARTIAL);

				int knobX = Math.min(cr - 4, Math.max(cl, filled - 2));
				fill(matrices, knobX, rowTop + 3, knobX + 4, rowTop + 15, COLOR_KNOB);

				String text = value + slider.suffix;
				textRenderer.draw(matrices, text, cl - 6 - textRenderer.getWidth(text), rowTop + 5,
						TEXT_DIM);

			} else if (setting instanceof Action) {
				fill(matrices, cl, rowTop + 2, cr, rowTop + 16,
						hovered ? COLOR_ROW_SELECTED : COLOR_TAB_ACTIVE);
				drawBorder(matrices, cl, rowTop + 2, cr, rowTop + 16);
				String value = "Open";
				textRenderer.draw(matrices, value,
						cl + (cr - cl - textRenderer.getWidth(value)) / 2, rowTop + 5, TEXT_TITLE);
			}
		}

		disableScissor();
		drawBorder(matrices, panelLeft, contentTop, panelRight, contentBottom);
	}

	private void applySlider(Slider slider, double mouseX) {
		int cl = controlLeft();
		int cr = controlRight();
		double fraction = (mouseX - cl) / (double) (cr - cl);
		fraction = Math.max(0.0D, Math.min(1.0D, fraction));

		slider.setter.accept(slider.min + (int) Math.round(fraction * (slider.max - slider.min)));
	}

	// ------------------------------------------------------------------
	// Input
	// ------------------------------------------------------------------

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) {
			return super.mouseClicked(mouseX, mouseY, button);
		}

		if (mouseY >= top && mouseY < top + TAB_HEIGHT
				&& mouseX >= left && mouseX < left + panelWidth) {
			Tab[] tabs = Tab.values();
			int tabWidth = panelWidth / tabs.length;
			int index = Math.min(tabs.length - 1, (int) ((mouseX - left) / tabWidth));
			if (tabs[index] != tab) {
				tab = tabs[index];
				listScroll = 0;
				detailScroll = 0;
				deathScroll = 0;
				settingsScroll = 0;
				searchActive = false;
				viewingDeath = -1;
			}
			return true;
		}

		if (tab == Tab.SETTINGS) {
			List<Setting> settings = buildSettings();
			for (int i = 0; i < settings.size(); i++) {
				Setting setting = settings.get(i);
				int rowTop = settingsRowTop(i);
				if (mouseY < rowTop + 1 || mouseY > rowTop + 16) {
					continue;
				}
				if (mouseX < controlLeft() || mouseX > controlRight()) {
					continue;
				}

				if (setting instanceof Toggle toggle) {
					toggle.flip.run();
					HudConfig.save();
				} else if (setting instanceof Slider slider) {
					draggingSlider = i;
					applySlider(slider, mouseX);
				} else if (setting instanceof Action action) {
					action.run.run();
				}
				return true;
			}
			return true;
		}

		if (tab == Tab.DEATHS) {
			return handleDeathClick(mouseX, mouseY);
		}

		if (tab == Tab.MAIN || tab == Tab.SIDE) {
			// List/Tree switch, drawn in both layouts.
			if (mouseX >= viewToggleX1 && mouseX <= viewToggleX2
					&& mouseY >= viewToggleY1 && mouseY <= viewToggleY2) {
				HudConfig.treeView = !HudConfig.treeView;
				HudConfig.save();
				// Clear the filter on switch: the tree has no search box, and a
				// filter still applied there would hide quests with no clue why.
				searchActive = false;
				searchQuery = "";
				treeScroll = 0;
				listScroll = 0;
				return true;
			}

			if (HudConfig.treeView) {
				return handleTreeClick(mouseX, mouseY, tab == Tab.SIDE);
			}

			// Search strip
			if (mouseX >= listLeft && mouseX < listRight
					&& mouseY >= contentTop && mouseY < contentTop + SEARCH_HEIGHT) {
				searchActive = !searchActive;
				return true;
			}
			searchActive = false;

			// Pin toggle
			if (pinButtonShown && mouseX >= pinButtonX1 && mouseX <= pinButtonX2
					&& mouseY >= pinButtonY1 && mouseY <= pinButtonY2) {
				String id = tab == Tab.MAIN ? selectedMainQuest : selectedSideQuest;
				if (id != null) {
					HudConfig.pinnedQuestId = id.equals(HudConfig.pinnedQuestId) ? "" : id;
					HudConfig.save();
				}
				return true;
			}

			// Manual objective checkboxes toggle directly
			for (ManualHit hit : manualHits) {
				if (mouseY >= hit.y1 && mouseY <= hit.y2
						&& mouseX >= detailLeft + 4 && mouseX <= detailLeft + 20) {
					String questId = tab == Tab.MAIN ? selectedMainQuest : selectedSideQuest;
					if (questId != null) {
						sendCheckmark(questId, hit.taskId, !hit.done);
					}
					return true;
				}
			}

			if (checkmarkTaskId != null
					&& mouseX >= checkmarkButtonX1 && mouseX <= checkmarkButtonX2
					&& mouseY >= checkmarkButtonY1 && mouseY <= checkmarkButtonY2) {
				String questId = tab == Tab.MAIN ? selectedMainQuest : selectedSideQuest;
				if (questId != null) {
					sendCheckmark(questId, checkmarkTaskId, true);
				}
				return true;
			}

			if (mouseX >= listLeft && mouseX < listRight
					&& mouseY >= listTop() && mouseY < contentBottom) {
				List<Entry> entries = tab == Tab.MAIN ? buildMainEntries() : buildSideEntries();
				int y = listTop() - (int) listScroll;

				for (Entry entry : entries) {
					int rowTop = y;
					int rowBottom = y + entry.height;
					y = rowBottom;

					if (mouseY < rowTop || mouseY >= rowBottom || entry.quest == null) {
						continue;
					}

					if (tab == Tab.MAIN) {
						selectedMainQuest = entry.quest.getId();
					} else {
						selectedSideQuest = entry.quest.getId();
					}
					detailScroll = 0;
					return true;
				}
				return true;
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	private boolean handleTreeClick(double mouseX, double mouseY, boolean side) {
		if (!side) {
			// Clamped, not wrapping: phase 6 is the end of the line.
			int[] leftArrow = treeArrowLeft();
			if (mouseX >= leftArrow[0] && mouseX <= leftArrow[2]
					&& mouseY >= leftArrow[1] && mouseY <= leftArrow[3]) {
				treePhaseIndex = Math.max(0, phaseIndex() - 1);
				treeScroll = 0;
				return true;
			}

			int[] rightArrow = treeArrowRight();
			if (mouseX >= rightArrow[0] && mouseX <= rightArrow[2]
					&& mouseY >= rightArrow[1] && mouseY <= rightArrow[3]) {
				treePhaseIndex = Math.min(PHASE_PAGES.length - 1, phaseIndex() + 1);
				treeScroll = 0;
				return true;
			}
		}

		// "Open" jumps back to the list layout focused on the selected quest.
		int[] open = treeOpenButton();
		if (mouseX >= open[0] && mouseX <= open[2] && mouseY >= open[1] && mouseY <= open[3]) {
			HudConfig.treeView = false;
			HudConfig.save();
			return true;
		}

		List<Quest> quests = treeQuests(side);
		for (int i = 0; i < quests.size(); i++) {
			int[] pos = nodePos(i);
			if (mouseX >= pos[0] && mouseX < pos[0] + NODE
					&& mouseY >= pos[1] && mouseY < pos[1] + NODE
					&& mouseY >= treeGridTop() - 2 && mouseY < treeGridBottom()) {

				if (side) {
					selectedSideQuest = quests.get(i).getId();
				} else {
					selectedMainQuest = quests.get(i).getId();
				}
				detailScroll = 0;
				return true;
			}
		}
		return true;
	}

	private boolean handleDeathClick(double mouseX, double mouseY) {
		List<DeathRecord> deaths = ClientQuestState.get().getDeaths();
		int panelRight = left + panelWidth - PADDING;

		if (viewingDeath >= 0) {
			int bx2 = panelRight - 6;
			int bx1 = bx2 - 44;
			if (mouseX >= bx1 && mouseX <= bx2
					&& mouseY >= contentTop + 4 && mouseY <= contentTop + 17) {
				viewingDeath = -1;
			}
			return true;
		}

		int rowHeight = 21;
		int y = contentTop + 5 - (int) deathScroll + 14;
		for (int i = deaths.size() - 1; i >= 0; i--) {
			if (mouseY >= y - 2 && mouseY < y + 18 && mouseY >= contentTop && mouseY < contentBottom) {
				if (deaths.get(i).hasInventory()) {
					viewingDeath = i;
				}
				return true;
			}
			y += rowHeight;
		}
		return true;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (tab == Tab.SETTINGS && draggingSlider >= 0) {
			List<Setting> settings = buildSettings();
			if (draggingSlider < settings.size()
					&& settings.get(draggingSlider) instanceof Slider slider) {
				applySlider(slider, mouseX);
				return true;
			}
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (draggingSlider >= 0) {
			draggingSlider = -1;
			HudConfig.save();
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		double step = amount * 12.0D;

		switch (tab) {
			case DEATHS -> deathScroll = Math.max(0, deathScroll - step);
			case SETTINGS -> settingsScroll = Math.max(0, settingsScroll - step);
			case STATS, SUMMARY -> {
				// Nothing to scroll.
			}
			default -> {
				if (HudConfig.treeView) {
					treeScroll = Math.max(0, treeScroll - step);
				} else if (mouseX >= detailLeft) {
					detailScroll = Math.max(0, detailScroll - step);
				} else {
					listScroll = Math.max(0, listScroll - step);
				}
			}
		}
		return true;
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (searchActive && chr >= ' ' && searchQuery.length() < 40) {
			searchQuery += chr;
			listScroll = 0;
			return true;
		}
		return super.charTyped(chr, modifiers);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (searchActive) {
			// 259 = backspace, 256 = escape, 257 = enter
			if (keyCode == 259) {
				if (!searchQuery.isEmpty()) {
					searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
					listScroll = 0;
				}
				return true;
			}
			if (keyCode == 256 || keyCode == 257) {
				searchActive = false;
				if (keyCode == 256) {
					searchQuery = "";
				}
				return true;
			}
			// Swallow everything else so 'U' does not close the screen mid-word.
			return true;
		}

		if (ForeverSurvivalClient.openQuestsKey != null
				&& ForeverSurvivalClient.openQuestsKey.matchesKey(keyCode, scanCode)) {
			this.close();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private void sendCheckmark(String questId, String taskId, boolean set) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(questId);
		buf.writeString(taskId);
		buf.writeBoolean(set);
		ClientPlayNetworking.send(ModNetworking.CHECKMARK, buf);
	}

	// ------------------------------------------------------------------
	// Drawing helpers
	// ------------------------------------------------------------------

	private void enableScissor(int x1, int y1, int x2, int y2) {
		Window window = MinecraftClient.getInstance().getWindow();
		double scale = window.getScaleFactor();

		int sx = (int) (x1 * scale);
		int sy = (int) ((window.getScaledHeight() - y2) * scale);
		int sw = (int) ((x2 - x1) * scale);
		int sh = (int) ((y2 - y1) * scale);

		RenderSystem.enableScissor(sx, sy, Math.max(0, sw), Math.max(0, sh));
	}

	private void disableScissor() {
		RenderSystem.disableScissor();
	}

	private void drawTick(MatrixStack matrices, int x, int y, int color) {
		fill(matrices, x + 1, y + 4, x + 3, y + 6, color);
		fill(matrices, x + 2, y + 5, x + 4, y + 7, color);
		fill(matrices, x + 3, y + 3, x + 5, y + 5, color);
		fill(matrices, x + 4, y + 1, x + 6, y + 3, color);
		fill(matrices, x + 5, y, x + 7, y + 2, color);
	}

	private void drawEmptyBox(MatrixStack matrices, int x, int y, int color) {
		int size = 7;
		fill(matrices, x, y, x + size, y + 1, color);
		fill(matrices, x, y + size - 1, x + size, y + size, color);
		fill(matrices, x, y, x + 1, y + size, color);
		fill(matrices, x + size - 1, y, x + size, y + size, color);
	}

	private void drawLock(MatrixStack matrices, int x, int y, int color) {
		fill(matrices, x + 1, y, x + 6, y + 1, color);
		fill(matrices, x, y + 1, x + 1, y + 4, color);
		fill(matrices, x + 6, y + 1, x + 7, y + 4, color);
		fill(matrices, x - 1, y + 4, x + 8, y + 10, color);
	}

	private void drawPin(MatrixStack matrices, int x, int y, int color) {
		fill(matrices, x + 2, y, x + 5, y + 5, color);
		fill(matrices, x, y + 5, x + 7, y + 6, color);
		fill(matrices, x + 3, y + 6, x + 4, y + 10, color);
	}

	private int drawLine(MatrixStack matrices, String text, int x, int y, int color) {
		textRenderer.draw(matrices, text, x, y, color);
		return y + 10;
	}

	private int drawWrapped(MatrixStack matrices, String text, int x, int y, int width, int color) {
		if (text == null || text.isEmpty()) {
			return y;
		}

		List<OrderedText> lines = textRenderer.wrapLines(new LiteralText(text), width);
		for (OrderedText line : lines) {
			textRenderer.draw(matrices, line, x, y, color);
			y += 9;
		}
		return y;
	}

	private void drawProgressBar(MatrixStack matrices, int x, int y, int width, int value, int max) {
		int height = 3;
		fill(matrices, x, y, x + width, y + height, COLOR_BAR_BG);

		int clamped = Math.max(0, Math.min(value, max));
		int filled = max <= 0 ? width : (int) ((long) width * clamped / max);
		if (filled > 0) {
			fill(matrices, x, y, x + filled, y + height, clamped >= max ? COLOR_BAR_DONE : COLOR_BAR_PARTIAL);
		}
	}

	private void drawBorder(MatrixStack matrices, int x1, int y1, int x2, int y2) {
		fill(matrices, x1, y1, x2, y1 + 1, COLOR_BORDER);
		fill(matrices, x1, y2 - 1, x2, y2, COLOR_BORDER);
		fill(matrices, x1, y1, x1 + 1, y2, COLOR_BORDER);
		fill(matrices, x2 - 1, y1, x2, y2, COLOR_BORDER);
	}

	private String trim(String text, int maxWidth) {
		if (textRenderer.getWidth(text) <= maxWidth) {
			return text;
		}
		return textRenderer.trimToWidth(text, Math.max(0, maxWidth - textRenderer.getWidth("..."))) + "...";
	}

	private double clampScroll(double scroll, int contentHeight, int viewHeight) {
		int maxScroll = Math.max(0, contentHeight - viewHeight);
		return Math.max(0, Math.min(scroll, maxScroll));
	}

	private static int colorOf(Formatting formatting) {
		Integer value = formatting.getColorValue();
		return 0xFF000000 | (value == null ? 0xFFFFFF : value);
	}

	private static int questProgress(PlayerQuestData data, Quest quest) {
		int total = 0;
		for (QuestTask task : quest.getTasks()) {
			total += Math.min(task.getRequired(), data.getProgress(quest.getId(), task.getId()));
		}
		return total;
	}

	private static int questTotal(Quest quest) {
		int total = 0;
		for (QuestTask task : quest.getTasks()) {
			total += task.getRequired();
		}
		return Math.max(1, total);
	}
}
