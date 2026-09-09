package com.foreversurvival.quest;

import static com.foreversurvival.quest.QuestRegistry.BOATS;
import static com.foreversurvival.quest.QuestRegistry.FENCES;
import static com.foreversurvival.quest.QuestRegistry.OVERWORLD;
import static com.foreversurvival.quest.QuestRegistry.RAW_FISH;
import static com.foreversurvival.quest.QuestRegistry.WOOL;
import static com.foreversurvival.quest.QuestRegistry.addMain;

import static com.foreversurvival.quest.QuestRegistry.HOES;

import com.foreversurvival.quest.task.CraftTask;
import com.foreversurvival.quest.task.ItemTask;
import com.foreversurvival.quest.task.KillTask;
import com.foreversurvival.quest.task.PositionTask;
import com.foreversurvival.quest.task.StatTask;
import com.foreversurvival.quest.task.StructureTask;
import com.foreversurvival.quest.task.UseTask;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.stats.Stats;
import net.minecraft.tags.StructureTags;

/**
 * PHASE 2 - THE ESTABLISHMENT (25 quests).
 *
 * The first few weeks: iron, food security, a village, the deep caves,
 * diamonds, and an enchanting setup. This is the phase where a temporary
 * shelter turns into a base you actually live in.
 *
 * ORDER MATTERS: nothing here asks for a material the chain has not already
 * introduced. The compass comes after Deep Delving because it needs redstone;
 * rails come after it because powered rails need gold; the enchanting table
 * comes after Paper Trail because 15 bookshelves is 45 books.
 */
final class Phase2Quests {

	private Phase2Quests() {
	}

	static void register() {
		addMain(Quest.builder("p2_01_iron_age", QuestPhase.PHASE_2)
				.title("The Iron Age")
				.desc("Raw iron is a rock. Smelt it.")
				.icon(Items.IRON_INGOT)
				.guide("An Iron Pickaxe is required for diamond, gold, redstone and emerald ore. "
						+ "Anything weaker destroys them.")
				.tools("Furnace", "Fuel")
				.task(new ItemTask("ingots", "Smelt Iron Ingots", 20, Items.IRON_INGOT),
						new CraftTask("i_pick", "Craft an Iron Pickaxe", 1, Items.IRON_PICKAXE),
						new CraftTask("i_sword", "Craft an Iron Sword", 1, Items.IRON_SWORD)));

		addMain(Quest.builder("p2_02_bucket_list", QuestPhase.PHASE_2)
				.title("Bucket List")
				.desc("Three ingots that solve lava, fire, farming and falling.")
				.icon(Items.WATER_BUCKET)
				.guide("3 iron ingots in a V. Water cancels fall damage if you place it before you "
						+ "land, and turns lava into obsidian.")
				.tools("3 Iron Ingots")
				.task(new CraftTask("bucket", "Craft a Bucket", 1, Items.BUCKET),
						new ItemTask("water", "Carry a Water Bucket", 1, Items.WATER_BUCKET)));

		addMain(Quest.builder("p2_03_clad_in_iron", QuestPhase.PHASE_2)
				.title("Clad in Iron")
				.desc("A full iron set halves almost everything that hits you.")
				.icon(Items.IRON_CHESTPLATE)
				.guide("The full set costs 24 ingots. A Shield is 6 planks + 1 ingot and blocks "
						+ "skeleton arrows outright.")
				.tools("Crafting Table", "24 Iron Ingots")
				.task(new CraftTask("helm", "Craft an Iron Helmet", 1, Items.IRON_HELMET),
						new CraftTask("chest", "Craft an Iron Chestplate", 1, Items.IRON_CHESTPLATE),
						new CraftTask("legs", "Craft Iron Leggings", 1, Items.IRON_LEGGINGS),
						new CraftTask("boots", "Craft Iron Boots", 1, Items.IRON_BOOTS),
						new CraftTask("shield", "Craft a Shield", 1, Items.SHIELD)));

		addMain(Quest.builder("p2_04_furnace_array", QuestPhase.PHASE_2)
				.title("The Furnace Array")
				.desc("Two specialists that smelt twice as fast as the generalist.")
				.icon(Items.BLAST_FURNACE)
				.guide("A Blast Furnace does ores and armour at double speed; a Smoker does food. "
						+ "Both cost 1 furnace plus 5 iron or 5 logs respectively.")
				.tools("Furnace", "Iron Ingots", "Logs")
				.task(new CraftTask("blast", "Craft a Blast Furnace", 1, Items.BLAST_FURNACE),
						new CraftTask("smoker", "Craft a Smoker", 1, Items.SMOKER)));

		addMain(Quest.builder("p2_05_seeds_to_bread", QuestPhase.PHASE_2)
				.title("Seeds to Bread")
				.desc("A farm that outpaces your appetite.")
				.icon(Items.BREAD)
				.guide("A 9x9 plot around one centre water block is the classic layout. Light it with "
						+ "torches so crops keep growing at night and mobs cannot trample them.")
				.tools("Hoe", "Water Bucket", "Torches")
				.task(new UseTask("tilled", "Till Farmland", 40, HOES),
						new ItemTask("wheat", "Harvest Wheat", 48, Items.WHEAT),
						new CraftTask("bread", "Bake Bread", 16, Items.BREAD)));

		addMain(Quest.builder("p2_06_green_thumb", QuestPhase.PHASE_2)
				.title("Green Thumb")
				.desc("Wheat alone is a boring diet.")
				.icon(Items.CARROT)
				.guide("Carrots and potatoes come from village farms and rarely from zombies. "
						+ "Melon and pumpkin stems need one free dirt block beside them to fruit.")
				.tools("Hoe", "Water Bucket")
				.task(new ItemTask("carrots", "Collect Carrots", 16, Items.CARROT),
						new ItemTask("potatoes", "Collect Potatoes", 16, Items.POTATO),
						new ItemTask("gourd", "Collect a Pumpkin or Melon", 1,
								Items.PUMPKIN, Items.MELON)));

		addMain(Quest.builder("p2_07_the_fisherman", QuestPhase.PHASE_2)
				.title("The Fisherman")
				.desc("Infinite food from a stick and some string.")
				.icon(Items.FISHING_ROD)
				.guide("3 sticks + 2 string. Fishing works in any water at least 1 block deep and is "
						+ "the cheapest renewable food in the early game.")
				.tools("Fishing Rod", "Water")
				.task(new CraftTask("rod", "Craft a Fishing Rod", 1, Items.FISHING_ROD),
						new ItemTask("fish", "Catch Fish", 16, RAW_FISH),
						new ItemTask("cooked_fish", "Cook Fish", 8,
								Items.COOKED_COD, Items.COOKED_SALMON)));

		addMain(Quest.builder("p2_08_animal_husbandry", QuestPhase.PHASE_2)
				.title("Animal Husbandry")
				.desc("A renewable food and leather supply, penned in and safe.")
				.icon(Items.LEATHER)
				.guide("Wheat breeds cows and sheep, carrots breed pigs, seeds breed chickens. "
						+ "Fence and light the pen or they get eaten overnight.")
				.tools("Fences", "Wheat")
				.task(new CraftTask("fence", "Craft Fences", 16, FENCES),
						new StatTask("bred", "Breed Animals", 6, Stats.ANIMALS_BRED),
						new ItemTask("leather", "Collect Leather", 12, Items.LEATHER)));

		addMain(Quest.builder("p2_09_shear_delight", QuestPhase.PHASE_2)
				.title("Shear Delight")
				.desc("Sheep regrow wool. Killing them does not.")
				.icon(Items.SHEARS)
				.guide("2 iron ingots. Shearing gives 1-3 wool and the sheep regrows it after eating "
						+ "grass, so one small flock covers every bed and carpet you will ever need.")
				.tools("2 Iron Ingots", "Sheep Pen")
				.task(new CraftTask("shears", "Craft Shears", 1, Items.SHEARS),
						new ItemTask("wool_stock", "Stockpile Wool", 32, WOOL)));

		addMain(Quest.builder("p2_10_neighbours", QuestPhase.PHASE_2)
				.title("Neighbours")
				.desc("Other people live here, and they want your wheat.")
				.icon(Items.EMERALD)
				.guide("Farmers buy crops for emeralds, and every trade you make lowers that "
						+ "villager's prices permanently.")
				.tools("Crops to sell")
				.task(new StructureTask("village", "Stand inside a Village", OVERWORLD,
								StructureTags.VILLAGE),
						new StatTask("trade", "Trade with Villagers", 8, Stats.TRADED_WITH_VILLAGER),
						new ItemTask("emeralds", "Collect Emeralds", 12, Items.EMERALD)));

		addMain(Quest.builder("p2_11_set_sail", QuestPhase.PHASE_2)
				.title("Set Sail")
				.desc("The fastest travel you have until Elytra.")
				.icon(Items.OAK_BOAT)
				.guide("Boats move at 8 blocks per second on water against a sprint's 5.6 on land. "
						+ "They also let you carry an animal across an ocean.")
				.tools("5 Planks")
				.task(new CraftTask("boat", "Craft a Boat", 1, BOATS),
						new StatTask("sailed", "Travel by Boat (blocks)", 1000,
								Stats.BOAT_ONE_CM, 100)));

		addMain(Quest.builder("p2_12_the_stable", QuestPhase.PHASE_2)
				.title("The Stable")
				.desc("Four legs beat two over land.")
				.icon(Items.SADDLE)
				.guide("Mount a horse repeatedly with an empty hand until hearts appear. Saddles are "
						+ "not craftable - they come from chests, fishing and Leatherworker trades.")
				.tools("Saddle", "Lead", "Wheat or Apples")
				.task(new ItemTask("saddle", "Obtain a Saddle", 1, Items.SADDLE),
						new UseTask("saddled", "Saddle a Horse", 1, Items.SADDLE),
						new CraftTask("lead", "Craft Leads", 2, Items.LEAD)));

		addMain(Quest.builder("p2_13_copper_rush", QuestPhase.PHASE_2)
				.title("The Copper Rush")
				.desc("The most abundant metal nobody bothers to mine.")
				.icon(Items.RAW_COPPER)
				.guide("Copper peaks at Y=48 and is densest in Dripstone Caves. A Lightning Rod on "
						+ "your roof stops the base burning down.")
				.tools("Stone Pickaxe or better")
				.task(new ItemTask("raw_copper", "Collect Raw Copper", 40, Items.RAW_COPPER),
						new CraftTask("copper_block", "Craft a Copper Block", 1, Items.COPPER_BLOCK.weathering().pick(WeatherState.UNAFFECTED)),
						new CraftTask("rod", "Craft a Lightning Rod", 1, Items.LIGHTNING_ROD.weathering().pick(WeatherState.UNAFFECTED))));

		addMain(Quest.builder("p2_14_below_zero", QuestPhase.PHASE_2)
				.title("Below Zero")
				.desc("Under Y=0 the stone turns black and the rules change.")
				.icon(Items.DEEPSLATE)
				.guide("Deepslate starts at Y=0 and runs to bedrock at Y=-64. It takes twice as long "
						+ "to mine as stone, and open lava lakes are common near Y=-54.")
				.tools("Iron Pickaxe", "Water Bucket", "Torches")
				.task(new ItemTask("deepslate", "Collect Deepslate", 48,
								Items.DEEPSLATE, Items.COBBLED_DEEPSLATE),
						new PositionTask("bedrock", "Reach Y=-58, just above bedrock",
								PositionTask.Kind.BELOW_Y, -58)));

		addMain(Quest.builder("p2_15_deep_delving", QuestPhase.PHASE_2)
				.title("Deep Delving")
				.desc("Gold, lapis and redstone all live down here.")
				.icon(Items.GOLD_INGOT)
				.guide("Gold peaks at Y=-16. Lapis peaks at Y=0. Redstone peaks at Y=-59 and gets "
						+ "much denser below Y=-32.")
				.tools("Iron Pickaxe", "Water Bucket", "Torches")
				.task(new ItemTask("raw_gold", "Collect Raw Gold", 16, Items.RAW_GOLD),
						new ItemTask("redstone", "Collect Redstone Dust", 32, Items.REDSTONE),
						new ItemTask("lapis", "Collect Lapis Lazuli", 20, Items.LAPIS_LAZULI)));

		addMain(Quest.builder("p2_16_the_mineshaft", QuestPhase.PHASE_2)
				.title("The Abandoned Mineshaft")
				.desc("Someone dug here first. It did not end well for them.")
				.icon(Items.RAIL)
				.guide("Mineshafts generate below Y=0 in most biomes. Cave spider spawners hide "
						+ "behind the cobwebs - break the webs with a sword and torch the spawner fast.")
				.tools("Iron Sword", "Torches", "Milk Bucket")
				.task(new StructureTask("mineshaft", "Find an Abandoned Mineshaft", OVERWORLD,
								StructureTags.MINESHAFT),
						new ItemTask("string", "Collect String from cobwebs", 24, Items.STRING),
						new KillTask("cave_spiders", "Kill Cave Spiders", 12, EntityTypes.CAVE_SPIDER)));

		addMain(Quest.builder("p2_17_rails_and_carts", QuestPhase.PHASE_2)
				.title("Rails and Minecarts")
				.desc("Mineshafts hand you the rails. Use them.")
				.icon(Items.MINECART)
				.guide("Powered rails need gold and redstone. One powered rail every 8 flat blocks "
						+ "keeps a cart at full speed; a chest minecart hauls your mining trip home.")
				.tools("Iron Ingots", "Gold Ingots", "Redstone", "Sticks")
				.task(new CraftTask("minecart", "Craft a Minecart", 1, Items.MINECART),
						new CraftTask("rails", "Craft Rails", 64, Items.RAIL),
						new CraftTask("powered", "Craft Powered Rails", 6, Items.POWERED_RAIL)));

		addMain(Quest.builder("p2_18_the_spawner", QuestPhase.PHASE_2)
				.title("The Monster Spawner")
				.desc("A mossy room, two chests, and something producing monsters.")
				.icon(Items.SPAWNER)
				.guide("Dungeons are mossy cobblestone rooms holding a Monster Spawner and 1-2 "
						+ "chests. Light the spawner to level 12 or higher to stop it, or build a farm "
						+ "around it - it never runs out.")
				.tools("Iron Sword", "Torches", "Shield")
				.task(new StructureTask("dungeon", "Find a Monster Spawner room", OVERWORLD,
								Blocks.SPAWNER, Blocks.MOSSY_COBBLESTONE),
						new ItemTask("rotten", "Collect Rotten Flesh", 32, Items.ROTTEN_FLESH),
						new ItemTask("bones", "Collect Bones", 24, Items.BONE)));

		addMain(Quest.builder("p2_19_redstone_basics", QuestPhase.PHASE_2)
				.title("Redstone Basics")
				.desc("The half of the game most players never touch.")
				.icon(Items.REDSTONE)
				.guide("A Piston needs 3 planks, 4 cobblestone, 1 iron and 1 redstone. Repeaters "
						+ "delay and strengthen a signal; levers hold it on.")
				.tools("Redstone", "Iron Ingot", "Cobblestone")
				.task(new CraftTask("r_torch", "Craft a Redstone Torch", 1, Items.REDSTONE_TORCH),
						new CraftTask("lever", "Craft a Lever", 1, Items.LEVER),
						new CraftTask("piston", "Craft a Piston", 1, Items.PISTON),
						new CraftTask("repeater", "Craft a Repeater", 1, Items.REPEATER)));

		addMain(Quest.builder("p2_20_paper_trail", QuestPhase.PHASE_2)
				.title("Paper Trail")
				.desc("Forty-five books is a lot of cows and a lot of cane.")
				.icon(Items.BOOK)
				.guide("Sugar cane grows on sand or dirt next to water at any light level. 3 cane = "
						+ "3 paper, 3 paper + 1 leather = 1 book. 15 bookshelves needs 45 of each.")
				.tools("Sugar Cane Farm", "Cows")
				.task(new ItemTask("cane", "Collect Sugar Cane", 48, Items.SUGAR_CANE),
						new CraftTask("paper", "Craft Paper", 45, Items.PAPER),
						new CraftTask("books", "Craft Books", 15, Items.BOOK)));

		addMain(Quest.builder("p2_21_cartography", QuestPhase.PHASE_2)
				.title("Know Where You Are")
				.desc("A compass, a map, and the end of getting lost.")
				.icon(Items.COMPASS)
				.guide("A compass is 4 iron plus 1 redstone; a map is 8 paper around it. The compass "
						+ "points at your world spawn, not your bed. Maps fill in as you walk, and one "
						+ "in an item frame becomes a wall map that keeps updating.")
				.tools("4 Iron Ingots", "1 Redstone", "8 Paper")
				.task(new CraftTask("compass", "Craft a Compass", 1, Items.COMPASS),
						new CraftTask("map", "Craft a Map", 1, Items.MAP),
						new ItemTask("filled", "Carry a Filled Map", 1, Items.FILLED_MAP)));

		addMain(Quest.builder("p2_22_diamonds_are_forever", QuestPhase.PHASE_2)
				.title("Diamonds Are Forever")
				.desc("The line between surviving and thriving.")
				.icon(Items.DIAMOND)
				.guide("Diamond peaks at Y=-59 and never spawns above Y=16. In 1.18 it is rarer when "
						+ "exposed to air, so branch mine through solid deepslate. Eight covers a "
						+ "pickaxe, a sword and the enchanting table.")
				.tools("Iron Pickaxe", "Water Bucket", "Torches")
				.task(new ItemTask("diamonds", "Collect Diamonds", 14, Items.DIAMOND)));

		addMain(Quest.builder("p2_23_diamond_standard", QuestPhase.PHASE_2)
				.title("The Diamond Standard")
				.desc("The pickaxe that opens the Nether.")
				.icon(Items.DIAMOND_PICKAXE)
				.guide("A Diamond Pickaxe is the only way to mine obsidian - for the enchanting "
						+ "table first, and the Nether portal after it.")
				.tools("5 Diamonds", "Sticks")
				.task(new CraftTask("d_pick", "Craft a Diamond Pickaxe", 1, Items.DIAMOND_PICKAXE),
						new CraftTask("d_sword", "Craft a Diamond Sword", 1, Items.DIAMOND_SWORD)));

		addMain(Quest.builder("p2_24_the_enchanter", QuestPhase.PHASE_2)
				.title("The Enchanter")
				.desc("Levels are worthless until you have somewhere to spend them.")
				.icon(Items.ENCHANTING_TABLE)
				.guide("Table = 2 diamonds, 4 obsidian, 1 book. Exactly 15 bookshelves placed 2 blocks "
						+ "away with a 1 block air gap gives level 30 enchants.")
				.tools("Diamond Pickaxe", "Water Bucket", "45 Books")
				.task(new ItemTask("obsidian", "Collect Obsidian", 4, Items.OBSIDIAN),
						new CraftTask("table", "Craft an Enchanting Table", 1, Items.ENCHANTING_TABLE),
						new CraftTask("shelves", "Craft Bookshelves", 15, Items.BOOKSHELF),
						new StatTask("enchanted", "Enchant Items", 3, Stats.ENCHANT_ITEM)));

		addMain(Quest.builder("p2_25_the_anvil", QuestPhase.PHASE_2)
				.title("The Anvil")
				.desc("Thirty-one iron ingots so your favourite pickaxe never dies.")
				.icon(Items.ANVIL)
				.guide("3 iron blocks + 4 ingots. An anvil combines two damaged items, applies "
						+ "enchanted books, and renames things. Each use raises the prior-work penalty, "
						+ "so combine books before you put them on the tool.")
				.tools("31 Iron Ingots", "Enchanting Table")
				.task(new CraftTask("anvil", "Craft an Anvil", 1, Items.ANVIL),
						new StatTask("used_anvil", "Use an Anvil", 3, Stats.INTERACT_WITH_ANVIL)));
	}
}
