package com.foreversurvival.client;

import com.foreversurvival.quest.Quest;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

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
		super(new Component("Edit HUD Layout"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int y = this.height - 26;

		addDrawableChild(new Button(this.width / 2 - 180, y, 84, 20,
				new Component("Auto height"), button -> {
					HudConfig.hudHeight = 0;
					HudConfig.save();
				}));

		addDrawableChild(new Button(this.width / 2 - 92, y, 84, 20,
				new Component("Reset layout"), button -> {
					HudConfig.hudX = 0.72D;
					HudConfig.hudY = 0.02D;
					HudConfig.hudWidth = 150;
					HudConfig.hudHeight = 0;
					HudConfig.locatorX = 0.5D;
					HudConfig.locatorY = 0.04D;
					HudConfig.locatorWidth = 182;
					HudConfig.locatorHeight = 9;
					HudConfig.save();
				}));

		addDrawableChild(new Button(this.width / 2 - 4, y, 96, 20,
				new Component(HudConfig.locatorXpBarMode ? "Bar: XP slot" : "Bar: free"),
				button -> {
					HudConfig.locatorXpBarMode = !HudConfig.locatorXpBarMode;
					HudConfig.save();
					this.client.setScreen(new HudEditScreen(parent));
				}));

		addDrawableChild(new Button(this.width / 2 + 96, y, 84, 20,
				new Component("Done"), button -> {
					HudConfig.save();
					this.client.setScreen(parent);
				}));
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
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		this.renderBackground(matrices);

		Quest quest = ClientQuestState.getCurrentMainQuest();
		double[] questOrigin = originOf(Element.QUEST);
		QuestHud.get().renderAt(matrices, questOrigin[0], questOrigin[1], HudConfig.hudScale, quest);
		drawFrame(matrices, Element.QUEST, mouseX, mouseY);

		if (locatorMovable()) {
			double[] barOrigin = originOf(Element.LOCATOR);
			LocatorBar.get().renderPreviewAt(matrices, barOrigin[0], barOrigin[1],
					HudConfig.locatorScale);
			drawFrame(matrices, Element.LOCATOR, mouseX, mouseY);
		}

		String hint = "Drag the body to move  -  right edge = width  -  bottom edge = height "
				+ " -  corner = both";
		textRenderer.draw(matrices, hint, (this.width - textRenderer.getWidth(hint)) / 2.0F, 8.0F,
				TEXT_HINT);

		String keys = "Snaps to an 8px grid, screen centre and edges  -  hold Shift to snap freely."
				+ "  Arrow keys nudge (Shift = 10px)";
		textRenderer.draw(matrices, keys, (this.width - textRenderer.getWidth(keys)) / 2.0F, 20.0F,
				TEXT_DIM);

		// Centre guides, shown while dragging so the snap targets are visible.
		if (active != null && mode == Mode.MOVING && snapping) {
			int cx = this.width / 2;
			int cy = this.height / 2;
			fill(matrices, cx, 0, cx + 1, this.height, 0x33FFD24A);
			fill(matrices, 0, cy, this.width, cy + 1, 0x33FFD24A);
		}

		if (HudConfig.locatorEnabled && HudConfig.locatorXpBarMode) {
			String note = "Locator bar is in the XP bar slot - set it to free placement to move it";
			textRenderer.draw(matrices, note, (this.width - textRenderer.getWidth(note)) / 2.0F, 32.0F,
					TEXT_DIM);
		}

		// Live size readout, pinned near the cursor while dragging.
		if (active != null && mode != Mode.NONE) {
			String label = active == Element.QUEST
					? (HudConfig.hudWidth + " x "
							+ (HudConfig.hudHeight == 0 ? "auto" : String.valueOf(HudConfig.hudHeight)))
					: (HudConfig.locatorWidth + " x " + HudConfig.locatorHeight);

			int w = textRenderer.getWidth(label) + 8;
			int tipX = Math.min(mouseX + 10, this.width - w - 2);
			int tipY = Math.max(2, mouseY - 16);
			fill(matrices, tipX, tipY, tipX + w, tipY + 13, COLOR_TIP_BG);
			textRenderer.draw(matrices, label, tipX + 4, tipY + 3, COLOR_HANDLE);
		}

		super.render(matrices, mouseX, mouseY, delta);
	}

	private void drawFrame(MatrixStack matrices, Element element, int mouseX, int mouseY) {
		double[] b = boundsOf(element);
		int x1 = (int) Math.round(b[0]);
		int y1 = (int) Math.round(b[1]);
		int x2 = (int) Math.round(b[2]);
		int y2 = (int) Math.round(b[3]);

		Mode hover = hitTest(element, mouseX, mouseY);
		boolean hot = active == element || hover != Mode.NONE;
		int colour = hot ? COLOR_OUTLINE_ACTIVE : COLOR_OUTLINE;

		fill(matrices, x1, y1, x2, y1 + 1, colour);
		fill(matrices, x1, y2 - 1, x2, y2, colour);
		fill(matrices, x1, y1, x1 + 1, y2, colour);
		fill(matrices, x2 - 1, y1, x2, y2, colour);

		// Handles light up individually so it is obvious what is grabbable.
		boolean widthHot = hover == Mode.RESIZE_WIDTH || hover == Mode.RESIZE_BOTH;
		boolean heightHot = hover == Mode.RESIZE_HEIGHT || hover == Mode.RESIZE_BOTH;

		fill(matrices, x2 - 3, y1 + CORNER, x2, y2 - CORNER,
				widthHot ? COLOR_HANDLE : COLOR_HANDLE_DIM);
		fill(matrices, x1 + CORNER, y2 - 3, x2 - CORNER, y2,
				heightHot ? COLOR_HANDLE : COLOR_HANDLE_DIM);
		fill(matrices, x2 - CORNER, y2 - CORNER, x2, y2,
				hover == Mode.RESIZE_BOTH ? COLOR_HANDLE : COLOR_HANDLE_DIM);
	}

	// ------------------------------------------------------------------
	// Input
	// ------------------------------------------------------------------

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (active == null || mode == Mode.NONE) {
			return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
		}

		// Shift is the usual "ignore snapping" modifier.
		snapping = !hasShiftDown();

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
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (mode != Mode.NONE) {
			mode = Mode.NONE;
			active = null;
			HudConfig.save();
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		Element target = selected;
		if (target == Element.LOCATOR && !locatorMovable()) {
			target = Element.QUEST;
		}

		int step = hasShiftDown() ? 10 : 1;
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
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void close() {
		HudConfig.save();
		this.client.setScreen(parent);
	}
}
