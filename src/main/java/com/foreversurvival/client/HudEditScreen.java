package com.foreversurvival.client;

import com.foreversurvival.quest.Quest;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

/**
 * Layout editor: drag either HUD element to move it, drag its bottom-right
 * handle to resize it.
 *
 * Positions are written back to {@link HudConfig} as screen fractions, so a
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
		RESIZING
	}

	private static final int HANDLE = 8;
	private static final int COLOR_OUTLINE = 0xFF5A7BD0;
	private static final int COLOR_OUTLINE_ACTIVE = 0xFFFFD24A;
	private static final int COLOR_HANDLE = 0xFFFFD24A;
	private static final int TEXT_HINT = 0xFFCFCFDA;
	private static final int TEXT_DIM = 0xFF7A7A8A;

	private final Screen parent;

	private Element active;
	private Mode mode = Mode.NONE;
	/** Offset from the element's origin to the cursor when the drag started. */
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
					HudConfig.locatorX = 0.5D;
					HudConfig.locatorY = 0.04D;
					HudConfig.locatorScale = 1.0D;
					HudConfig.save();
				}));

		addDrawableChild(new ButtonWidget(this.width / 2 - 50, y, 100, 20,
				new LiteralText("Done"), button -> {
					HudConfig.save();
					this.client.setScreen(parent);
				}));

		addDrawableChild(new ButtonWidget(this.width / 2 + 54, y, 100, 20,
				new LiteralText("Toggle bar"), button -> {
					HudConfig.locatorEnabled = !HudConfig.locatorEnabled;
					HudConfig.save();
				}));
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	// ------------------------------------------------------------------
	// Element geometry (in screen pixels)
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
		// Leave room for the name and distance lines drawn under the bar.
		return new int[] { size[0], size[1] + (HudConfig.locatorShowDistance ? 22 : 12) };
	}

	/** Top-left corner in pixels. The locator bar is positioned by its centre. */
	private double[] originOf(Element element) {
		int[] size = sizeOf(element);
		double scale = scaleOf(element);

		if (element == Element.QUEST) {
			return new double[] { HudConfig.hudX * this.width, HudConfig.hudY * this.height };
		}
		return new double[] {
				HudConfig.locatorX * this.width - (size[0] * scale) / 2.0D,
				HudConfig.locatorY * this.height
		};
	}

	private void setOrigin(Element element, double pixelX, double pixelY) {
		int[] size = sizeOf(element);
		double scale = scaleOf(element);

		double maxX = this.width - 8;
		double maxY = this.height - 8;
		pixelX = HudConfig.clamp(pixelX, -size[0] * scale + 8, maxX);
		pixelY = HudConfig.clamp(pixelY, 0.0D, maxY);

		if (element == Element.QUEST) {
			HudConfig.hudX = HudConfig.clamp(pixelX / this.width, 0.0D, 1.0D);
			HudConfig.hudY = HudConfig.clamp(pixelY / this.height, 0.0D, 1.0D);
		} else {
			double centre = pixelX + (size[0] * scale) / 2.0D;
			HudConfig.locatorX = HudConfig.clamp(centre / this.width, 0.0D, 1.0D);
			HudConfig.locatorY = HudConfig.clamp(pixelY / this.height, 0.0D, 1.0D);
		}
	}

	private void setScale(Element element, double scale) {
		scale = HudConfig.clamp(scale, 0.5D, 2.0D);
		if (element == Element.QUEST) {
			HudConfig.hudScale = scale;
		} else {
			HudConfig.locatorScale = scale;
		}
	}

	private boolean isOver(Element element, double mouseX, double mouseY) {
		double[] origin = originOf(element);
		int[] size = sizeOf(element);
		double scale = scaleOf(element);

		return mouseX >= origin[0] && mouseX <= origin[0] + size[0] * scale
				&& mouseY >= origin[1] && mouseY <= origin[1] + size[1] * scale;
	}

	private boolean isOverHandle(Element element, double mouseX, double mouseY) {
		double[] handle = handleOf(element);
		return mouseX >= handle[0] && mouseX <= handle[0] + HANDLE
				&& mouseY >= handle[1] && mouseY <= handle[1] + HANDLE;
	}

	private double[] handleOf(Element element) {
		double[] origin = originOf(element);
		int[] size = sizeOf(element);
		double scale = scaleOf(element);
		return new double[] { origin[0] + size[0] * scale - HANDLE, origin[1] + size[1] * scale - HANDLE };
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

		if (HudConfig.locatorEnabled) {
			double[] barOrigin = originOf(Element.LOCATOR);
			LocatorBar.get().renderPreviewAt(matrices, barOrigin[0], barOrigin[1],
					HudConfig.locatorScale);
		}

		drawOutline(matrices, Element.QUEST, mouseX, mouseY);
		if (HudConfig.locatorEnabled) {
			drawOutline(matrices, Element.LOCATOR, mouseX, mouseY);
		}

		String hint = "Drag to move  -  drag the corner handle to resize";
		textRenderer.draw(matrices, hint, (this.width - textRenderer.getWidth(hint)) / 2.0F, 10.0F,
				TEXT_HINT);

		String detail = String.format("Panel %.2fx   Bar %.2fx", HudConfig.hudScale,
				HudConfig.locatorScale);
		textRenderer.draw(matrices, detail, (this.width - textRenderer.getWidth(detail)) / 2.0F, 22.0F,
				TEXT_DIM);

		super.render(matrices, mouseX, mouseY, delta);
	}

	private void drawOutline(MatrixStack matrices, Element element, int mouseX, int mouseY) {
		double[] origin = originOf(element);
		int[] size = sizeOf(element);
		double scale = scaleOf(element);

		int x1 = (int) Math.round(origin[0]);
		int y1 = (int) Math.round(origin[1]);
		int x2 = (int) Math.round(origin[0] + size[0] * scale);
		int y2 = (int) Math.round(origin[1] + size[1] * scale);

		boolean hot = active == element || isOver(element, mouseX, mouseY);
		int colour = hot ? COLOR_OUTLINE_ACTIVE : COLOR_OUTLINE;

		fill(matrices, x1, y1, x2, y1 + 1, colour);
		fill(matrices, x1, y2 - 1, x2, y2, colour);
		fill(matrices, x1, y1, x1 + 1, y2, colour);
		fill(matrices, x2 - 1, y1, x2, y2, colour);

		double[] handle = handleOf(element);
		fill(matrices, (int) handle[0], (int) handle[1],
				(int) handle[0] + HANDLE, (int) handle[1] + HANDLE, COLOR_HANDLE);
	}

	// ------------------------------------------------------------------
	// Input
	// ------------------------------------------------------------------

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			// Locator first: it is usually the smaller target.
			for (Element element : new Element[] { Element.LOCATOR, Element.QUEST }) {
				if (element == Element.LOCATOR && !HudConfig.locatorEnabled) {
					continue;
				}

				if (isOverHandle(element, mouseX, mouseY)) {
					// Pin the origin now: the locator bar is anchored by its
					// centre, so recomputing it while the scale changes would
					// make the element crawl away from the cursor.
					double[] origin = originOf(element);
					active = element;
					mode = Mode.RESIZING;
					grabX = origin[0];
					grabY = origin[1];
					return true;
				}

				if (isOver(element, mouseX, mouseY)) {
					double[] origin = originOf(element);
					active = element;
					mode = Mode.MOVING;
					grabX = mouseX - origin[0];
					grabY = mouseY - origin[1];
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (active != null && mode == Mode.MOVING) {
			setOrigin(active, mouseX - grabX, mouseY - grabY);
			return true;
		}

		if (active != null && mode == Mode.RESIZING) {
			int[] size = sizeOf(active);
			if (size[0] > 0) {
				// Scale so the dragged corner follows the cursor horizontally,
				// measured from the origin captured when the drag began.
				setScale(active, (mouseX - grabX) / size[0]);
			}
			return true;
		}

		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
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
