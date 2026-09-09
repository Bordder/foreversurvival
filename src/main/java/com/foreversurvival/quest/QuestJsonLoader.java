package com.foreversurvival.quest;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.foreversurvival.ForeverSurvival;
import com.foreversurvival.quest.task.CheckmarkTask;
import com.foreversurvival.quest.task.CraftTask;
import com.foreversurvival.quest.task.ItemTask;
import com.foreversurvival.quest.task.KillTask;
import com.foreversurvival.quest.task.QuestTask;
import com.foreversurvival.quest.task.StructureTask;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Registry;

/**
 * Loads extra quests from {@code config/foreversurvival/quests/*.json}.
 *
 * Additive by design: the 156 built-in quests stay in Java, and JSON files add
 * to them or override one by declaring the same id. A file may contain a single
 * quest object or an array of them.
 *
 * <pre>
 * {
 *   "id": "my_quest",
 *   "phase": "SIDE",                       // PHASE_1..PHASE_6 or SIDE
 *   "title": "Build a Windmill",
 *   "description": "Because you can.",
 *   "icon": "minecraft:oak_planks",
 *   "parent": "p2_04_furnace_array",       // omit for side challenges
 *   "guide": "Static instructions go here.",
 *   "tools": ["Axe", "Scaffolding"],
 *   "tasks": [
 *     { "type": "item",      "id": "wood",  "description": "Collect Planks",
 *       "count": 64, "items": ["minecraft:oak_planks"] },
 *     { "type": "craft",     "id": "fence", "description": "Craft Fences",
 *       "count": 8,  "items": ["minecraft:oak_fence"] },
 *     { "type": "kill",      "id": "zoms",  "description": "Kill Zombies",
 *       "count": 10, "entities": ["minecraft:zombie"] },
 *     { "type": "structure", "id": "vil",   "description": "Find a Village",
 *       "dimension": "minecraft:overworld", "structure": "minecraft:village" },
 *     { "type": "structure", "id": "home",  "description": "Stand in your base",
 *       "blocks": ["minecraft:crafting_table", "minecraft:furnace"] },
 *     { "type": "checkmark", "id": "done",  "description": "Say it is finished" }
 *   ]
 * }
 * </pre>
 *
 * A malformed file is logged and skipped; it never stops the game from loading.
 */
public final class QuestJsonLoader {

	private QuestJsonLoader() {
	}

	public static Path questDirectory() {
		return FabricLoader.getInstance().getConfigDir()
				.resolve(ForeverSurvival.MOD_ID)
				.resolve("quests");
	}

	public static void loadAll() {
		Path directory = questDirectory();

		try {
			Files.createDirectories(directory);
		} catch (IOException e) {
			ForeverSurvival.LOGGER.warn("Could not create the custom quest directory", e);
			return;
		}

		int loaded = 0;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.json")) {
			for (Path file : stream) {
				loaded += loadFile(file);
			}
		} catch (IOException e) {
			ForeverSurvival.LOGGER.warn("Could not read the custom quest directory", e);
			return;
		}

		if (loaded > 0) {
			ForeverSurvival.LOGGER.info("Loaded {} custom quest(s) from {}", loaded, directory);
		}
	}

	private static int loadFile(Path file) {
		try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			JsonElement root = JsonParser.parseReader(reader);

			List<JsonObject> objects = new ArrayList<>();
			if (root.isJsonArray()) {
				for (JsonElement element : root.getAsJsonArray()) {
					objects.add(element.getAsJsonObject());
				}
			} else {
				objects.add(root.getAsJsonObject());
			}

			int count = 0;
			for (JsonObject object : objects) {
				try {
					QuestManager.get().registerOrReplace(parseQuest(object));
					count++;
				} catch (Exception e) {
					ForeverSurvival.LOGGER.error("Skipping a bad quest in {}: {}",
							file.getFileName(), e.getMessage());
				}
			}
			return count;

		} catch (Exception e) {
			ForeverSurvival.LOGGER.error("Could not parse {}: {}", file.getFileName(), e.getMessage());
			return 0;
		}
	}

	private static Quest parseQuest(JsonObject json) {
		String id = required(json, "id").getAsString();

		QuestPhase phase = QuestPhase.SIDE;
		if (json.has("phase")) {
			phase = QuestPhase.valueOf(json.get("phase").getAsString().toUpperCase());
		}

		Quest.Builder builder = Quest.builder(id, phase)
				.title(optionalString(json, "title", id))
				.desc(optionalString(json, "description", ""))
				.guide(optionalString(json, "guide", ""))
				.icon(parseItem(optionalString(json, "icon", "minecraft:paper")))
				.parent(json.has("parent") ? json.get("parent").getAsString() : null);

		if (json.has("tools")) {
			List<String> tools = new ArrayList<>();
			for (JsonElement element : json.getAsJsonArray("tools")) {
				tools.add(element.getAsString());
			}
			builder.tools(tools.toArray(new String[0]));
		}

		JsonArray tasks = required(json, "tasks").getAsJsonArray();
		if (tasks.size() == 0) {
			throw new IllegalArgumentException("quest '" + id + "' has no tasks");
		}
		for (JsonElement element : tasks) {
			builder.task(parseTask(element.getAsJsonObject()));
		}

		return builder.build();
	}

	private static QuestTask parseTask(JsonObject json) {
		String type = required(json, "type").getAsString().toLowerCase();
		String id = required(json, "id").getAsString();
		String description = optionalString(json, "description", id);
		int count = json.has("count") ? json.get("count").getAsInt() : 1;

		switch (type) {
			case "item":
				return new ItemTask(id, description, count, parseItems(json));
			case "craft":
				return new CraftTask(id, description, count, parseItems(json));
			case "kill":
				return new KillTask(id, description, count, parseEntities(json));
			case "checkmark":
				return new CheckmarkTask(id, description);
			case "structure": {
				String dimension = json.has("dimension") ? json.get("dimension").getAsString() : null;

				if (json.has("structure")) {
					Identifier structureId = new Identifier(json.get("structure").getAsString());
					StructureFeature<?> feature = Registry.STRUCTURE_FEATURE.get(structureId);
					if (feature == null) {
						throw new IllegalArgumentException("unknown structure " + structureId);
					}
					return new StructureTask(id, description, dimension, feature);
				}
				return new StructureTask(id, description, dimension, parseBlocks(json));
			}
			default:
				throw new IllegalArgumentException("unknown task type '" + type + "'");
		}
	}

	private static Item[] parseItems(JsonObject json) {
		JsonArray array = required(json, "items").getAsJsonArray();
		Item[] items = new Item[array.size()];
		for (int i = 0; i < array.size(); i++) {
			items[i] = parseItem(array.get(i).getAsString());
		}
		return items;
	}

	private static Item parseItem(String id) {
		Identifier identifier = new Identifier(id);
		Item item = Registry.ITEM.get(identifier);
		if (item == Items.AIR) {
			throw new IllegalArgumentException("unknown item " + identifier);
		}
		return item;
	}

	private static EntityType<?>[] parseEntities(JsonObject json) {
		JsonArray array = required(json, "entities").getAsJsonArray();
		EntityType<?>[] types = new EntityType<?>[array.size()];
		for (int i = 0; i < array.size(); i++) {
			Identifier identifier = new Identifier(array.get(i).getAsString());
			types[i] = Registry.ENTITY_TYPE.get(identifier);
		}
		return types;
	}

	private static Block[] parseBlocks(JsonObject json) {
		JsonArray array = required(json, "blocks").getAsJsonArray();
		Block[] blocks = new Block[array.size()];
		for (int i = 0; i < array.size(); i++) {
			blocks[i] = Registry.BLOCK.get(new Identifier(array.get(i).getAsString()));
		}
		return blocks;
	}

	private static JsonElement required(JsonObject json, String key) {
		JsonElement element = json.get(key);
		if (element == null) {
			throw new IllegalArgumentException("missing required field '" + key + "'");
		}
		return element;
	}

	private static String optionalString(JsonObject json, String key, String fallback) {
		return json.has(key) ? json.get(key).getAsString() : fallback;
	}
}
