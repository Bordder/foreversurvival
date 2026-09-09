package com.foreversurvival.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.foreversurvival.client.LocatorBar;

import net.minecraft.client.gui.Hud;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Lets the locator bar share the experience bar's slot.
 *
 * When the rotation says it is the locator's turn, the vanilla XP bar is
 * cancelled for that frame and the locator is drawn in its place, lined up with
 * the hotbar. The rest of the time this does nothing at all.
 *
 * The HEAD flag is recorded even when we do not cancel, so
 * {@code LocatorBar.renderFree} can tell whether vanilla ever offered the slot.
 * In creative, or while riding a mount, vanilla draws something else there and
 * never calls this - the flag is how the bar still gets shown in those states.
 */
@Mixin(Hud.class)
public class InGameHudMixin {

	@Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
	private void foreversurvival$locatorInXpSlot(MatrixStack matrices, int x, CallbackInfo ci) {
		LocatorBar locator = LocatorBar.get();
		locator.markXpSlotOffered();

		if (locator.shouldTakeXpSlot()) {
			locator.renderXpSlot(matrices, x);
			ci.cancel();
		}
	}
}
