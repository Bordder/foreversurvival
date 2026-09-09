package com.foreversurvival.client;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.foreversurvival.ForeverSurvival;
import com.foreversurvival.network.ModPayloads;
import com.foreversurvival.network.PlayerLocation;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.nbt.CompoundTag;

/**
 * Client entrypoint: one key binding ('U'), two packet receivers, and the HUD.
 */
public class ForeverSurvivalClient implements ClientModInitializer {

	/** 26.2 key categories are registered objects, not free-form strings. */
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(ForeverSurvival.MOD_ID, "main"));

	public static KeyMapping openQuestsKey;
	public static KeyMapping toggleHudKey;

	@Override
	public void onInitializeClient() {
		HudConfig.load();
		QuestHud.register();
		DurabilityTooltip.register();

		// Both defaults are keys vanilla leaves unbound, and both show up in
		// Options -> Controls -> ForeverSurvival for rebinding.
		openQuestsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key." + ForeverSurvival.MOD_ID + ".open_quests",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_U,
				CATEGORY));

		toggleHudKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key." + ForeverSurvival.MOD_ID + ".toggle_hud",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_APOSTROPHE,
				CATEGORY));

		// Decoding now happens in the payload codec, off the client thread, so
		// each handler only has to apply an already-parsed record.
		ClientPlayNetworking.registerGlobalReceiver(ModPayloads.SyncData.TYPE,
				(payload, context) -> context.client().execute(
						() -> ClientQuestState.accept(payload.root())));

		ClientPlayNetworking.registerGlobalReceiver(ModPayloads.PlayerLocations.TYPE,
				(payload, context) -> {
					List<PlayerLocation> locations = new ArrayList<>(payload.entries().size());
					for (ModPayloads.PlayerLocations.Entry entry : payload.entries()) {
						locations.add(new PlayerLocation(entry.name(), entry.x(), entry.y(),
								entry.z(), entry.dimension()));
					}
					context.client().execute(() -> ClientLocatorState.accept(locations));
				});

		// Stale positions from a previous server would point at nothing.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientLocatorState.clear());

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openQuestsKey.consumeClick()) {
				if (client.player != null) {
					client.setScreenAndShow(new QuestScreen());
				}
			}

			while (toggleHudKey.consumeClick()) {
				// One key hides both HUD elements at once, for screenshots.
				boolean showing = HudConfig.enabled || HudConfig.locatorEnabled;
				HudConfig.enabled = !showing;
				HudConfig.locatorEnabled = !showing;
				HudConfig.save();
			}
		});
	}
}
