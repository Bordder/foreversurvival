package com.foreversurvival;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.foreversurvival.data.QuestDataHolder;
import com.foreversurvival.death.DeathLogger;
import com.foreversurvival.network.ModNetworking;
import com.foreversurvival.quest.QuestJsonLoader;
import com.foreversurvival.quest.QuestManager;
import com.foreversurvival.quest.QuestRegistry;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * ForeverSurvival - a self-contained, strictly linear, months-long survival
 * progression for Minecraft 1.18.2.
 *
 * No questing library, no scripting engine, no team mod: everything from the
 * quest tree to the GUI to the death log lives inside this jar.
 */
public class ForeverSurvival implements ModInitializer {

	public static final String MOD_ID = "foreversurvival";
	public static final Logger LOGGER = LogManager.getLogger("ForeverSurvival");

	/** Key under which all player state is stored inside vanilla player NBT. */
	public static final String NBT_ROOT_KEY = "ForeverSurvivalData";

	/** How often player positions are broadcast for the locator bar, in ticks. */
	private static final int LOCATION_SYNC_INTERVAL = 5;

	@Override
	public void onInitialize() {
		LOGGER.info("ForeverSurvival: building quest tree...");
		QuestRegistry.registerAll();
		QuestJsonLoader.loadAll();
		LOGGER.info("ForeverSurvival: {} main quests, {} side challenges registered.",
				QuestManager.get().getMainQuests().size(),
				QuestManager.get().getSideQuests().size());

		// Payload types must be registered before anything can be sent.
		ModNetworking.registerPayloads();
		ModNetworking.registerServerReceivers();
		DeathLogger.register();

		// Progress is polled once a second for every online player.
		// Player positions go out more often so the locator bar stays responsive.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			QuestManager.get().tick(server);

			if (server.getTicks() % LOCATION_SYNC_INTERVAL == 0) {
				ModNetworking.syncPlayerLocations(server);
			}
		});

		// Fresh join: push the full state so the GUI is correct immediately.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				ModNetworking.syncToClient(handler.player));

		// Respawning creates a brand new player entity - carry the data across,
		// otherwise every death would wipe months of progress.
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			QuestDataHolder.get(newPlayer).copyFrom(QuestDataHolder.get(oldPlayer));
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
				ModNetworking.syncToClient(newPlayer));
	}
}
