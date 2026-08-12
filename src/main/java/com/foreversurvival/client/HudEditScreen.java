package com.foreversurvival.client;

import com.foreversurvival.quest.Quest;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

/**
 * Layout editor.
 *
 * Drag an element's body to move it. Drag its RIGHT edge to change width, its
 * BOTTOM edge to change height, or the corner grip to change both - width and
 * height are independent, so the box can be stretched as far as you like in
 * either direction without distorting the text.
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

	private static final int GRIP = 8;
	private static final int EDGE = 4;
	private static final int COLOR_OUTLINE = 0xFF5A7BD0;
	private static final int COLOR_OUTLINE_ACTIVE = 0xFFFFD24A;
	private static final int COLOR_GRIP = 0xFFFFD24A;
	private static final int COLOR_EDGE = 0xAAFFD24A;
	private static final int TEXT_HINT = 0xFFCFCFDA;
	private static final int TEXT_DIM = 0xFF7A7A8A;

	private final Screen parent;

	private Element active;
	private Mode mode = Mode.NONE;
	private double grabX;
	private double grabY;

	public HudEditScreen(Screen parent) {
		super(new LiteralText("Edit HUD Layout"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int y = this.height - 28;

		addDrawableChild(new ButtonWidget(this.width / 2 - 154, y, 100, 20,
				new LiteralText("Reset layout"), button -> {
					HudConfig.hudX = 0.72D;
					HudConfig.hudY = 0.02D;
					HudConfig.hudScale = 1.0D;
					HudConfig.hudWidth = 150;
					HudConfig.hudMinHeight = 0;
					HudConfig.locatorX = 0.5D;
					HudConfig.locatorY = 0.04D;
					HudConfig.locatorScale = 1.0D;
					HudConfig.locatorWidth = 182;
					HudConfig.locatorHeight = 9;
					HudConfig.save();
				}));

		addDrawableChild(new ButtonWidget(this.width / 2 - 50, y, 100, 20,
				new LiteralText("Done"), button -> {
					HudConfig.save();
					this.client.setScreen(parent);
				}));

		addDrawableChild(new ButtonWidget(this.width / 2 + 54, y, 100, 20,
				new LiteralText(HudConfig.locatorXpBarMode ? "Bar: XP slot" : "Bar: free"),
				button -> {
					HudConfig.locatorXpBarMode = !HudConfig.locatorXpBarMode;
					HudConfig.save();
					this.client.setScreen(new HudEditScreen(parent));
				}));
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	/** The locator can only be dragged when it is not pinned to the XP slot. */
	private boolean locatorMovable() {
		return HudConfig.locatorEnabled && !HudConfig.locatorXpBarMode;
	}

	// ------------------------------------------------------------------
	// Geometry
	// ------------------------------------------------------------------

	private double scaleOf(Element element) {
		return element == Element.QUEST ? HudConfig.hudScale : HudConfig.locatorScale;
	}

	private int[] sizeOf(Element element) {
		if (element == Element.QUEST) {
			Quest quest = ClientQuestState.getCurrentMainQuest();
			return QuestHud.get().measure(quest);
		}
		int[] size = LocatorBar.get().measure();
		return new int[] { size[0], size[1] + 12 };
	}

	private double[] originOf(Element element) {
		if (element == Element.QUEST) {
			return new double[] { HudConfig.hudX * this.width, HudConfig.hudY * this.height };
		}

		int[] size = sizeOf(element);
		double scale = scaleOf(element);
		return new double[] {
				HudConfig.locatorX * this.width - (size[0] * scale) / 2.0D,
				HudConfig.locatorY * this.height
		};
	}

	private void setOrigin(Element element, double pixelX, double pixelY) {
		int[] size = sizeOf(element);
		double scale = scaleOf(element);

		pixelX = HudConfig.clamp(pixelX, -size[0] * scale + 8, (double) this.width - 8);
		pixelY = HudConfig.clamp(pixelY, 0.0D, (double) this.height - 8);

		if (element == Element.QUEST) {
			HudConfig.hudX = HudConfig.clamp(pixelX / this.width, 0.0D, 1.0D);
			HudConfig.hudY = HudConfig.clamp(pixelY / this.height, 0.0D, 1.0D);
		} else {
			double centre = pixelX + (size[0] * scale) / 2.0D;
			HudConfig.locatorX = HudConfig.clamp(centre / this.width, 0.0D, 1.0D);
			HudConfig.locatorY = HudConfig.clamp(pixelY / this.height, 0.0D, 1.0D);
		}
	}

	private void setWidth(Element element, double pixels) {
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
			// Height is a floor: the panel still grows to fit its content.
			HudConfig.hudMinHeight = HudConfig.clamp(value, 0, HudConfig.MAX_HUD_HEIGHT);
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
		boolean inside = mouseX >= b[0] - EDGE && mouseX <= b[2] + EDGE
				&& mouseY >= b[1] - EDGE && mouseY <= b[3] + EDGE;
		if (!inside) {
			return Mode.NONE;
		}

		boolean nearRight = mouseX >= b[2] - EDGE;
		boolean nearBottom = mouseY >= b[3] - EDGE;

		if (nearRight && nearBottom) {
			return Mode.RESIZE_BOTH;
		}
		if (nearRight) {
			return Mode.RESIZE_WIDTH;
		}
		if (nearBottom) {
			return Mode.RESIZE_HEIGHT;
		}
		if (mouseX <= b[2] && mouseY <= b[3]) {
			return Mode.MOVING;
		}
		return Mode.NONE;
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

		String hint = "Drag to move  -  drag the right edge for width, the bottom edge for height";
		textRenderer.draw(matrices, hint, (this.width - textRenderer.getWidth(hint)) / 2.0F, 10.0F,
				TEXT_HINT);

		String detail = "Panel " + HudConfig.hudWidth + " x "
				+ (HudConfig.hudMinHeight == 0 ? "auto" : String.valueOf(HudConfig.hudMinHeight))
				+ "     Bar " + HudConfig.locatorWidth + " x " + HudConfig.locatorHeight;
		textRenderer.draw(matrices, detail, (this.width - textRenderer.getWidth(detail)) / 2.0F, 22.0F,
				TEXT_DIM);

		if (HudConfig.locatorEnabled && HudConfig.locatorXpBarMode) {
			String note = "Locator bar sits in the XP bar slot - switch it to free "
					+ "placement to move it here";
			textRenderer.draw(matrices, note, (this.width - textRenderer.getWidth(note)) / 2.0F, 34.0F,
					TEXT_DIM);
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

		// Edge grips: right for width, bottom for height, corner for both.
		fill(matrices, x2 - 2, y1 + GRIP, x2, y2 - GRIP, COLOR_EDGE);
		fill(matrices, x1 + GRIP, y2 - 2, x2 - GRIP, y2, COLOR_EDGE);
		fill(matrices, x2 - GRIP, y2 - GRIP, x2, y2, COLOR_GRIP);
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
	public void close() {
		HudConfig.save();
		this.client.setScreen(parent);
	}
}
