package com.foreversurvival.client;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import com.mojang.blaze3d.systems.RenderSystem;

import com.foreversurvival.data.DeathRecord;
import com.foreversurvival.data.PlayerQuestData;
import com.foreversurvival.network.ModPayloads;
import com.foreversurvival.quest.Quest;
import com.foreversurvival.quest.QuestManager;
import com.foreversurvival.quest.QuestPhase;
import com.foreversurvival.quest.task.QuestTask;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.resources.RegistryOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.ChatFormatting;

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
	private double statsScroll;

	/** Dropdowns in the Stats tab. */
	private boolean statsTimesOpen;
	private boolean statsCombatOpen;

	private int draggingSlider = -1;

	private boolean searchActive;
	private String searchQuery = "";

	/** Phases folded shut in the list. Static so it survives closing the book. */
	private static final Set<QuestPhase> collapsedPhases = EnumSet.noneOf(QuestPhase.class);

	// Tree view state
	private static final int NODE = 30;
	private static final int NODE_STEP = 40;
	private static final int TREE_HEADER = 18;
	private static final int TREE_FOOTER = 40;
	private int treePhaseIndex;
	private double treeScroll;
	private int viewToggleX1;
	private int viewToggleX2;
	private int viewToggleY1;
	private int viewToggleY2;

	// Scrollbars
	private static final int SB_LIST = 0;
	private static final int SB_DETAIL = 1;
	private static final int SB_DEATHS = 2;
	private static final int SB_SETTINGS = 3;
	private static final int SB_TREE = 4;
	private static final int SB_STATS = 5;
	private static final int SB_WIDTH = 5;
	private static final int SB_MIN_THUMB = 14;

	/** Geometry of each scroll region, recorded during render for hit-testing. */
	private static final class Bar {

		int x;
		int y1;
		int y2;
		int contentHeight;
		int viewHeight;
		boolean active;
	}

	private final Bar[] scrollBars = new Bar[6];
	private int scrollDrag = -1;
	/** Cursor offset inside the thumb when the drag started. */
	private double scrollGrab;

	{
		for (int i = 0; i < scrollBars.length; i++) {
			scrollBars[i] = new Bar();
		}
	}

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
		super(Component.literal("ForeverSurvival"));
	}

	@Override
	protected void init() {
		// Tree view wants room to breathe, so the book is a bit larger now; it
		// still shrinks to fit small windows via the min().
		this.panelWidth = Math.min(this.width - 32, 540);
		this.panelHeight = Math.min(this.height - 32, 300);
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
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			float delta) {
		this.extractBackground(graphics, mouseX, mouseY, delta);

		graphics.fill(left - 1, top - 1, left + panelWidth + 1, top + panelHeight + 1, COLOR_BORDER);
		graphics.fill(left, top, left + panelWidth, top + panelHeight, COLOR_PANEL);

		renderTabs(graphics, mouseX, mouseY);

		checkmarkTaskId = null;
		pinButtonShown = false;
		manualHits.clear();
		clearScrollbars();

		switch (tab) {
			case MAIN -> {
				if (HudConfig.treeView) {
					renderTree(graphics, mouseX, mouseY, false);
				} else {
					renderSearch(graphics, mouseX, mouseY);
					renderQuestList(graphics, mouseX, mouseY, buildMainEntries(), selectedMainQuest);
					renderQuestDetail(graphics, mouseX, mouseY, selectedMainQuest);
				}
			}
			case SIDE -> {
				if (HudConfig.treeView) {
					renderTree(graphics, mouseX, mouseY, true);
				} else {
					renderSearch(graphics, mouseX, mouseY);
					renderQuestList(graphics, mouseX, mouseY, buildSideEntries(), selectedSideQuest);
					renderQuestDetail(graphics, mouseX, mouseY, selectedSideQuest);
				}
			}
			case DEATHS -> renderDeaths(graphics, mouseX, mouseY);
			case STATS -> renderStats(graphics, mouseX, mouseY);
			case SUMMARY -> renderSummary(graphics);
			case SETTINGS -> renderSettings(graphics, mouseX, mouseY);
		}

		super.extractRenderState(graphics, mouseX, mouseY, delta);
	}

	private void renderTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		Tab[] tabs = Tab.values();
		int tabWidth = panelWidth / tabs.length;

		for (int i = 0; i < tabs.length; i++) {
			int x1 = left + i * tabWidth;
			int x2 = (i == tabs.length - 1) ? left + panelWidth : x1 + tabWidth;
			boolean active = tabs[i] == tab;
			boolean hovered = mouseX >= x1 && mouseX < x2 && mouseY >= top && mouseY < top + TAB_HEIGHT;

			graphics.fill(x1, top, x2, top + TAB_HEIGHT, active ? COLOR_TAB_ACTIVE
					: (hovered ? COLOR_ROW_HOVER : COLOR_TAB_IDLE));
			graphics.fill(x1, top + TAB_HEIGHT - 1, x2, top + TAB_HEIGHT, COLOR_BORDER);

			String label = tabs[i].label;
			int textX = x1 + (x2 - x1 - font.width(label)) / 2;
			graphics.text(font, label, (int) (textX), (int) (top + 5), active ? TEXT_TITLE : TEXT_DIM);
		}
	}

	// ------------------------------------------------------------------
	// Search
	// ------------------------------------------------------------------

	private int listTop() {
		return contentTop + SEARCH_HEIGHT;
	}

	private void renderSearch(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int y1 = contentTop;
		int y2 = contentTop + SEARCH_HEIGHT;
		boolean hovered = mouseX >= listLeft && mouseX < listRight && mouseY >= y1 && mouseY < y2;

		graphics.fill(listLeft, y1, listRight, y2,
				searchActive ? COLOR_ROW_SELECTED : (hovered ? COLOR_ROW_HOVER : COLOR_HEADER_ROW));

		drawMagnifier(graphics, listLeft + 3, y1 + 3, searchActive ? TEXT_TITLE : TEXT_DIM);

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
		graphics.text(font, trim(text, LIST_WIDTH - 52), (int) (listLeft + 15), (int) (y1 + 3), color);

		drawViewToggle(graphics, listRight - 34, y1, listRight, y2, mouseX, mouseY);
	}

	/** Small List/Tree switch, shown in both layouts. */
	private void drawViewToggle(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2,
			int mouseX, int mouseY) {
		viewToggleX1 = x1;
		viewToggleY1 = y1;
		viewToggleX2 = x2;
		viewToggleY2 = y2;

		boolean hovered = mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
		graphics.fill(x1, y1, x2, y2, hovered ? COLOR_ROW_SELECTED : COLOR_TAB_ACTIVE);
		drawBorder(graphics, x1, y1, x2, y2);

		String label = HudConfig.treeView ? "Tree" : "List";
		graphics.text(font, label, (int) (x1 + (x2 - x1 - font.width(label)) / 2), (int) (y1 + 3), TEXT_TITLE);
	}

	/** Magnifying glass drawn from blocks - no font glyph needed. */
	private void drawMagnifier(GuiGraphicsExtractor graphics, int x, int y, int color) {
		graphics.fill(x + 1, y, x + 5, y + 1, color);
		graphics.fill(x, y + 1, x + 1, y + 5, color);
		graphics.fill(x + 5, y + 1, x + 6, y + 5, color);
		graphics.fill(x + 1, y + 5, x + 5, y + 6, color);
		graphics.fill(x + 5, y + 5, x + 8, y + 8, color);
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
			// A collapsed phase keeps its header but hides its quests. A live
			// search overrides collapse so matches are never hidden.
			if (searchQuery.isEmpty() && collapsedPhases.contains(current)) {
				continue;
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

	private void renderQuestList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, List<Entry> entries,
			String selectedId) {
		int top = listTop();
		graphics.fill(listLeft, top, listRight, contentBottom, COLOR_SUBPANEL);

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
				boolean headerHover = mouseX >= listLeft && mouseX < listRight
						&& mouseY >= Math.max(rowTop, top) && mouseY < Math.min(rowBottom, contentBottom);
				graphics.fill(listLeft, rowTop, listRight, rowBottom,
						headerHover ? COLOR_ROW_HOVER : COLOR_HEADER_ROW);

				boolean collapsed = collapsedPhases.contains(entry.header);
				int colour = colorOf(entry.header.getColor());
				drawFoldArrow(graphics, listLeft + 4, rowTop + 4, collapsed, colour);

				String label = trim(entry.header.getDisplayName(), LIST_WIDTH - 40);
				graphics.text(font, label, (int) (listLeft + 13), (int) (rowTop + 3), colour);

				// When folded, show how many quests are hidden.
				if (collapsed) {
					int count = phaseQuestCount(entry.header);
					String badge = "(" + count + ")";
					graphics.text(font, badge, (int) (listRight - 6 - font.width(badge)), (int) (rowTop + 3), TEXT_DIM);
				}
				continue;
			}

			Quest quest = entry.quest;
			boolean unlocked = QuestManager.get().isUnlocked(data, quest);
			boolean completed = data.isCompleted(quest.getId());
			boolean selected = quest.getId().equals(selectedId);
			boolean hovered = mouseX >= listLeft && mouseX < listRight
					&& mouseY >= Math.max(rowTop, top) && mouseY < Math.min(rowBottom, contentBottom);

			if (selected) {
				graphics.fill(listLeft, rowTop, listRight, rowBottom, COLOR_ROW_SELECTED);
			} else if (hovered) {
				graphics.fill(listLeft, rowTop, listRight, rowBottom, COLOR_ROW_HOVER);
			}

			Minecraft.getInstance().getItemRenderer()
					.renderInGuiWithOverrides(quest.getIconStack(), listLeft + 3, rowTop + 2);
			if (!unlocked) {
				graphics.fill(listLeft + 3, rowTop + 2, listLeft + 19, rowTop + 18, 0x99161620);
			}

			int titleColor = !unlocked ? TEXT_LOCKED : (completed ? TEXT_DONE : TEXT_BODY);
			graphics.text(font, trim(quest.getTitle(), LIST_WIDTH - 44), (int) (listLeft + 22), (int) (rowTop + 3), titleColor);

			if (quest.getId().equals(HudConfig.pinnedQuestId)) {
				drawPin(graphics, listRight - 32, rowTop + 5, TEXT_GUIDE);
			}

			// Right-hand badges sit clear of the scrollbar track.
			if (completed) {
				drawTick(graphics, listRight - 20, rowTop + 6, TEXT_DONE);
				graphics.text(font, "Complete", (int) (listLeft + 22), (int) (rowTop + 12), TEXT_DONE);
			} else if (unlocked) {
				drawProgressBar(graphics, listLeft + 22, rowTop + 13, LIST_WIDTH - 38,
						questProgress(data, quest), questTotal(quest));
			} else {
				drawLock(graphics, listRight - 20, rowTop + 5, TEXT_LOCKED);
				graphics.text(font, "Locked - preview", (int) (listLeft + 22), (int) (rowTop + 12), TEXT_LOCKED);
			}
		}

		disableScissor();
		drawScrollbar(graphics, SB_LIST, listRight, top + 1, contentBottom - 1, totalHeight,
				mouseX, mouseY);
		drawBorder(graphics, listLeft, top, listRight, contentBottom);
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

	/** Set at the start of every tree render / click so the layout is consistent. */
	private int treeNodeCount = 1;

	/**
	 * A roughly square grid, so ~20 quests fill the panel instead of sitting in
	 * one thin row. Columns are capped by what fits horizontally; anything taller
	 * than the panel scrolls.
	 */
	private int treeCols() {
		int maxByWidth = Math.max(1, (panelWidth - 24) / NODE_STEP);
		int square = Math.max(1, (int) Math.round(Math.sqrt(treeNodeCount)));
		return Math.min(square, maxByWidth);
	}

	private int treeRows() {
		int cols = treeCols();
		return Math.max(1, (treeNodeCount + cols - 1) / cols);
	}

	/** Left edge of the node grid, centred so the block sits mid-panel. */
	private int treeGridLeft() {
		int cols = treeCols();
		int blockWidth = (cols - 1) * NODE_STEP + NODE;
		return left + Math.max(PADDING + 8, (panelWidth - blockWidth) / 2);
	}

	private int treeGridTop() {
		return contentTop + TREE_HEADER + 4;
	}

	private int treeGridBottom() {
		return contentBottom - TREE_FOOTER;
	}

	/** Vertical slack shared above and below when the grid is shorter than the area. */
	private int treeGridVOffset() {
		int available = treeGridBottom() - treeGridTop();
		int used = treeRows() * NODE_STEP;
		return Math.max(0, (available - used) / 2);
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
				treeGridLeft() + col * NODE_STEP,
				treeGridTop() + treeGridVOffset() + row * NODE_STEP - (int) treeScroll
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

	private void renderTree(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean side) {
		int panelLeft = left + PADDING;
		int panelRight = left + panelWidth - PADDING;
		graphics.fill(panelLeft, contentTop, panelRight, contentBottom, COLOR_SUBPANEL);

		PlayerQuestData data = ClientQuestState.get();
		List<Quest> quests = treeQuests(side);
		treeNodeCount = Math.max(1, quests.size());
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

		graphics.fill(panelLeft, contentTop, panelRight, contentTop + TREE_HEADER, COLOR_HEADER_ROW);

		// Right-to-left layout so nothing can ever overlap: the List/Tree button
		// owns the far right, then the next arrow, then the counter, and the
		// title takes whatever is left.
		drawViewToggle(graphics, panelRight - 38, contentTop + 2, panelRight - 4, contentTop + 16,
				mouseX, mouseY);

		int rightLimit = panelRight - 38 - 6;
		if (!side) {
			int[] leftArrow = treeArrowLeft();
			int[] rightArrow = treeArrowRight();
			drawArrow(graphics, leftArrow, true, mouseX, mouseY, phaseIndex() > 0);
			drawArrow(graphics, rightArrow, false, mouseX, mouseY,
					phaseIndex() < PHASE_PAGES.length - 1);
			rightLimit = rightArrow[0] - 6;
		}

		String counts = quests.size() + " quests - " + done + " done";
		int countsX = rightLimit - font.width(counts);
		graphics.text(font, counts, (int) (countsX), (int) (contentTop + 5), TEXT_DIM);

		int titleX = panelLeft + (side ? 8 : 26);
		graphics.text(font, trim(title, Math.max(20, countsX - titleX - 8)), (int) (titleX), (int) (contentTop + 5), titleColor);

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
				graphics.fill(Math.min(ax, bx), ay - 1, Math.max(ax, bx), ay + 1, colour);
			} else {
				// Row wrap: drop straight down in the shared column.
				graphics.fill(ax - 1, Math.min(ay, by), ax + 1, Math.max(ay, by), colour);
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

			drawNode(graphics, pos[0], pos[1], quest, data, current, over, side);
		}

		disableScissor();
		drawScrollbar(graphics, SB_TREE, panelRight, treeGridTop(), treeGridBottom(), contentHeight,
				mouseX, mouseY);

		// ---- Footer ----
		graphics.fill(panelLeft, contentBottom - TREE_FOOTER, panelRight, contentBottom - TREE_FOOTER + 1,
				COLOR_BORDER);

		String selectedId = side ? selectedSideQuest : selectedMainQuest;
		Quest focus = hovered != null ? hovered
				: (selectedId == null ? null : QuestManager.get().getQuest(selectedId));

		if (focus != null) {
			boolean unlocked = QuestManager.get().isUnlocked(data, focus);
			boolean complete = data.isCompleted(focus.getId());

			int textY = contentBottom - TREE_FOOTER + 6;
			int colour = complete ? TEXT_DONE : (unlocked ? TEXT_TITLE : TEXT_LOCKED);
			graphics.text(font, trim(focus.getTitle(), panelRight - panelLeft - 70), (int) (panelLeft + 8), (int) (textY), colour);

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
			graphics.text(font, trim(line.toString(), panelRight - panelLeft - 70), (int) (panelLeft + 8), (int) (textY + 11), unlocked ? TEXT_BODY : TEXT_LOCKED);

			if (!unlocked) {
				graphics.text(font, "Locked - preview only", (int) (panelLeft + 8), (int) (textY + 22), TEXT_WARN);
			}

			int[] open = treeOpenButton();
			boolean openHover = mouseX >= open[0] && mouseX <= open[2]
					&& mouseY >= open[1] && mouseY <= open[3];
			graphics.fill(open[0], open[1], open[2], open[3],
					openHover ? COLOR_ROW_SELECTED : COLOR_TAB_ACTIVE);
			drawBorder(graphics, open[0], open[1], open[2], open[3]);
			graphics.text(font, "Open", (int) (open[0] + (44 - font.width("Open")) / 2), (int) (open[1] + 3), TEXT_TITLE);
		} else {
			graphics.text(font, "Hover or click a quest", (int) (panelLeft + 8), (int) (contentBottom - TREE_FOOTER + 6), TEXT_DIM);
		}

		drawBorder(graphics, panelLeft, contentTop, panelRight, contentBottom);
	}

	private void drawNode(GuiGraphicsExtractor graphics, int x, int y, Quest quest, PlayerQuestData data,
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

		graphics.fill(x, y, x + NODE, y + NODE, background);
		graphics.fill(x, y, x + NODE, y + 1, border);
		graphics.fill(x, y + NODE - 1, x + NODE, y + NODE, border);
		graphics.fill(x, y, x + 1, y + NODE, border);
		graphics.fill(x + NODE - 1, y, x + NODE, y + NODE, border);

		// 16px item icon centred in the 30px node.
		Minecraft.getInstance().getItemRenderer()
				.renderInGuiWithOverrides(quest.getIconStack(), x + 7, y + 7);

		if (!unlocked) {
			graphics.fill(x + 1, y + 1, x + NODE - 1, y + NODE - 1, 0xAA12121A);
			drawLock(graphics, x + 11, y + 10, TEXT_LOCKED);
		} else if (complete) {
			drawTick(graphics, x + NODE - 10, y + NODE - 10, TEXT_DONE);
		}

		if (quest.getId().equals(HudConfig.pinnedQuestId)) {
			drawPin(graphics, x + 1, y + 1, TEXT_GUIDE);
		}
	}

	private void drawArrow(GuiGraphicsExtractor graphics, int[] rect, boolean pointLeft, int mouseX, int mouseY,
			boolean enabled) {
		boolean hovered = enabled && mouseX >= rect[0] && mouseX <= rect[2]
				&& mouseY >= rect[1] && mouseY <= rect[3];

		graphics.fill(rect[0], rect[1], rect[2], rect[3],
				!enabled ? COLOR_TAB_IDLE : (hovered ? COLOR_ROW_SELECTED : COLOR_TAB_ACTIVE));
		drawBorder(graphics, rect[0], rect[1], rect[2], rect[3]);

		int colour = enabled ? TEXT_TITLE : TEXT_LOCKED;
		int cx = (rect[0] + rect[2]) / 2;
		int cy = (rect[1] + rect[3]) / 2;
		for (int i = 0; i < 4; i++) {
			int dx = pointLeft ? i : -i;
			graphics.fill(cx + dx - 1, cy - i, cx + dx, cy + i + 1, colour);
		}
	}

	// ------------------------------------------------------------------
	// Quest detail
	// ------------------------------------------------------------------

	private void renderQuestDetail(GuiGraphicsExtractor graphics, int mouseX, int mouseY, String selectedId) {
		graphics.fill(detailLeft, contentTop, detailRight, contentBottom, COLOR_SUBPANEL);

		Quest quest = selectedId == null ? null : QuestManager.get().getQuest(selectedId);
		PlayerQuestData data = ClientQuestState.get();

		if (quest == null) {
			graphics.text(font, "Select a quest", (int) (detailLeft + 6), (int) (contentTop + 6), TEXT_DIM);
			drawBorder(graphics, detailLeft, contentTop, detailRight, contentBottom);
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

		y = drawLine(graphics, quest.getTitle(), innerLeft, y, unlocked ? TEXT_TITLE : TEXT_LOCKED);
		y = drawLine(graphics, quest.getPhase().getDisplayName(), innerLeft, y,
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
		graphics.fill(pinButtonX1, pinButtonY1, pinButtonX2, pinButtonY2,
				pinHover ? COLOR_ROW_SELECTED : COLOR_TAB_ACTIVE);
		drawBorder(graphics, pinButtonX1, pinButtonY1, pinButtonX2, pinButtonY2);
		String pinLabel = pinned ? "Unpin" : "Pin";
		graphics.text(font, pinLabel, (int) (pinButtonX1 + (pinButtonX2 - pinButtonX1 - font.width(pinLabel)) / 2), (int) (pinButtonY1 + 3), pinned ? TEXT_GUIDE : TEXT_DIM);

		if (completed) {
			drawTick(graphics, innerLeft, y + 1, TEXT_DONE);
			y = drawLine(graphics, "   COMPLETED", innerLeft, y, TEXT_DONE);
			y += 2;
		} else if (!unlocked) {
			Quest parent = quest.getParentId() == null ? null
					: QuestManager.get().getQuest(quest.getParentId());
			String parentName = parent == null ? "the previous quest" : parent.getTitle();
			y = drawWrapped(graphics, "LOCKED - finish \"" + parentName + "\" first. "
					+ "Nothing here counts until then.", innerLeft, y, wrapWidth, TEXT_WARN);
			y += 4;
		}

		y = drawWrapped(graphics, quest.getDescription(), innerLeft, y, wrapWidth, TEXT_BODY);
		y += 5;

		if (!quest.getRequiredTools().isEmpty()) {
			y = drawLine(graphics, "REQUIRED:", innerLeft, y, TEXT_TOOLS);
			y = drawWrapped(graphics, String.join(", ", quest.getRequiredTools()),
					innerLeft, y, wrapWidth, TEXT_TOOLS);
			y += 5;
		}

		if (!quest.getGuideText().isEmpty()) {
			y = drawLine(graphics, "GUIDE:", innerLeft, y, TEXT_GUIDE);
			y = drawWrapped(graphics, quest.getGuideText(), innerLeft, y, wrapWidth, TEXT_GUIDE);
			y += 6;
		}

		y = drawLine(graphics, "OBJECTIVES:", innerLeft, y, unlocked ? TEXT_TITLE : TEXT_LOCKED);
		for (QuestTask task : quest.getTasks()) {
			int progress = Math.min(task.getRequired(), data.getProgress(quest.getId(), task.getId()));
			boolean done = progress >= task.getRequired();

			String label = task.getTypeLabel() + ": " + task.getDescription();
			if (task.getRequired() > 1) {
				label = label + "  " + progress + "/" + task.getRequired();
			}

			int color = !unlocked ? TEXT_LOCKED : (done ? TEXT_DONE : TEXT_BODY);
			if (done) {
				drawTick(graphics, innerLeft, y + 1, TEXT_DONE);
			} else {
				drawEmptyBox(graphics, innerLeft, y + 1, color);
			}

			// Manual objectives can be toggled straight from this checkbox.
			if (task.isManual() && unlocked && !completed
					&& y >= contentTop && y < bodyBottom - 8) {
				manualHits.add(new ManualHit(task.getId(), done, y - 1, y + 9));
			}

			y = drawWrapped(graphics, label, innerLeft + 12, y, wrapWidth - 12, color);

			if (task.getRequired() > 1 && unlocked) {
				drawProgressBar(graphics, innerLeft + 12, y + 1, wrapWidth - 16, progress,
						task.getRequired());
			}
			y += 8;
		}

		disableScissor();

		int documentHeight = y - startY + 6;
		detailScroll = clampScroll(detailScroll, documentHeight, bodyBottom - contentTop);
		drawScrollbar(graphics, SB_DETAIL, detailRight, contentTop + 1, bodyBottom - 1,
				documentHeight, mouseX, mouseY);

		if (showFooter) {
			checkmarkTaskId = pendingManualTaskId;
			checkmarkButtonX1 = detailLeft + 6;
			checkmarkButtonY1 = contentBottom - FOOTER_HEIGHT + 3;
			checkmarkButtonX2 = detailRight - 6;
			checkmarkButtonY2 = contentBottom - 4;

			boolean hovered = mouseX >= checkmarkButtonX1 && mouseX <= checkmarkButtonX2
					&& mouseY >= checkmarkButtonY1 && mouseY <= checkmarkButtonY2;

			graphics.fill(checkmarkButtonX1, checkmarkButtonY1, checkmarkButtonX2, checkmarkButtonY2,
					hovered ? COLOR_ROW_SELECTED : COLOR_TAB_ACTIVE);
			drawBorder(graphics, checkmarkButtonX1, checkmarkButtonY1, checkmarkButtonX2, checkmarkButtonY2);

			String label = "Mark objective complete";
			int textX = checkmarkButtonX1
					+ (checkmarkButtonX2 - checkmarkButtonX1 - font.width(label)) / 2;
			graphics.text(font, label, (int) (textX), (int) (checkmarkButtonY1 + 5), TEXT_TITLE);
		}

		drawBorder(graphics, detailLeft, contentTop, detailRight, contentBottom);
	}

	// ------------------------------------------------------------------
	// Deaths
	// ------------------------------------------------------------------

	private void renderDeaths(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int panelLeft = left + PADDING;
		int panelRight = left + panelWidth - PADDING;
		graphics.fill(panelLeft, contentTop, panelRight, contentBottom, COLOR_SUBPANEL);

		List<DeathRecord> deaths = ClientQuestState.get().getDeaths();

		if (viewingDeath >= 0 && viewingDeath < deaths.size()) {
			renderDeathInventory(graphics, deaths.get(viewingDeath), panelLeft, panelRight);
			drawBorder(graphics, panelLeft, contentTop, panelRight, contentBottom);
			return;
		}

		if (deaths.isEmpty()) {
			graphics.text(font, "No deaths recorded. Yet.", (int) (panelLeft + 6), (int) (contentTop + 6), TEXT_DIM);
			drawBorder(graphics, panelLeft, contentTop, panelRight, contentBottom);
			return;
		}

		int rowHeight = 21;
		int headerHeight = 14;
		int totalHeight = headerHeight + deaths.size() * rowHeight + 6;
		deathScroll = clampScroll(deathScroll, totalHeight, contentBottom - contentTop);

		enableScissor(panelLeft, contentTop + 1, panelRight, contentBottom - 1);

		int y = contentTop + 5 - (int) deathScroll;
		graphics.text(font, "Recorded deaths: " + deaths.size()
				+ " (newest first - click one to see your inventory)", (int) (panelLeft + 6), (int) (y), TEXT_DIM);
		y += headerHeight;

		for (int i = deaths.size() - 1; i >= 0; i--) {
			DeathRecord record = deaths.get(i);
			boolean hovered = mouseX >= panelLeft && mouseX < panelRight
					&& mouseY >= Math.max(y - 2, contentTop) && mouseY < Math.min(y + 18, contentBottom);

			if (hovered && record.hasInventory()) {
				graphics.fill(panelLeft + 1, y - 2, panelRight - 1, y + 18, COLOR_ROW_HOVER);
			}

			String line = "Death #" + (i + 1) + ": "
					+ record.getX() + ", " + record.getY() + ", " + record.getZ()
					+ " [" + record.getDimensionDisplayName() + "]";
			graphics.text(font, trim(line, panelRight - panelLeft - 22), (int) (panelLeft + 6), (int) (y), TEXT_DEATH);

			String cause = record.getCause().isEmpty() ? "Unknown cause" : record.getCause();
			if (!record.hasInventory()) {
				cause = cause + "  (inventory not kept)";
			}
			graphics.text(font, trim(cause, panelRight - panelLeft - 22), (int) (panelLeft + 6), (int) (y + 10), TEXT_DIM);

			y += rowHeight;
		}

		disableScissor();
		drawScrollbar(graphics, SB_DEATHS, panelRight, contentTop + 1, contentBottom - 1, totalHeight,
				mouseX, mouseY);
		drawBorder(graphics, panelLeft, contentTop, panelRight, contentBottom);
	}

	private void renderDeathInventory(GuiGraphicsExtractor graphics, DeathRecord record, int panelLeft,
			int panelRight) {
		String header = "Death #" + (viewingDeath + 1) + " - "
				+ record.getX() + ", " + record.getY() + ", " + record.getZ()
				+ " [" + record.getDimensionDisplayName() + "]";
		graphics.text(font, trim(header, panelRight - panelLeft - 60), (int) (panelLeft + 6), (int) (contentTop + 5), TEXT_DEATH);
		graphics.text(font, trim(record.getCause(), panelRight - panelLeft - 60), (int) (panelLeft + 6), (int) (contentTop + 15), TEXT_DIM);

		// Back button
		int bx2 = panelRight - 6;
		int bx1 = bx2 - 44;
		graphics.fill(bx1, contentTop + 4, bx2, contentTop + 17, COLOR_TAB_ACTIVE);
		drawBorder(graphics, bx1, contentTop + 4, bx2, contentTop + 17);
		graphics.text(font, "Back", (int) (bx1 + (44 - font.width("Back")) / 2), (int) (contentTop + 7), TEXT_TITLE);

		ItemStack[] slots = readInventory(record);

		int gridLeft = panelLeft + 10;
		int gridTop = contentTop + 32;

		// Main inventory rows (slots 9-35) then the hotbar (slots 0-8).
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				drawSlot(graphics, gridLeft + col * 18, gridTop + row * 18, slots[9 + row * 9 + col]);
			}
		}
		for (int col = 0; col < 9; col++) {
			drawSlot(graphics, gridLeft + col * 18, gridTop + 3 * 18 + 4, slots[col]);
		}

		// Armour and offhand off to the right.
		// Armour stacks vertically with the off-hand underneath it, so the two
		// labels can never run into each other however wide the font is.
		int sideLeft = gridLeft + 9 * 18 + 12;
		graphics.text(font, "Armour", (int) (sideLeft), (int) (gridTop - 10), TEXT_DIM);
		// Vanilla stores armour boots-first (slot 100 = boots, 103 = helmet), so
		// walk it backwards to match how the inventory screen stacks it.
		for (int i = 0; i < 4; i++) {
			drawSlot(graphics, sideLeft, gridTop + i * 18, slots[39 - i]);
		}

		int offhandTop = gridTop + 4 * 18 + 12;
		graphics.text(font, "Off-hand", (int) (sideLeft), (int) (offhandTop - 10), TEXT_DIM);
		drawSlot(graphics, sideLeft, offhandTop, slots[40]);
	}

	/**
	 * Rebuilds the 41-slot layout from the stored vanilla inventory list.
	 * 26.2 numbers the slots flat: 0-35 main, 36-39 armour, 40 offhand. The old
	 * 100+i armour and 150 offhand encoding is gone, so the stored slot index
	 * now maps straight onto this array.
	 */
	private ItemStack[] readInventory(DeathRecord record) {
		ItemStack[] slots = new ItemStack[41];
		for (int i = 0; i < slots.length; i++) {
			slots[i] = ItemStack.EMPTY;
		}

		ListTag list = record.getInventory();
		if (list == null) {
			return slots;
		}

		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			// No registry access without a level, and components cannot be
			// decoded without it.
			return slots;
		}

		RegistryOps<Tag> ops = level.registryAccess()
				.createSerializationContext(NbtOps.INSTANCE);

		for (int i = 0; i < list.size(); i++) {
			Tag entry = list.get(i);
			ItemStackWithSlot decoded = ItemStackWithSlot.CODEC
					.parse(ops, entry)
					.result()
					.orElse(null);
			if (decoded == null || decoded.stack().isEmpty()) {
				continue;
			}

			int slot = decoded.slot();
			if (slot >= 0 && slot < slots.length) {
				slots[slot] = decoded.stack();
			}
		}
		return slots;
	}

	private void drawSlot(GuiGraphicsExtractor graphics, int x, int y, ItemStack stack) {
		graphics.fill(x, y, x + 16, y + 16, COLOR_SLOT);
		if (stack == null || stack.isEmpty()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		client.getItemRenderer().renderInGuiWithOverrides(stack, x, y);
		client.getItemRenderer().renderGuiItemOverlay(font, stack, x, y);
	}

	// ------------------------------------------------------------------
	// Stats
	// ------------------------------------------------------------------

	/** Y of each Stats dropdown toggle row, recorded for click hit-testing. */
	private int statsToggleY = -1;
	private int statsCombatToggleY = -1;

	private void renderStats(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int panelLeft = left + PADDING;
		int panelRight = left + panelWidth - PADDING;
		graphics.fill(panelLeft, contentTop, panelRight, contentBottom, COLOR_SUBPANEL);

		CompoundTag stats = ClientQuestState.getStats();
		PlayerQuestData data = ClientQuestState.get();

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

		enableScissor(panelLeft, contentTop + 1, panelRight, contentBottom - 1);

		int x = panelLeft + 8;
		int startY = contentTop + 7 - (int) statsScroll;
		int y = startY;

		graphics.text(font, "This World", (int) (x), (int) (y), TEXT_TITLE);
		y += 14;

		y = stat(graphics, x, y, "Playtime", formatDuration(stats.getIntOr("PlayTime", 0)));
		y = stat(graphics, x, y, "Quests completed", mainDone + " main, " + sideDone + " side");
		y = stat(graphics, x, y, "Since last death", formatDuration(stats.getIntOr("SinceDeath", 0)));
		y += 6;

		y = stat(graphics, x, y, "Deaths", Integer.toString(stats.getIntOr("Deaths", 0)));
		y = stat(graphics, x, y, "Mobs killed", Integer.toString(stats.getIntOr("MobKills", 0)));
		y = stat(graphics, x, y, "Damage taken",
				String.format(Locale.ROOT, "%.1f hearts", stats.getIntOr("DamageTaken", 0) / 20.0D));
		y += 6;

		y = stat(graphics, x, y, "Distance walked", formatDistance(stats.getIntOr("WalkCm", 0)));
		y = stat(graphics, x, y, "Distance sprinted", formatDistance(stats.getIntOr("SprintCm", 0)));
		y = stat(graphics, x, y, "Distance flown", formatDistance(stats.getIntOr("FlyCm", 0)));
		y = stat(graphics, x, y, "Jumps", Integer.toString(stats.getIntOr("Jumps", 0)));
		y = stat(graphics, x, y, "Nights slept", Integer.toString(stats.getIntOr("Slept", 0)));
		y += 8;

		// ---- Time per quest dropdown ----
		statsToggleY = y;
		graphics.fill(panelLeft + 4, y - 2, panelRight - 4, y + 11, COLOR_HEADER_ROW);
		drawFoldArrow(graphics, panelLeft + 8, y + 1, !statsTimesOpen, TEXT_TITLE);
		graphics.text(font, "Time per quest", (int) (panelLeft + 18), (int) (y), TEXT_TITLE);
		String hint = statsTimesOpen ? "click to hide" : "click to show";
		graphics.text(font, hint, (int) (panelRight - 8 - font.width(hint)), (int) (y), TEXT_DIM);
		y += 15;

		if (statsTimesOpen) {
			int prevTicks = 0;
			boolean any = false;
			for (Quest quest : QuestManager.get().getMainQuests()) {
				if (!data.isCompleted(quest.getId())) {
					continue;
				}
				any = true;
				int t = data.getCompletionPlayTicks(quest.getId());
				String duration;
				if (t < 0) {
					duration = "-";
				} else {
					duration = formatDuration(Math.max(0, t - prevTicks));
					prevTicks = t;
				}
				graphics.text(font, trim(quest.getTitle(), 160), (int) (x + 4), (int) (y), TEXT_BODY);
				graphics.text(font, duration, (int) (panelRight - 8 - font.width(duration)), (int) (y), TEXT_GUIDE);
				y += 11;
			}
			if (!any) {
				graphics.text(font, "No quests completed yet.", (int) (x + 4), (int) (y), TEXT_DIM);
				y += 11;
			}
		}

		y += 4;

		// ---- Combat log dropdown ----
		statsCombatToggleY = y;
		graphics.fill(panelLeft + 4, y - 2, panelRight - 4, y + 11, COLOR_HEADER_ROW);
		drawFoldArrow(graphics, panelLeft + 8, y + 1, !statsCombatOpen, TEXT_TITLE);
		graphics.text(font, "Damage dealt per mob", (int) (panelLeft + 18), (int) (y), TEXT_TITLE);
		String chint = statsCombatOpen ? "click to hide" : "click to show";
		graphics.text(font, chint, (int) (panelRight - 8 - font.width(chint)), (int) (y), TEXT_DIM);
		y += 15;

		if (statsCombatOpen) {
			java.util.List<Map.Entry<String, Float>> rows =
					new ArrayList<>(data.getDamageDealt().entrySet());
			rows.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));

			if (rows.isEmpty()) {
				graphics.text(font, "No damage dealt yet.", (int) (x + 4), (int) (y), TEXT_DIM);
				y += 11;
			} else {
				for (Map.Entry<String, Float> entry : rows) {
					String name = prettyEntityName(entry.getKey());
					String value = Math.round(entry.getValue()) + " dmg";
					graphics.text(font, trim(name, 160), (int) (x + 4), (int) (y), TEXT_BODY);
					graphics.text(font, value, (int) (panelRight - 8 - font.width(value)), (int) (y), TEXT_DEATH);
					y += 11;
				}
			}
		}

		disableScissor();

		int contentHeight = (y - startY) + 8;
		statsScroll = clampScroll(statsScroll, contentHeight, contentBottom - contentTop);
		drawScrollbar(graphics, SB_STATS, panelRight, contentTop + 1, contentBottom - 1,
				contentHeight, mouseX, mouseY);
		drawBorder(graphics, panelLeft, contentTop, panelRight, contentBottom);
	}

	private int stat(GuiGraphicsExtractor graphics, int x, int y, String label, String value) {
		graphics.text(font, label, (int) (x), (int) (y), TEXT_DIM);
		graphics.text(font, value, (int) (x + 150), (int) (y), TEXT_BODY);
		return y + 11;
	}

	/** "minecraft:cave_spider" -> "Cave Spider". */
	private static String prettyEntityName(String id) {
		String name = id;
		int colon = name.indexOf(':');
		if (colon >= 0) {
			name = name.substring(colon + 1);
		}
		StringBuilder out = new StringBuilder();
		for (String part : name.split("_")) {
			if (part.isEmpty()) {
				continue;
			}
			if (out.length() > 0) {
				out.append(' ');
			}
			out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
		}
		return out.toString();
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

	private void renderSummary(GuiGraphicsExtractor graphics) {
		int panelLeft = left + PADDING;
		int panelRight = left + panelWidth - PADDING;
		graphics.fill(panelLeft, contentTop, panelRight, contentBottom, COLOR_SUBPANEL);

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

		graphics.text(font, "ForeverSurvival - Progress Summary", (int) (x), (int) (y), TEXT_TITLE);
		y += 15;
		graphics.text(font, "Main line: " + mainDone + " / " + mainTotal + "  (" + percent + "%)", (int) (x), (int) (y), TEXT_BODY);
		y += 13;
		drawProgressBar(graphics, x, y, panelRight - x - 8, mainDone, Math.max(1, mainTotal));
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
			graphics.text(font, line, (int) (x), (int) (y), done == total ? TEXT_DONE : colorOf(phase.getColor()));
			if (done == total) {
				drawTick(graphics, x + font.width(line) + 4, y + 1, TEXT_DONE);
			}
			y += 11;
		}

		y += 5;
		graphics.text(font, "Side Challenges: " + sideDone + " / " + sideTotal, (int) (x), (int) (y), TEXT_BODY);
		y += 11;
		graphics.text(font, "Deaths recorded: " + data.getDeaths().size(), (int) (x), (int) (y), TEXT_DEATH);
		y += 14;

		Quest current = ClientQuestState.getCurrentMainQuest();
		graphics.text(font, "Current objective:", (int) (x), (int) (y), TEXT_DIM);
		y += 11;
		graphics.text(font, current == null ? "All main quests complete." : current.getTitle(), (int) (x), (int) (y), TEXT_TITLE);

		disableScissor();
		drawBorder(graphics, panelLeft, contentTop, panelRight, contentBottom);
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
		settings.add(new Slider("Component opacity", 20, 100,
				() -> HudConfig.textOpacity, v -> HudConfig.textOpacity = v, "%"));
		settings.add(new Slider("Panel width", HudConfig.MIN_HUD_WIDTH, HudConfig.MAX_HUD_WIDTH,
				() -> HudConfig.hudWidth, v -> HudConfig.hudWidth = v, "px"));
		settings.add(new Slider("Panel min height", 0, HudConfig.MAX_HUD_HEIGHT,
				() -> HudConfig.hudHeight, v -> HudConfig.hudHeight = v, "px"));
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
		settings.add(new Toggle("Durability on item tooltips", () -> HudConfig.showDurability,
				() -> HudConfig.showDurability = !HudConfig.showDurability));

		settings.add(new Header("Layout"));
		settings.add(new Action("Move and resize...",
				() -> this.minecraft.setScreenAndShow(new HudEditScreen(this))));
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

	private void renderSettings(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int panelLeft = left + PADDING;
		int panelRight = left + panelWidth - PADDING;
		graphics.fill(panelLeft, contentTop, panelRight, contentBottom, COLOR_SUBPANEL);

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
				graphics.text(font, setting.label, (int) (panelLeft + 8), (int) (rowTop + 5), TEXT_TITLE);
				graphics.fill(panelLeft + 8, rowTop + 16, panelRight - 8, rowTop + 17, COLOR_BORDER);
				continue;
			}

			graphics.text(font, trim(setting.label, controlLeft() - panelLeft - 16), (int) (panelLeft + 8), (int) (rowTop + 5), TEXT_BODY);

			int cl = controlLeft();
			int cr = controlRight();
			boolean hovered = mouseX >= cl && mouseX <= cr
					&& mouseY >= rowTop + 1 && mouseY <= rowTop + 16;

			if (setting instanceof Toggle toggle) {
				boolean on = toggle.getter.getAsBoolean();
				graphics.fill(cl, rowTop + 2, cr, rowTop + 16,
						hovered ? COLOR_ROW_SELECTED : COLOR_TAB_ACTIVE);
				drawBorder(graphics, cl, rowTop + 2, cr, rowTop + 16);
				String value = on ? "On" : "Off";
				graphics.text(font, value, (int) (cl + (cr - cl - font.width(value)) / 2), (int) (rowTop + 5), on ? TEXT_DONE : TEXT_DIM);

			} else if (setting instanceof Slider slider) {
				int value = slider.getter.getAsInt();
				double fraction = (double) (value - slider.min) / (slider.max - slider.min);

				int trackY = rowTop + 8;
				graphics.fill(cl, trackY, cr, trackY + 2, COLOR_TRACK);
				int filled = cl + (int) Math.round((cr - cl) * fraction);
				graphics.fill(cl, trackY, filled, trackY + 2, COLOR_BAR_PARTIAL);

				int knobX = Math.min(cr - 4, Math.max(cl, filled - 2));
				graphics.fill(knobX, rowTop + 3, knobX + 4, rowTop + 15, COLOR_KNOB);

				String text = value + slider.suffix;
				graphics.text(font, text, (int) (cl - 6 - font.width(text)), (int) (rowTop + 5), TEXT_DIM);

			} else if (setting instanceof Action) {
				graphics.fill(cl, rowTop + 2, cr, rowTop + 16,
						hovered ? COLOR_ROW_SELECTED : COLOR_TAB_ACTIVE);
				drawBorder(graphics, cl, rowTop + 2, cr, rowTop + 16);
				String value = "Open";
				graphics.text(font, value, (int) (cl + (cr - cl - font.width(value)) / 2), (int) (rowTop + 5), TEXT_TITLE);
			}
		}

		disableScissor();
		drawScrollbar(graphics, SB_SETTINGS, panelRight, contentTop + 1, contentBottom - 1,
				totalHeight, mouseX, mouseY);
		drawBorder(graphics, panelLeft, contentTop, panelRight, contentBottom);
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
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();
		if (button != 0) {
			return super.mouseClicked(event, doubleClick);
		}

		// Scrollbars win over everything underneath them.
		if (scrollbarClicked(mouseX, mouseY)) {
			return true;
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

		if (tab == Tab.STATS) {
			int rowL = left + PADDING + 4;
			int rowR = left + panelWidth - PADDING - 4;
			if (statsToggleY >= 0 && mouseX >= rowL && mouseX <= rowR
					&& mouseY >= statsToggleY - 2 && mouseY <= statsToggleY + 11) {
				statsTimesOpen = !statsTimesOpen;
				statsScroll = 0;
			} else if (statsCombatToggleY >= 0 && mouseX >= rowL && mouseX <= rowR
					&& mouseY >= statsCombatToggleY - 2 && mouseY <= statsCombatToggleY + 11) {
				statsCombatOpen = !statsCombatOpen;
				statsScroll = 0;
			}
			return true;
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

					if (mouseY < rowTop || mouseY >= rowBottom) {
						continue;
					}

					// Header row: fold/unfold the phase.
					if (entry.header != null) {
						if (collapsedPhases.contains(entry.header)) {
							collapsedPhases.remove(entry.header);
						} else {
							collapsedPhases.add(entry.header);
						}
						listScroll = 0;
						return true;
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

		return super.mouseClicked(event, doubleClick);
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
		treeNodeCount = Math.max(1, quests.size());
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
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();
		if (scrollDrag >= 0) {
			dragScrollbar(scrollDrag, mouseY);
			return true;
		}

		if (tab == Tab.SETTINGS && draggingSlider >= 0) {
			List<Setting> settings = buildSettings();
			if (draggingSlider < settings.size()
					&& settings.get(draggingSlider) instanceof Slider slider) {
				applySlider(slider, mouseX);
				return true;
			}
		}
		return super.mouseDragged(event, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();
		if (scrollDrag >= 0) {
			scrollDrag = -1;
			return true;
		}

		if (draggingSlider >= 0) {
			draggingSlider = -1;
			HudConfig.save();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		double step = amount * 12.0D;

		switch (tab) {
			case DEATHS -> deathScroll = Math.max(0, deathScroll - step);
			case SETTINGS -> settingsScroll = Math.max(0, settingsScroll - step);
			case STATS -> statsScroll = Math.max(0, statsScroll - step);
			case SUMMARY -> {
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
	public boolean keyPressed(KeyEvent event) {
		int keyCode = event.key();
		int scanCode = event.scancode();
		int modifiers = event.modifiers();
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
		return super.keyPressed(event);
	}

	private void sendCheckmark(String questId, String taskId, boolean set) {
		ClientPlayNetworking.send(new ModPayloads.Checkmark(questId, taskId, set));
	}

	// ------------------------------------------------------------------
	// Drawing helpers
	// ------------------------------------------------------------------

	// ------------------------------------------------------------------
	// Scrollbars
	// ------------------------------------------------------------------

	private double getScroll(int id) {
		return switch (id) {
			case SB_LIST -> listScroll;
			case SB_DETAIL -> detailScroll;
			case SB_DEATHS -> deathScroll;
			case SB_SETTINGS -> settingsScroll;
			case SB_STATS -> statsScroll;
			default -> treeScroll;
		};
	}

	private void setScroll(int id, double value) {
		switch (id) {
			case SB_LIST -> listScroll = value;
			case SB_DETAIL -> detailScroll = value;
			case SB_DEATHS -> deathScroll = value;
			case SB_SETTINGS -> settingsScroll = value;
			case SB_STATS -> statsScroll = value;
			default -> treeScroll = value;
		}
	}

	/**
	 * Draws a vertical scrollbar down the right edge of a region and records its
	 * geometry so it can be grabbed. Draws nothing when everything already fits.
	 */
	private void drawScrollbar(GuiGraphicsExtractor graphics, int id, int rightEdge, int y1, int y2,
			int contentHeight, int mouseX, int mouseY) {
		Bar bar = scrollBars[id];
		int viewHeight = y2 - y1;

		bar.x = rightEdge - SB_WIDTH - 1;
		bar.y1 = y1;
		bar.y2 = y2;
		bar.contentHeight = contentHeight;
		bar.viewHeight = viewHeight;
		bar.active = contentHeight > viewHeight && viewHeight > SB_MIN_THUMB;

		if (!bar.active) {
			return;
		}

		graphics.fill(bar.x, y1, bar.x + SB_WIDTH, y2, COLOR_TRACK);

		int thumbHeight = Math.max(SB_MIN_THUMB,
				(int) ((long) viewHeight * viewHeight / contentHeight));
		int travel = viewHeight - thumbHeight;
		int maxScroll = contentHeight - viewHeight;
		double fraction = maxScroll <= 0 ? 0 : getScroll(id) / maxScroll;
		int thumbTop = y1 + (int) Math.round(travel * Math.max(0, Math.min(1, fraction)));

		boolean hovered = mouseX >= bar.x && mouseX <= bar.x + SB_WIDTH
				&& mouseY >= y1 && mouseY <= y2;
		int colour = (scrollDrag == id || hovered) ? COLOR_KNOB : 0xFF5A5A6E;

		graphics.fill(bar.x, thumbTop, bar.x + SB_WIDTH, thumbTop + thumbHeight, colour);
	}

	/** Maps a cursor position on the track to a scroll offset. */
	private void dragScrollbar(int id, double mouseY) {
		Bar bar = scrollBars[id];
		if (!bar.active) {
			return;
		}

		int thumbHeight = Math.max(SB_MIN_THUMB,
				(int) ((long) bar.viewHeight * bar.viewHeight / bar.contentHeight));
		int travel = bar.viewHeight - thumbHeight;
		if (travel <= 0) {
			return;
		}

		double top = mouseY - scrollGrab - bar.y1;
		double fraction = Math.max(0.0D, Math.min(1.0D, top / travel));
		setScroll(id, fraction * (bar.contentHeight - bar.viewHeight));
	}

	/** @return true when the click was consumed by a scrollbar. */
	private boolean scrollbarClicked(double mouseX, double mouseY) {
		for (int id = 0; id < scrollBars.length; id++) {
			Bar bar = scrollBars[id];
			if (!bar.active) {
				continue;
			}
			if (mouseX < bar.x || mouseX > bar.x + SB_WIDTH
					|| mouseY < bar.y1 || mouseY > bar.y2) {
				continue;
			}

			int thumbHeight = Math.max(SB_MIN_THUMB,
					(int) ((long) bar.viewHeight * bar.viewHeight / bar.contentHeight));
			int travel = bar.viewHeight - thumbHeight;
			int maxScroll = bar.contentHeight - bar.viewHeight;
			double fraction = maxScroll <= 0 ? 0 : getScroll(id) / maxScroll;
			int thumbTop = bar.y1 + (int) Math.round(travel * Math.max(0, Math.min(1, fraction)));

			scrollDrag = id;
			if (mouseY >= thumbTop && mouseY <= thumbTop + thumbHeight) {
				// Grabbed the thumb: keep the same point under the cursor.
				scrollGrab = mouseY - thumbTop;
			} else {
				// Clicked the track: centre the thumb on the cursor and follow.
				scrollGrab = thumbHeight / 2.0D;
				dragScrollbar(id, mouseY);
			}
			return true;
		}
		return false;
	}

	private void clearScrollbars() {
		for (Bar bar : scrollBars) {
			bar.active = false;
		}
	}

	private void enableScissor(int x1, int y1, int x2, int y2) {
		Window window = Minecraft.getInstance().getWindow();
		double scale = window.getScaleFactor();

		int sx = (int) (x1 * scale);
		int sy = (int) ((window.getGuiScaledHeight() - y2) * scale);
		int sw = (int) ((x2 - x1) * scale);
		int sh = (int) ((y2 - y1) * scale);

		RenderSystem.enableScissor(sx, sy, Math.max(0, sw), Math.max(0, sh));
	}

	private void disableScissor() {
		RenderSystem.disableScissor();
	}

	private void drawTick(GuiGraphicsExtractor graphics, int x, int y, int color) {
		graphics.fill(x + 1, y + 4, x + 3, y + 6, color);
		graphics.fill(x + 2, y + 5, x + 4, y + 7, color);
		graphics.fill(x + 3, y + 3, x + 5, y + 5, color);
		graphics.fill(x + 4, y + 1, x + 6, y + 3, color);
		graphics.fill(x + 5, y, x + 7, y + 2, color);
	}

	private void drawEmptyBox(GuiGraphicsExtractor graphics, int x, int y, int color) {
		int size = 7;
		graphics.fill(x, y, x + size, y + 1, color);
		graphics.fill(x, y + size - 1, x + size, y + size, color);
		graphics.fill(x, y, x + 1, y + size, color);
		graphics.fill(x + size - 1, y, x + size, y + size, color);
	}

	private void drawLock(GuiGraphicsExtractor graphics, int x, int y, int color) {
		graphics.fill(x + 1, y, x + 6, y + 1, color);
		graphics.fill(x, y + 1, x + 1, y + 4, color);
		graphics.fill(x + 6, y + 1, x + 7, y + 4, color);
		graphics.fill(x - 1, y + 4, x + 8, y + 10, color);
	}

	private void drawPin(GuiGraphicsExtractor graphics, int x, int y, int color) {
		graphics.fill(x + 2, y, x + 5, y + 5, color);
		graphics.fill(x, y + 5, x + 7, y + 6, color);
		graphics.fill(x + 3, y + 6, x + 4, y + 10, color);
	}

	/** Fold indicator: a right-pointing triangle when collapsed, down when open. */
	private void drawFoldArrow(GuiGraphicsExtractor graphics, int x, int y, boolean collapsed, int color) {
		if (collapsed) {
			// Points right: a column that shrinks top and bottom as it goes right.
			for (int i = 0; i < 4; i++) {
				graphics.fill(x + i, y + i, x + i + 1, y + 7 - i, color);
			}
		} else {
			// Points down.
			for (int i = 0; i < 4; i++) {
				graphics.fill(x + i, y + i, x + 7 - i, y + i + 1, color);
			}
		}
	}

	private int phaseQuestCount(QuestPhase phase) {
		int count = 0;
		List<Quest> quests = phase.isSide()
				? QuestManager.get().getSideQuests() : QuestManager.get().getMainQuests();
		for (Quest quest : quests) {
			if (quest.getPhase() == phase) {
				count++;
			}
		}
		return count;
	}

	private int drawLine(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
		graphics.text(font, text, (int) (x), (int) (y), color);
		return y + 10;
	}

	private int drawWrapped(GuiGraphicsExtractor graphics, String text, int x, int y, int width, int color) {
		if (text == null || text.isEmpty()) {
			return y;
		}

		List<FormattedCharSequence> lines = font.split(Component.literal(text), width);
		for (FormattedCharSequence line : lines) {
			graphics.text(font, line, (int) (x), (int) (y), color);
			y += 9;
		}
		return y;
	}

	private void drawProgressBar(GuiGraphicsExtractor graphics, int x, int y, int width, int value, int max) {
		int height = 3;
		graphics.fill(x, y, x + width, y + height, COLOR_BAR_BG);

		int clamped = Math.max(0, Math.min(value, max));
		int filled = max <= 0 ? width : (int) ((long) width * clamped / max);
		if (filled > 0) {
			graphics.fill(x, y, x + filled, y + height, clamped >= max ? COLOR_BAR_DONE : COLOR_BAR_PARTIAL);
		}
	}

	private void drawBorder(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2) {
		graphics.fill(x1, y1, x2, y1 + 1, COLOR_BORDER);
		graphics.fill(x1, y2 - 1, x2, y2, COLOR_BORDER);
		graphics.fill(x1, y1, x1 + 1, y2, COLOR_BORDER);
		graphics.fill(x2 - 1, y1, x2, y2, COLOR_BORDER);
	}

	private String trim(String text, int maxWidth) {
		if (font.width(text) <= maxWidth) {
			return text;
		}
		return font.trimToWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
	}

	private double clampScroll(double scroll, int contentHeight, int viewHeight) {
		int maxScroll = Math.max(0, contentHeight - viewHeight);
		return Math.max(0, Math.min(scroll, maxScroll));
	}

	private static int colorOf(ChatFormatting formatting) {
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
