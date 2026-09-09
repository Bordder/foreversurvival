package com.foreversurvival.client;

import com.foreversurvival.quest.Quest;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * Layout editor.
 *
 * Grab the body to move. Grab the RIGHT edge for width, the BOTTOM edge for
 * height, or the corner for both - the two axes are completely independent, so
 * the box stretches as far as you like in either direction and the text simply
 * re-wraps. Nothing is scaled or distorted.
 *
 * Grab zones are deliberately generous and the handles are drawn on the frame,
 * so there is no hunting for a one-pixel corner. Arrow keys nudge the selected
 * element a pixel at a time (hold shift for ten) once you have clicked it.
 *
 * Positions are written back as screen fractions and sizes as GUI pixels, so a
 * layout arranged here survives a resolution change.
 */
public class HudEditScreen extends Screen {

	private enum Element {
		QUEST,
		LOCATOR
	}

	private enum Mode {
		NONE,
		MOVING,
		RESIZE_WIDTH,
		RESIZE_HEIGHT,
		RESIZE_BOTH
	}

	/** How far outside an edge still counts as grabbing it. */
	private static final int EDGE = 6;
	private static final int CORNER = 12;
	private static final int MIN_PANEL_HEIGHT = 40;
	/** Snap grid in GUI pixels, and how near a guide has to be to grab you. */
	private static final int GRID = 8;
	private static final int SNAP_RANGE = 6;
	private static final int MARGIN = 4;

	/** Held shift disables snapping for fine positioning. */
	private boolean snapping = true;

	private static final int COLOR_OUTLINE = 0xFF5A7BD0;
	private static final int COLOR_OUTLINE_ACTIVE = 0xFFFFD24A;
	private static final int COLOR_HANDLE = 0xFFFFD24A;
	private static final int COLOR_HANDLE_DIM = 0x99FFD24A;
	private static final int COLOR_TIP_BG = 0xE0101018;
	private static final int TEXT_HINT = 0xFFCFCFDA;
	private static final int TEXT_DIM = 0xFF7A7A8A;

	private final Screen parent;

	private Element active;
	private Element selected = Element.QUEST;
	private Mode mode = Mode.NONE;
	private double grabX;
	private double grabY;

	public HudEditScreen(Screen parent) {
		super(Component.literal("Edit HUD Layout"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int y = this.height - 26;

		addRenderableWidget(Button.builder(Component.literal("Auto height"), button -> {
					HudConfig.hudHeight = 0;
					HudConfig.save();
				}).bounds(this.width / 2 - 180, y, 84, 20).build());

		addRenderableWidget(Button.builder(Component.literal("Reset layout"), button -> {
					HudConfig.hudX = 0.72D;
					HudConfig.hudY = 0.02D;
					HudConfig.hudWidth = 150;
					HudConfig.hudHeight = 0;
					HudConfig.locatorX = 0.5D;
					HudConfig.locatorY = 0.04D;
					HudConfig.locatorWidth = 182;
					HudConfig.locatorHeight = 9;
					HudConfig.save();
				}).bounds(this.width / 2 - 92, y, 84, 20).build());

		addRenderableWidget(Button.builder(Component.literal(HudConfig.locatorXpBarMode ? "Bar: XP slot" : "Bar: free"), button -> {
					HudConfig.locatorXpBarMode = !HudConfig.locatorXpBarMode;
					HudConfig.save();
					this.minecraft.setScreenAndShow(new HudEditScreen(parent));
				}).bounds(this.width / 2 - 4, y, 96, 20).build());

		addRenderableWidget(Button.builder(Component.literal("Done"), button -> {
					HudConfig.save();
					this.minecraft.setScreenAndShow(parent);
				}).bounds(this.width / 2 + 96, y, 84, 20).build());
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private boolean locatorMovable() {
		return HudConfig.locatorEnabled && !HudConfig.locatorXpBarMode;
	}

	// ------------------------------------------------------------------
	// Geometry - everything is 1:1 with screen pixels, no scaling involved
	// ------------------------------------------------------------------

	/** Zoom, applied on top of the width/height box exactly as in-game. */
	private double scaleOf(Element element) {
		return element == Element.QUEST ? HudConfig.hudScale : HudConfig.locatorScale;
	}

	/** Unscaled box size. */
	private int[] sizeOf(Element element) {
		if (element == Element.QUEST) {
			return QuestHud.get().measure(ClientQuestState.getCurrentMainQuest());
		}
		int[] size = LocatorBar.get().measure();
		return new int[] { size[0], size[1] + 12 };
	}

	private double[] originOf(Element element) {
		if (element == Element.QUEST) {
			return new double[] { HudConfig.hudX * this.width, HudConfig.hudY * this.height };
		}

		int[] size = sizeOf(element);
		return new double[] {
				HudConfig.locatorX * this.width - (size[0] * scaleOf(element)) / 2.0D,
				HudConfig.locatorY * this.height
		};
	}

	/** Snaps a value to the grid, or to a guide line when one is close. */
	private double snap(double value, double guide) {
		if (Math.abs(value - guide) <= SNAP_RANGE) {
			return guide;
		}
		return Math.round(value / GRID) * (double) GRID;
	}

	private void setOrigin(Element element, double pixelX, double pixelY) {
		int[] size = sizeOf(element);
		double scale = scaleOf(element);
		double boxWidth = size[0] * scale;
		double boxHeight = size[1] * scale;

		if (snapping) {
			// Guides: centred on screen, and flush against each edge.
			pixelX = snap(pixelX, (this.width - boxWidth) / 2.0D);
			pixelX = Math.abs(pixelX - MARGIN) <= SNAP_RANGE ? MARGIN : pixelX;
			pixelX = Math.abs(pixelX - (this.width - boxWidth - MARGIN)) <= SNAP_RANGE
					? this.width - boxWidth - MARGIN : pixelX;

			pixelY = snap(pixelY, (this.height - boxHeight) / 2.0D);
			pixelY = Math.abs(pixelY - MARGIN) <= SNAP_RANGE ? MARGIN : pixelY;
			pixelY = Math.abs(pixelY - (this.height - boxHeight - MARGIN)) <= SNAP_RANGE
					? this.height - boxHeight - MARGIN : pixelY;
		}

		// Keep the whole box on screen - dragging it half off is never useful.
		pixelX = HudConfig.clamp(pixelX, 0.0D, Math.max(0.0D, this.width - boxWidth));
		pixelY = HudConfig.clamp(pixelY, 0.0D, Math.max(0.0D, this.height - boxHeight));

		if (element == Element.QUEST) {
			HudConfig.hudX = HudConfig.clamp(pixelX / this.width, 0.0D, 1.0D);
			HudConfig.hudY = HudConfig.clamp(pixelY / this.height, 0.0D, 1.0D);
		} else {
			double centre = pixelX + (size[0] * scale) / 2.0D;
			HudConfig.locatorX = HudConfig.clamp(centre / this.width, 0.0D, 1.0D);
			HudConfig.locatorY = HudConfig.clamp(pixelY / this.height, 0.0D, 1.0D);
		}
	}

	private void nudge(Element element, int dx, int dy) {
		double[] origin = originOf(element);
		setOrigin(element, origin[0] + dx, origin[1] + dy);
	}

	private void setWidth(Element element, double pixels) {
		// Divide by the zoom so the dragged edge tracks the cursor on screen.
		int value = (int) Math.round(pixels / scaleOf(element));
		if (element == Element.QUEST) {
			HudConfig.hudWidth = HudConfig.clamp(value, HudConfig.MIN_HUD_WIDTH, HudConfig.MAX_HUD_WIDTH);
		} else {
			HudConfig.locatorWidth = HudConfig.clamp(value, HudConfig.MIN_BAR_WIDTH,
					HudConfig.MAX_BAR_WIDTH);
		}
	}

	private void setHeight(Element element, double pixels) {
		int value = (int) Math.round(pixels / scaleOf(element));
		if (element == Element.QUEST) {
			HudConfig.hudHeight = HudConfig.clamp(value, MIN_PANEL_HEIGHT, HudConfig.MAX_HUD_HEIGHT);
		} else {
			HudConfig.locatorHeight = HudConfig.clamp(value - 12, HudConfig.MIN_BAR_HEIGHT,
					HudConfig.MAX_BAR_HEIGHT);
		}
	}

	private double[] boundsOf(Element element) {
		double[] origin = originOf(element);
		int[] size = sizeOf(element);
		double scale = scaleOf(element);
		return new double[] {
				origin[0], origin[1],
				origin[0] + size[0] * scale, origin[1] + size[1] * scale
		};
	}

	private Mode hitTest(Element element, double mouseX, double mouseY) {
		double[] b = boundsOf(element);
		if (mouseX < b[0] - EDGE || mouseX > b[2] + EDGE
				|| mouseY < b[1] - EDGE || mouseY > b[3] + EDGE) {
			return Mode.NONE;
		}

		boolean nearRight = mouseX >= b[2] - EDGE;
		boolean nearBottom = mouseY >= b[3] - EDGE;
		boolean cornerish = mouseX >= b[2] - CORNER && mouseY >= b[3] - CORNER;

		if (cornerish && nearRight && nearBottom) {
			return Mode.RESIZE_BOTH;
		}
		if (nearRight) {
			return Mode.RESIZE_WIDTH;
		}
		if (nearBottom) {
			return Mode.RESIZE_HEIGHT;
		}
		return Mode.MOVING;
	}

	// ------------------------------------------------------------------
	// Rendering
	// ------------------------------------------------------------------

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			float delta) {
		this.extractBackground(graphics, mouseX, mouseY, delta);

		Quest quest = ClientQuestState.getCurrentMainQuest();
		double[] questOrigin = originOf(Element.QUEST);
		QuestHud.get().renderAt(graphics, questOrigin[0], questOrigin[1], HudConfig.hudScale, quest);
		drawFrame(graphics, Element.QUEST, mouseX, mouseY);

		if (locatorMovable()) {
			double[] barOrigin = originOf(Element.LOCATOR);
			LocatorBar.get().renderPreviewAt(graphics, barOrigin[0], barOrigin[1],
					HudConfig.locatorScale);
			drawFrame(graphics, Element.LOCATOR, mouseX, mouseY);
		}

		String hint = "Drag the body to move  -  right edge = width  -  bottom edge = height "
				+ " -  corner = both";
		graphics.text(font, hint, (int) ((this.width - font.width(hint)) / 2.0F), (int) (8.0F), TEXT_HINT);

		String keys = "Snaps to an 8px grid, screen centre and edges  -  hold Shift to snap freely."
				+ "  Arrow keys nudge (Shift = 10px)";
		graphics.text(font, keys, (int) ((this.width - font.width(keys)) / 2.0F), (int) (20.0F), TEXT_DIM);

		// Centre guides, shown while dragging so the snap targets are visible.
		if (active != null && mode == Mode.MOVING && snapping) {
			int cx = this.width / 2;
			int cy = this.height / 2;
			graphics.fill(cx, 0, cx + 1, this.height, 0x33FFD24A);
			graphics.fill(0, cy, this.width, cy + 1, 0x33FFD24A);
		}

		if (HudConfig.locatorEnabled && HudConfig.locatorXpBarMode) {
			String note = "Locator bar is in the XP bar slot - set it to free placement to move it";
			graphics.text(font, note, (int) ((this.width - font.width(note)) / 2.0F), (int) (32.0F), TEXT_DIM);
		}

		// Live size readout, pinned near the cursor while dragging.
		if (active != null && mode != Mode.NONE) {
			String label = active == Element.QUEST
					? (HudConfig.hudWidth + " x "
							+ (HudConfig.hudHeight == 0 ? "auto" : String.valueOf(HudConfig.hudHeight)))
					: (HudConfig.locatorWidth + " x " + HudConfig.locatorHeight);

			int w = font.width(label) + 8;
			int tipX = Math.min(mouseX + 10, this.width - w - 2);
			int tipY = Math.max(2, mouseY - 16);
			graphics.fill(tipX, tipY, tipX + w, tipY + 13, COLOR_TIP_BG);
			graphics.text(font, label, (int) (tipX + 4), (int) (tipY + 3), COLOR_HANDLE);
		}

		super.extractRenderState(graphics, mouseX, mouseY, delta);
	}

	private void drawFrame(GuiGraphicsExtractor graphics, Element element, int mouseX, int mouseY) {
		double[] b = boundsOf(element);
		int x1 = (int) Math.round(b[0]);
		int y1 = (int) Math.round(b[1]);
		int x2 = (int) Math.round(b[2]);
		int y2 = (int) Math.round(b[3]);

		Mode hover = hitTest(element, mouseX, mouseY);
		boolean hot = active == element || hover != Mode.NONE;
		int colour = hot ? COLOR_OUTLINE_ACTIVE : COLOR_OUTLINE;

		graphics.fill(x1, y1, x2, y1 + 1, colour);
		graphics.fill(x1, y2 - 1, x2, y2, colour);
		graphics.fill(x1, y1, x1 + 1, y2, colour);
		graphics.fill(x2 - 1, y1, x2, y2, colour);

		// Handles light up individually so it is obvious what is grabbable.
		boolean widthHot = hover == Mode.RESIZE_WIDTH || hover == Mode.RESIZE_BOTH;
		boolean heightHot = hover == Mode.RESIZE_HEIGHT || hover == Mode.RESIZE_BOTH;

		graphics.fill(x2 - 3, y1 + CORNER, x2, y2 - CORNER,
				widthHot ? COLOR_HANDLE : COLOR_HANDLE_DIM);
		graphics.fill(x1 + CORNER, y2 - 3, x2 - CORNER, y2,
				heightHot ? COLOR_HANDLE : COLOR_HANDLE_DIM);
		graphics.fill(x2 - CORNER, y2 - CORNER, x2, y2,
				hover == Mode.RESIZE_BOTH ? COLOR_HANDLE : COLOR_HANDLE_DIM);
	}

	// ------------------------------------------------------------------
	// Input
	// ------------------------------------------------------------------

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();
		if (button == 0) {
			Element[] order = locatorMovable()
					? new Element[] { Element.LOCATOR, Element.QUEST }
					: new Element[] { Element.QUEST };

			for (Element element : order) {
				Mode hit = hitTest(element, mouseX, mouseY);
				if (hit == Mode.NONE) {
					continue;
				}

				active = element;
				selected = element;
				mode = hit;

				double[] origin = originOf(element);
				if (hit == Mode.MOVING) {
					grabX = mouseX - origin[0];
					grabY = mouseY - origin[1];
				} else {
					// Pin the origin: the bar is anchored by its centre, so
					// recomputing it mid-resize would walk away from the cursor.
					grabX = origin[0];
					grabY = origin[1];
				}
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();
		if (active == null || mode == Mode.NONE) {
			return super.mouseDragged(event, deltaX, deltaY);
		}

		// Shift is the usual "ignore snapping" modifier.
		snapping = !event.hasShiftDown();

		switch (mode) {
			case MOVING -> setOrigin(active, mouseX - grabX, mouseY - grabY);
			case RESIZE_WIDTH -> setWidth(active, mouseX - grabX);
			case RESIZE_HEIGHT -> setHeight(active, mouseY - grabY);
			case RESIZE_BOTH -> {
				setWidth(active, mouseX - grabX);
				setHeight(active, mouseY - grabY);
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();
		if (mode != Mode.NONE) {
			mode = Mode.NONE;
			active = null;
			HudConfig.save();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int keyCode = event.key();
		int scanCode = event.scancode();
		int modifiers = event.modifiers();
		Element target = selected;
		if (target == Element.LOCATOR && !locatorMovable()) {
			target = Element.QUEST;
		}

		int step = event.hasShiftDown() ? 10 : 1;
		boolean moved = true;

		// 263 left, 262 right, 265 up, 264 down
		switch (keyCode) {
			case 263 -> nudge(target, -step, 0);
			case 262 -> nudge(target, step, 0);
			case 265 -> nudge(target, 0, -step);
			case 264 -> nudge(target, 0, step);
			default -> moved = false;
		}

		if (moved) {
			HudConfig.save();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void close() {
		HudConfig.save();
		this.minecraft.setScreenAndShow(parent);
	}
}
