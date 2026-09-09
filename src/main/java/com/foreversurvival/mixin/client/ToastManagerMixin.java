package com.foreversurvival.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.foreversurvival.client.HudConfig;

import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;

/**
 * Swallows the vanilla advancement pop-up.
 *
 * ForeverSurvival runs its own progression and shows its own completion
 * feedback, so the built-in "Advancement Made!" toast is just noise on top of
 * it. Only advancement toasts are dropped - recipe unlocks, tutorial hints and
 * system toasts still come through.
 *
 * Purely cosmetic and client-side: the advancement itself is still granted.
 */
@Mixin(ToastManager.class)
public class ToastManagerMixin {

	@Inject(method = "add", at = @At("HEAD"), cancellable = true)
	private void foreversurvival$hideAdvancementToasts(Toast toast, CallbackInfo ci) {
		if (HudConfig.hideAdvancementToasts && toast instanceof AdvancementToast) {
			ci.cancel();
		}
	}
}
