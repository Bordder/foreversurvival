package com.foreversurvival.client;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.foreversurvival.ForeverSurvival;
import com.foreversurvival.network.ModNetworking;
import com.foreversurvival.network.PlayerLocation;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.nbt.NbtCompound;

/**
 * Client entrypoint: one key binding ('U'), two packet receivers, and the HUD.
 */
public class ForeverSurvivalClient implements ClientModInitializer {

	public static KeyBinding openQuestsKey;
	public static KeyBinding toggleHudKey;

	@Override
	public void onInitializeClient() {
		HudConfig.load();
		QuestHud.register();
		DurabilityTooltip.register();

		// Both defaults are keys vanilla leaves unbound, and both show up in
		// Options -> Controls -> ForeverSurvival for rebinding.
		openQuestsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key." + ForeverSurvival.MOD_ID + ".open_quests",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_U,
				"key.categories." + ForeverSurvival.MOD_ID));

		toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key." + ForeverSurvival.MOD_ID + ".toggle_hud",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_APOSTROPHE,
				"key.categories." + ForeverSurvival.MOD_ID));

		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.SYNC_DATA,
				(client, handler, buf, responseSender) -> {
					// Read off the network thread, apply on the client thread.
					NbtCompound nbt = buf.readNbt();
					client.execute(() -> {
						if (nbt != null) {
							ClientQuestState.accept(nbt);
						}
					});
				});

		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.PLAYER_LOCATIONS,
				(client, handler, buf, responseSender) -> {
					int count = buf.readVarInt();
					List<PlayerLocation> locations = new ArrayList<>(count);
					for (int i = 0; i < count; i++) {
						String name = buf.readString(64);
						double x = buf.readDouble();
						double y = buf.readDouble();
						double z = buf.readDouble();
						String dimension = buf.readString(128);
						locations.add(new PlayerLocation(name, x, y, z, dimension));
					}
					client.execute(() -> ClientLocatorState.accept(locations));
				});

		// Stale positions from a previous server would point at nothing.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientLocatorState.clear());

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openQuestsKey.wasPressed()) {
				if (client.player != null) {
					client.setScreen(new QuestScreen());
				}
			}

			while (toggleHudKey.wasPressed()) {
				// One key hides both HUD elements at once, for screenshots.
				boolean showing = HudConfig.enabled || HudConfig.locatorEnabled;
				HudConfig.enabled = !showing;
				HudConfig.locatorEnabled = !showing;
				HudConfig.save();
			}
		});
	}
}
