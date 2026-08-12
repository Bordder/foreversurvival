package com.foreversurvival.quest;

import static com.foreversurvival.quest.QuestRegistry.OVERWORLD;
import static com.foreversurvival.quest.QuestRegistry.addMain;

import com.foreversurvival.quest.task.CheckmarkTask;
import com.foreversurvival.quest.task.CraftTask;
import com.foreversurvival.quest.task.ItemTask;
import com.foreversurvival.quest.task.KillTask;
import com.foreversurvival.quest.task.StructureTask;

import net.minecraft.entity.EntityType;
import net.minecraft.item.Items;
import net.minecraft.world.gen.feature.StructureFeature;

/**
 * PHASE 4 - THE MASTERY (24 quests, MAJOR).
 *
 * The long middle of a survival world: every remaining structure, the farms
 * that make the game self-sustaining, and the Wither. This is the phase that
 * turns a base into an economy.
 */
final class Phase4Quests {

	private Phase4Quests() {
	}

	static void register() {
		addMain(Quest.builder("p4_01_desert_temple", QuestPhase.PHASE_4)
				.title("The Desert Temple")
				.desc("Four chests, and a pressure plate that ends the run.")
				.icon(Items.CHISELED_SANDSTONE)
				.guide("Break into the treasure room from the SIDE. The blue terracotta in the floor "
						+ "sits on a stone pressure plate wired to nine TNT - never drop into the pit.")
				.tools("Pickaxe", "Torches")
				.task(new StructureTask("temple", "Find a Desert Temple", OVERWORLD,
								StructureFeature.DESERT_PYRAMID),
						new CheckmarkTask("looted", "Loot all four temple chests without triggering the TNT")));

		addMain(Quest.builder("p4_02_jungle_temple", QuestPhase.PHASE_4)
				.title("The Jungle Temple")
				.desc("Tripwires, dispensers, and a three-lever puzzle.")
				.icon(Items.MOSSY_COBBLESTONE)
				.guide("Cut the tripwire with shears before you step over it, or the dispensers fire "
						+ "arrows. The lever puzzle behind the stairs opens the second chest.")
				.tools("Shears", "Pickaxe", "Torches")
				.task(new StructureTask("jungle", "Find a Jungle Temple", OVERWORLD,
								StructureFeature.JUNGLE_PYRAMID),
						new ItemTask("hooks", "Collect Tripwire Hooks", 2, Items.TRIPWIRE_HOOK)));

		addMain(Quest.builder("p4_03_shipwreck", QuestPhase.PHASE_4)
				.title("Buried Treasure")
				.desc("A map from a wreck, and an X that is always exactly right.")
				.icon(Items.HEART_OF_THE_SEA)
				.guide("Shipwrecks and ocean ruins hold a Buried Treasure Map. The chest is always "
						+ "directly under the X, usually a few blocks down in the sand or gravel. "
						+ "It is the ONLY source of a Heart of the Sea.")
				.tools("Boat", "Shovel", "Water Breathing Potion")
				.task(new CheckmarkTask("map_found", "Find a Buried Treasure Map"),
						new ItemTask("heart", "Dig up a Heart of the Sea", 1, Items.HEART_OF_THE_SEA)));

		addMain(Quest.builder("p4_04_ocean_monument", QuestPhase.PHASE_4)
				.title("The Ocean Monument")
				.desc("Three Elder Guardians and a debuff designed to stop you.")
				.icon(Items.PRISMARINE_BRICKS)
				.guide("Deep Ocean biomes. Kill all three Elder Guardians to stop Mining Fatigue; milk "
						+ "clears it meanwhile. Placing a door underwater makes an air pocket.")
				.tools("Water Breathing Potions", "Milk Buckets", "Doors")
				.task(new KillTask("elder", "Kill Elder Guardians", 3, EntityType.ELDER_GUARDIAN),
						new StructureTask("monument", "Find an Ocean Monument", OVERWORLD,
								StructureFeature.MONUMENT),
						new ItemTask("sponge", "Collect a Sponge", 1, Items.SPONGE, Items.WET_SPONGE)));

		addMain(Quest.builder("p4_05_conduit_power", QuestPhase.PHASE_4)
				.title("Conduit Power")
				.desc("An underwater beacon that lets you breathe forever.")
				.icon(Items.CONDUIT)
				.guide("1 Heart of the Sea + 8 nautilus shells. Activate it with 16 prismarine-family "
						+ "blocks in a frame; 42 blocks gives the full 96 block range.")
				.tools("Heart of the Sea", "Nautilus Shells", "Prismarine")
				.task(new ItemTask("shells", "Collect Nautilus Shells", 8, Items.NAUTILUS_SHELL),
						new CraftTask("conduit", "Craft a Conduit", 1, Items.CONDUIT),
						new CheckmarkTask("activated", "Activate a Conduit underwater")));

		addMain(Quest.builder("p4_06_guardian_farm", QuestPhase.PHASE_4)
				.title("The Guardian Farm")
				.desc("Drain the monument, and it never stops paying.")
				.icon(Items.SEA_LANTERN)
				.guide("Guardians only spawn inside the monument's original volume. Drain it with "
						+ "sponges, or build a kill chamber under the spawning platforms. This is the "
						+ "best XP and prismarine source in the game.")
				.tools("Sponges", "Building Blocks", "Water Buckets", "Conduit")
				.task(new KillTask("guardians", "Kill Guardians", 40, EntityType.GUARDIAN),
						new ItemTask("shards", "Collect Prismarine Shards", 32, Items.PRISMARINE_SHARD),
						new CheckmarkTask("farm", "Build a guardian farm or drained kill room")));

		addMain(Quest.builder("p4_07_trading_hall", QuestPhase.PHASE_4)
				.title("The Trading Hall")
				.desc("Villagers are the real economy. Build them somewhere to live.")
				.icon(Items.EMERALD_BLOCK)
				.guide("Place an unclaimed workstation next to a jobless villager to set its "
						+ "profession, and break it before the first trade to re-roll. Keep each "
						+ "villager in its own cell so they cannot swap jobs.")
				.tools("Workstations", "Beds", "Building Blocks", "Villagers")
				.task(new CheckmarkTask("hall", "Build a trading hall with at least 6 villagers"),
						new ItemTask("emeralds", "Stockpile Emeralds", 32, Items.EMERALD),
						new CraftTask("emerald_block", "Craft Emerald Blocks", 2, Items.EMERALD_BLOCK)));

		addMain(Quest.builder("p4_08_the_cure", QuestPhase.PHASE_4)
				.title("The Cure")
				.desc("A zombie villager, a splash potion, and a lifetime discount.")
				.icon(Items.GOLDEN_APPLE)
				.guide("Throw a Splash Potion of Weakness at a zombie villager, then feed it a Golden "
						+ "Apple. It converts in 3-5 minutes. A cured villager gives permanently "
						+ "reduced prices - cure it twice for the maximum discount.")
				.tools("Splash Potion of Weakness", "Golden Apples", "Iron Bars")
				.task(new ItemTask("gapple", "Craft a Golden Apple", 1, Items.GOLDEN_APPLE),
						new CheckmarkTask("found", "Find and trap a zombie villager"),
						new CheckmarkTask("cured", "Cure a zombie villager")));

		addMain(Quest.builder("p4_09_mending", QuestPhase.PHASE_4)
				.title("Mending")
				.desc("The enchantment that means you never lose a tool again.")
				.icon(Items.ENCHANTED_BOOK)
				.guide("Mending cannot be got from an enchanting table. Put a lectern next to a "
						+ "jobless villager, check the trade, and break the lectern to re-roll until "
						+ "a Librarian offers it.")
				.tools("Lectern", "Villager", "Emeralds")
				.task(new CraftTask("lectern", "Craft a Lectern", 1, Items.LECTERN),
						new ItemTask("books", "Collect Enchanted Books", 4, Items.ENCHANTED_BOOK),
						new CheckmarkTask("mending", "Obtain a Mending book")));

		addMain(Quest.builder("p4_10_storage_system", QuestPhase.PHASE_4)
				.title("The Storage System")
				.desc("The chest wall becomes a sorting machine.")
				.icon(Items.HOPPER)
				.guide("A hopper feeding a chest, with a comparator reading a filter chest, sorts "
						+ "items automatically. Every long-lived world eventually needs one - build it "
						+ "before the chest wall becomes unmanageable.")
				.tools("Hoppers", "Chests", "Comparators", "Redstone")
				.task(new CraftTask("hoppers", "Craft Hoppers", 12, Items.HOPPER),
						new CraftTask("comparators", "Craft Comparators", 4, Items.COMPARATOR),
						new CheckmarkTask("sorter", "Build a working item sorter")));

		addMain(Quest.builder("p4_11_auto_smelter", QuestPhase.PHASE_4)
				.title("The Smelter")
				.desc("A furnace array that feeds itself.")
				.icon(Items.BLAST_FURNACE)
				.guide("A hopper into the top loads material, one into the side loads fuel, and one "
						+ "underneath pulls the output. A bank of blast furnaces clears a full mining "
						+ "trip while you sleep.")
				.tools("Hoppers", "Blast Furnaces", "Chests")
				.task(new CraftTask("blast_bank", "Craft Blast Furnaces", 4, Items.BLAST_FURNACE),
						new CheckmarkTask("auto_smelt", "Build an automatic smelter with hoppers")));

		addMain(Quest.builder("p4_12_iron_economy", QuestPhase.PHASE_4)
				.title("The Iron Economy")
				.desc("Stop mining iron. Make iron come to you.")
				.icon(Items.IRON_BLOCK)
				.guide("Three villagers, three beds and a zombie they can see makes them panic and "
						+ "spawn iron golems. This is the quest that pays for everything after it.")
				.tools("Villagers", "Beds", "Building Blocks", "Water Bucket")
				.task(new CheckmarkTask("golem_farm", "Build a working iron golem farm"),
						new ItemTask("stock", "Stockpile Iron Blocks", 32, Items.IRON_BLOCK)));

		addMain(Quest.builder("p4_13_gold_farm", QuestPhase.PHASE_4)
				.title("The Gold Farm")
				.desc("Zombified piglins spawn on any nether block. Use that.")
				.icon(Items.GOLD_BLOCK)
				.guide("Build a platform in the Nether roof or over a lava sea, out of any non-nylium "
						+ "block, and let zombified piglins spawn and fall. It gives gold, XP and "
						+ "the rotten flesh for villager trades.")
				.tools("Building Blocks", "Fire Resistance", "Elytra")
				.task(new KillTask("zpigs", "Kill Zombified Piglins", 60,
								EntityType.ZOMBIFIED_PIGLIN),
						new CraftTask("gold_blocks", "Craft Gold Blocks", 8, Items.GOLD_BLOCK),
						new CheckmarkTask("gold_farm", "Build a gold or XP farm in the Nether")));

		addMain(Quest.builder("p4_14_creeper_farm", QuestPhase.PHASE_4)
				.title("The Creeper Farm")
				.desc("Gunpowder is the fuel of the late game.")
				.icon(Items.GUNPOWDER)
				.guide("Creepers need a 3 block high dark space and never spawn on slabs or glass. "
						+ "Cats scare them, so keep the farm away from villages. Gunpowder means "
						+ "rockets, TNT and splash potions forever.")
				.tools("Building Blocks", "Water Buckets", "Sword")
				.task(new KillTask("creepers", "Kill Creepers", 50, EntityType.CREEPER),
						new ItemTask("gunpowder", "Collect Gunpowder", 32, Items.GUNPOWDER),
						new CheckmarkTask("creeper_farm", "Build a mob or creeper farm")));

		addMain(Quest.builder("p4_15_blaze_farm", QuestPhase.PHASE_4)
				.title("The Blaze Farm")
				.desc("A spawner you keep instead of destroying.")
				.icon(Items.BLAZE_POWDER)
				.guide("Wall off a fortress blaze spawner, leave a 9x9x3 spawn space, and funnel them "
						+ "into a drop. Blaze powder is the base of every potion and every eye of "
						+ "ender you will ever need.")
				.tools("Fire Resistance Potions", "Building Blocks", "Hoppers")
				.task(new ItemTask("powder", "Collect Blaze Powder", 24, Items.BLAZE_POWDER),
						new CheckmarkTask("blaze_farm", "Build a blaze spawner farm")));

		addMain(Quest.builder("p4_16_slime_chunk", QuestPhase.PHASE_4)
				.title("The Slime Chunk")
				.desc("One chunk in ten, below Y=40, and no way to tell from the surface.")
				.icon(Items.SLIME_BLOCK)
				.guide("Slimes spawn below Y=40 in about 10% of chunks regardless of light, and in "
						+ "swamps between Y=50 and Y=70 at night. Slime blocks and sticky pistons are "
						+ "the basis of every flying machine.")
				.tools("Sword", "Torches", "Pickaxe")
				.task(new KillTask("slimes", "Kill Slimes", 20, EntityType.SLIME),
						new ItemTask("slimeballs", "Collect Slimeballs", 16, Items.SLIME_BALL),
						new CraftTask("sticky", "Craft Sticky Pistons", 4, Items.STICKY_PISTON)));

		addMain(Quest.builder("p4_17_witch_hut", QuestPhase.PHASE_4)
				.title("The Swamp Hut")
				.desc("One witch, one cauldron, and a spawn rate you can exploit.")
				.icon(Items.REDSTONE)
				.guide("Witches spawn inside the hut's bounding box at any light level. They drop "
						+ "redstone, glowstone, sugar, sticks and gunpowder - the single best source "
						+ "of redstone that does not involve mining.")
				.tools("Bow", "Armour", "Milk Bucket")
				.task(new StructureTask("hut", "Find a Swamp Hut", OVERWORLD,
								StructureFeature.SWAMP_HUT),
						new KillTask("witches", "Kill Witches", 15, EntityType.WITCH),
						new ItemTask("glowstone_dust", "Collect Glowstone Dust", 16,
								Items.GLOWSTONE_DUST)));

		addMain(Quest.builder("p4_18_skull_collector", QuestPhase.PHASE_4)
				.title("The Skull Collector")
				.desc("Three skulls at a 2.5% drop rate. Bring patience.")
				.icon(Items.WITHER_SKELETON_SKULL)
				.guide("They spawn in fortresses on nether brick only. Looting III raises the skull "
						+ "drop to 5.5% and roughly halves the grind. Milk clears the wither effect.")
				.tools("Looting III Sword", "Milk Bucket", "Fire Resistance Potions")
				.task(new KillTask("wskellies", "Kill Wither Skeletons", 20,
								EntityType.WITHER_SKELETON),
						new ItemTask("skulls", "Collect Wither Skeleton Skulls", 3,
								Items.WITHER_SKELETON_SKULL)));

		addMain(Quest.builder("p4_19_the_wither", QuestPhase.PHASE_4)
				.title("The Wither")
				.desc("The hardest fight in the game if you do it in the open.")
				.icon(Items.NETHER_STAR)
				.guide("4 soul sand in a T, then the 3 skulls on top LAST. Fight it in a sealed "
						+ "obsidian box. Withered targets cannot regenerate - carry milk.")
				.tools("Netherite Armour", "Enchanted Sword", "Golden Apples", "Milk Bucket", "Obsidian")
				.task(new ItemTask("soul_sand", "Collect Soul Sand", 6, Items.SOUL_SAND),
						new KillTask("wither", "Defeat the Wither", 1, EntityType.WITHER),
						new ItemTask("star", "Collect a Nether Star", 1, Items.NETHER_STAR)));

		addMain(Quest.builder("p4_20_beacon_of_hope", QuestPhase.PHASE_4)
				.title("A Beacon of Hope")
				.desc("The trophy for killing a god.")
				.icon(Items.BEACON)
				.guide("3 obsidian + 5 glass + 1 nether star. Tier 1 is a single 3x3 layer - 9 blocks - "
						+ "and already gives Speed or Haste in a 20 block radius. It needs clear sky "
						+ "above it.")
				.tools("Nether Star", "Obsidian", "Glass", "9 Metal Blocks")
				.task(new CraftTask("beacon", "Craft a Beacon", 1, Items.BEACON),
						new ItemTask("blocks", "Stockpile Metal Blocks", 9,
								Items.IRON_BLOCK, Items.GOLD_BLOCK),
						new CheckmarkTask("tier1", "Activate a beacon with an effect selected")));

		addMain(Quest.builder("p4_21_full_pyramid", QuestPhase.PHASE_4)
				.title("The Full Pyramid")
				.desc("164 blocks of metal for a permanent buff.")
				.icon(Items.IRON_BLOCK)
				.guide("Tier 4 is layers of 9, 25, 49 and 81. It extends the range to 50 blocks and "
						+ "unlocks the second effect slot - Haste II halves every mining trip you take "
						+ "after this.")
				.tools("164 Metal Blocks", "Beacon", "Iron Farm")
				.task(new CheckmarkTask("pyramid", "Complete a full Tier 4 beacon pyramid"),
						new CheckmarkTask("haste", "Run Haste II from the beacon")));

		addMain(Quest.builder("p4_22_woodland_mansion", QuestPhase.PHASE_4)
				.title("The Woodland Mansion")
				.desc("Tens of thousands of blocks away, and full of things that hurt.")
				.icon(Items.ENCHANTED_GOLDEN_APPLE)
				.guide("Buy a Woodland Explorer Map from a Cartographer - finding one by hand can take "
						+ "a real day. Vindicators hit for 13 and Evokers summon vexes that pass "
						+ "through walls. Fight in doorways.")
				.tools("Woodland Explorer Map", "Netherite Armour", "Golden Apples", "Elytra")
				.task(new KillTask("vindicators", "Kill Vindicators", 10, EntityType.VINDICATOR),
						new ItemTask("notch", "Loot an Enchanted Golden Apple", 1,
								Items.ENCHANTED_GOLDEN_APPLE),
						new CheckmarkTask("mansion", "Clear a Woodland Mansion")));

		addMain(Quest.builder("p4_23_raid_breaker", QuestPhase.PHASE_4)
				.title("The Raid Breaker")
				.desc("Kill a captain, then deal with what shows up.")
				.icon(Items.TOTEM_OF_UNDYING)
				.guide("Kill the pillager with the banner on its head for Bad Omen, then enter a "
						+ "village. Evokers drop the totem - hold it in your OFF-HAND.")
				.tools("Netherite Armour", "Enchanted Sword", "Shield", "Golden Apples")
				.task(new KillTask("pillagers", "Kill Pillagers", 20, EntityType.PILLAGER),
						new KillTask("ravagers", "Kill Ravagers", 2, EntityType.RAVAGER),
						new KillTask("evokers", "Kill Evokers", 2, EntityType.EVOKER),
						new ItemTask("totem", "Collect a Totem of Undying", 1,
								Items.TOTEM_OF_UNDYING)));

		addMain(Quest.builder("p4_24_full_diamond_enchant", QuestPhase.PHASE_4)
				.title("Kitted Out")
				.desc("Full armour, fully enchanted, before you go anywhere near the End.")
				.icon(Items.DIAMOND_CHESTPLATE)
				.guide("Protection IV on all four pieces is 80% damage reduction, the vanilla cap. "
						+ "Combine books on the anvil BEFORE applying them to keep the prior-work "
						+ "penalty down.")
				.tools("Enchanting Table", "Anvil", "Emeralds", "XP Farm")
				.task(new CraftTask("d_helm", "Craft a Diamond Helmet", 1, Items.DIAMOND_HELMET),
						new CraftTask("d_chest", "Craft a Diamond Chestplate", 1,
								Items.DIAMOND_CHESTPLATE),
						new CraftTask("d_legs", "Craft Diamond Leggings", 1, Items.DIAMOND_LEGGINGS),
						new CraftTask("d_boots", "Craft Diamond Boots", 1, Items.DIAMOND_BOOTS),
						new CheckmarkTask("prot4", "Get Protection IV on every armour piece"),
						new CheckmarkTask("mending_gear", "Put Mending on your sword and pickaxe")));
	}
}
