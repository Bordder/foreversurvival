package com.foreversurvival.quest;

import static com.foreversurvival.quest.QuestRegistry.BOATS;
import static com.foreversurvival.quest.QuestRegistry.MUSIC_DISCS;
import static com.foreversurvival.quest.QuestRegistry.addMain;

import com.foreversurvival.quest.task.CheckmarkTask;
import com.foreversurvival.quest.task.CraftTask;
import com.foreversurvival.quest.task.EnchantTask;
import com.foreversurvival.quest.task.ItemTask;
import com.foreversurvival.quest.task.KillTask;
import com.foreversurvival.quest.task.PositionTask;
import com.foreversurvival.quest.task.StatTask;

import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.stats.Stats;

/**
 * PHASE 6 - THE FOREVER GOALS (20 quests, MAJOR).
 *
 * The long tail: full automation, complete collections, and the building
 * projects that outlast everything else. These are the goals experienced
 * players actually set themselves once the game stops threatening them.
 */
final class Phase6Quests {

	private Phase6Quests() {
	}

	static void register() {
		addMain(Quest.builder("p6_01_master_enchanter", QuestPhase.PHASE_6)
				.title("The Master Enchanter")
				.desc("Every enchantment in the game, on the gear that deserves it.")
				.icon(Items.ENCHANTED_BOOK)
				.guide("Silk Touch and Fortune III cannot go on the same pickaxe, so you need two. "
						+ "Librarian trades are cheaper than the table once you have an emerald farm.")
				.tools("Trading Hall", "Anvil", "XP Farm")
				.task(new ItemTask("books", "Collect Enchanted Books", 20, Items.ENCHANTED_BOOK),
						new EnchantTask("fortune", "Carry a Fortune III tool", 1, 3, Enchantments.FORTUNE),
						new EnchantTask("silk", "Carry a Silk Touch tool", 1, 1, Enchantments.SILK_TOUCH),
						new EnchantTask("looting", "Carry a Looting III sword", 1, 3, Enchantments.LOOTING),
						new EnchantTask("efficiency", "Carry an Efficiency V tool", 1, 5,
								Enchantments.EFFICIENCY)));

		addMain(Quest.builder("p6_02_the_alchemist", QuestPhase.PHASE_6)
				.title("The Alchemist")
				.desc("Every potion in the book, brewed at least once.")
				.icon(Items.BREWING_STAND)
				.guide("Redstone extends duration, glowstone strengthens, gunpowder makes it a splash "
						+ "potion, dragon's breath makes it lingering, and a fermented spider eye "
						+ "inverts the effect.")
				.tools("Brewing Stand", "Nether Wart Farm", "Blaze Powder")
				.task(new ItemTask("potions", "Carry Brewed Potions", 8, Items.POTION),
						new ItemTask("splash", "Carry Splash Potions", 2, Items.SPLASH_POTION),
						new ItemTask("lingering", "Carry a Lingering Potion", 1, Items.LINGERING_POTION),
						new CraftTask("tipped", "Craft Tipped Arrows", 8, Items.TIPPED_ARROW),
						new CheckmarkTask("all_potions", "Brew every base potion type at least once")));

		addMain(Quest.builder("p6_03_master_farmer", QuestPhase.PHASE_6)
				.title("The Master Farmer")
				.desc("Stop gathering. Start receiving.")
				.icon(Items.HOPPER)
				.guide("Hoppers into chests, and keep each farm more than 128 blocks from the others "
						+ "so they do not fight over the mob cap. A world with these five running is a "
						+ "world where you only build.")
				.tools("Hoppers", "Redstone", "Building Blocks", "Water Buckets")
				.task(new CheckmarkTask("crop_farm", "Build an automatic crop farm"),
						new CheckmarkTask("tree_farm", "Build a tree or bamboo farm"),
						new CheckmarkTask("food_farm", "Build an automatic food farm"),
						new CheckmarkTask("sugar_farm", "Build an automatic sugar cane farm"),
						new CheckmarkTask("villager_farm", "Build a villager breeder")));

		addMain(Quest.builder("p6_04_nether_star_farm", QuestPhase.PHASE_6)
				.title("The Star Foundry")
				.desc("Wither skulls on tap, and beacons whenever you want one.")
				.icon(Items.NETHER_STAR)
				.guide("A wither skeleton farm built around fortress spawn platforms turns a 2.5% "
						+ "drop into a steady supply. Five stars is five beacons, which is every "
						+ "effect at once across a base.")
				.tools("Looting III Sword", "Building Blocks", "Fire Resistance")
				.task(new CheckmarkTask("skele_farm", "Build a wither skeleton farm"),
						new ItemTask("stars", "Collect Nether Stars", 5, Items.NETHER_STAR),
						new KillTask("withers", "Defeat the Wither (total)", 5, EntityType.WITHER)));

		addMain(Quest.builder("p6_05_beacon_array", QuestPhase.PHASE_6)
				.title("The Beacon Array")
				.desc("Every effect, everywhere, permanently.")
				.icon(Items.BEACON)
				.guide("Five Tier 4 beacons cover Speed, Haste, Resistance, Jump Boost and Strength "
						+ "at once. That is 820 metal blocks - only reachable with an iron farm and a "
						+ "gold farm both running.")
				.tools("Nether Stars", "820 Metal Blocks", "Iron Farm")
				.task(new CraftTask("beacons", "Craft Beacons", 4, Items.BEACON),
						new CheckmarkTask("array", "Run four or more beacons over one base")));

		addMain(Quest.builder("p6_06_the_menagerie", QuestPhase.PHASE_6)
				.title("The Menagerie")
				.desc("One of everything, alive and yours.")
				.icon(Items.LEAD)
				.guide("Leads and boats move most animals; a nether portal moves the rest. Name tags "
						+ "stop them despawning. Axolotls, goats and glow squid are the three the game "
						+ "makes hardest to keep.")
				.tools("Leads", "Boats", "Name Tags", "Buckets")
				.task(new CraftTask("leads", "Craft Leads", 4, Items.LEAD),
						new CheckmarkTask("farm_animals", "Pen every farm animal"),
						new CheckmarkTask("rare_mobs", "Collect an axolotl, a goat and a glow squid"),
						new CheckmarkTask("tamed", "Tame a wolf, cat, horse and parrot"),
						new CheckmarkTask("two_by_two", "Breed every breedable mob in the game")));

		addMain(Quest.builder("p6_07_cartographer", QuestPhase.PHASE_6)
				.title("The Cartographer")
				.desc("Every biome in the world, seen with your own eyes.")
				.icon(Items.FILLED_MAP)
				.guide("Around 60 biomes across the three dimensions. The underground ones - Lush "
						+ "Caves and Dripstone Caves - are the two people forget. Confirm each set "
						+ "yourself when you have genuinely stood in all of them.")
				.tools("Boat", "Horse", "Elytra", "Maps")
				.task(new CraftTask("boat", "Craft a Boat", 1, BOATS),
						new CheckmarkTask("overworld_biomes", "Visit every Overworld surface biome"),
						new CheckmarkTask("cave_biomes", "Visit Lush Caves and Dripstone Caves"),
						new CheckmarkTask("nether_biomes", "Visit all five Nether biomes"),
						new CheckmarkTask("end_biomes", "Visit the End's outer islands")));

		addMain(Quest.builder("p6_08_map_wall", QuestPhase.PHASE_6)
				.title("The Map Wall")
				.desc("Your world, on a wall, at 1:1.")
				.icon(Items.ITEM_FRAME)
				.guide("A fully zoomed-out map covers 2048 blocks. Nine of them in a 3x3 of item "
						+ "frames makes a wall map that keeps updating as you fly over the terrain.")
				.tools("Paper", "Cartography Table", "Item Frames")
				.task(new CraftTask("frames", "Craft Item Frames", 9, Items.ITEM_FRAME),
						new ItemTask("maps", "Carry Filled Maps", 9, Items.FILLED_MAP),
						new CheckmarkTask("wall", "Build a 3x3 or larger map wall")));

		addMain(Quest.builder("p6_09_trophy_hunter", QuestPhase.PHASE_6)
				.title("The Trophy Hunter")
				.desc("One of every head in the game.")
				.icon(Items.CREEPER_HEAD)
				.guide("Zombie, skeleton and creeper heads only drop when a CHARGED creeper kills that "
						+ "mob. Charge one with lightning during a thunderstorm, or with a Channeling "
						+ "trident. The dragon head is on the bow of an End Ship.")
				.tools("Channeling Trident or a Thunderstorm", "Elytra")
				.task(new ItemTask("zombie_head", "Obtain a Zombie Head", 1, Items.ZOMBIE_HEAD),
						new ItemTask("skele_skull", "Obtain a Skeleton Skull", 1, Items.SKELETON_SKULL),
						new ItemTask("creeper_head", "Obtain a Creeper Head", 1, Items.CREEPER_HEAD),
						new ItemTask("wither_skull", "Obtain a Wither Skeleton Skull", 1,
								Items.WITHER_SKELETON_SKULL),
						new ItemTask("dragon_head", "Obtain a Dragon Head", 1, Items.DRAGON_HEAD)));

		addMain(Quest.builder("p6_10_collector", QuestPhase.PHASE_6)
				.title("The Collector")
				.desc("Fourteen discs, none of which drop the way you want.")
				.icon(Items.JUKEBOX)
				.guide("A skeleton killing a creeper drops one of twelve discs - build a farm where "
						+ "they can shoot each other. Pigstep is bastion loot only; Otherside is "
						+ "dungeon and stronghold loot at a very low rate.")
				.tools("Bow", "Mob Farm", "Elytra")
				.task(new CraftTask("jukebox", "Craft a Jukebox", 1, Items.JUKEBOX),
						new ItemTask("discs", "Collect Music Discs", 10, MUSIC_DISCS)));

		addMain(Quest.builder("p6_11_balanced_diet", QuestPhase.PHASE_6)
				.title("A Balanced Diet")
				.desc("Everything edible in the game, eaten once.")
				.icon(Items.GOLDEN_CARROT)
				.guide("The awkward ones are the Enchanted Golden Apple, the Pufferfish, the Spider "
						+ "Eye, Rotten Flesh and a slice of Cake placed and eaten. Chorus fruit counts "
						+ "and will teleport you mid-meal.")
				.tools("A full pantry", "Patience")
				.task(new CraftTask("golden_carrot", "Craft a Golden Carrot", 1, Items.GOLDEN_CARROT),
						new CraftTask("glistering", "Craft a Glistering Melon Slice", 1,
								Items.GLISTERING_MELON_SLICE),
						new CheckmarkTask("diet", "Eat every food item in the game")));

		addMain(Quest.builder("p6_12_the_great_build", QuestPhase.PHASE_6)
				.title("The Great Build")
				.desc("At some point the game stops being about survival.")
				.icon(Items.SCAFFOLDING)
				.guide("Nothing can judge a build, so this is on your honour. Scaffolding, Elytra and "
						+ "a Haste II beacon are what make large projects bearable.")
				.tools("Beacon", "Elytra", "Scaffolding", "Shulker Boxes")
				.task(new CraftTask("scaffold", "Craft Scaffolding", 32, Items.SCAFFOLDING),
						new CheckmarkTask("mega_build", "Finish a build that took more than one session"),
						new CheckmarkTask("road", "Connect two distant bases with a permanent route"),
						new CheckmarkTask("monument", "Build a monument or statue")));

		addMain(Quest.builder("p6_13_nether_hub", QuestPhase.PHASE_6)
				.title("The Hub")
				.desc("Every base you own, five hundred blocks apart.")
				.icon(Items.LODESTONE)
				.guide("Divide Overworld coordinates by 8 for the linked Nether portal. A proper hub "
						+ "has a lit, walled tunnel to every destination and a sign at every junction. "
						+ "This is the single most useful build in a long world.")
				.tools("Obsidian", "Building Blocks", "Signs", "Pickaxe")
				.task(new CheckmarkTask("hub", "Build a central Nether hub"),
						new CheckmarkTask("four_portals", "Link at least four destinations to it"),
						new CheckmarkTask("lit", "Light and wall every tunnel")));

		addMain(Quest.builder("p6_14_the_archivist", QuestPhase.PHASE_6)
				.title("The Archivist")
				.desc("One of every wood, one of every stone.")
				.icon(Items.BARREL)
				.guide("Six overworld woods plus crimson and warped. Stone, deepslate, andesite, "
						+ "diorite, granite, tuff, calcite, basalt, blackstone and end stone. A room "
						+ "with one of each is how you plan every build afterwards.")
				.tools("Silk Touch Pickaxe", "Shulker Boxes", "Elytra")
				.task(new CheckmarkTask("woods", "Collect all eight wood types"),
						new CheckmarkTask("stones", "Collect every stone variant"),
						new CheckmarkTask("archive", "Build a materials archive room")));

		addMain(Quest.builder("p6_15_ocean_explorer", QuestPhase.PHASE_6)
				.title("The Ocean Explorer")
				.desc("The last parts of the map nobody bothers with.")
				.icon(Items.TRIDENT)
				.guide("Tridents only drop from drowned that spawn holding one - about 6.25% in ocean "
						+ "biomes, never from converted zombies. Channeling plus a thunderstorm is how "
						+ "you make charged creepers.")
				.tools("Water Breathing", "Depth Strider", "Conduit")
				.task(new KillTask("drowned", "Kill Drowned", 40, EntityType.DROWNED),
						new ItemTask("trident", "Obtain a Trident", 1, Items.TRIDENT),
						new EnchantTask("channeling", "Carry a Channeling trident", 1, 1,
								Enchantments.CHANNELING),
						new CheckmarkTask("ruins", "Explore an ocean ruin and a shipwreck")));

		addMain(Quest.builder("p6_16_the_completionist", QuestPhase.PHASE_6)
				.title("The Completionist")
				.desc("The game already keeps a list. Finish it.")
				.icon(Items.EXPERIENCE_BOTTLE)
				.guide("Open the vanilla advancements screen with L. Adventure and Husbandry take the "
						+ "longest - 'Two by Two', 'A Balanced Diet' and 'Monster Hunter' especially.")
				.tools("Everything you have")
				.task(new CheckmarkTask("story", "Complete the Story advancement tab"),
						new CheckmarkTask("nether_tab", "Complete the Nether advancement tab"),
						new CheckmarkTask("end_tab", "Complete the End advancement tab"),
						new CheckmarkTask("adventure", "Complete the Adventure advancement tab"),
						new CheckmarkTask("husbandry", "Complete the Husbandry advancement tab")));

		addMain(Quest.builder("p6_17_netherite_block", QuestPhase.PHASE_6)
				.title("The Vault")
				.desc("Nine ingots in a block, and nothing left to spend them on.")
				.icon(Items.NETHERITE_BLOCK)
				.guide("A netherite block is 9 ingots, which is 36 ancient debris and 36 gold. It is "
						+ "blast-proof and does not burn, so it is the only truly permanent decorative "
						+ "block in the game.")
				.tools("Ancient Debris", "Gold", "Haste II Beacon")
				.task(new CraftTask("n_block", "Craft a Netherite Block", 1, Items.NETHERITE_BLOCK),
						new CheckmarkTask("vault", "Build a secure vault room")));

		addMain(Quest.builder("p6_18_spawn_chunks", QuestPhase.PHASE_6)
				.title("The Spawn Chunks")
				.desc("The one place in the world that never stops ticking.")
				.icon(Items.COMPASS)
				.guide("A 16x16 chunk area around world spawn stays loaded whenever anyone is online. "
						+ "Farms and furnace arrays built there keep running while you are 10,000 "
						+ "blocks away. Follow a compass to find it.")
				.tools("Compass", "Building Blocks", "Redstone")
				.task(new CheckmarkTask("found_spawn", "Find your world spawn with a compass"),
						new CheckmarkTask("spawn_build", "Build something useful in the spawn chunks")));

		addMain(Quest.builder("p6_19_the_long_haul", QuestPhase.PHASE_6)
				.title("The Long Haul")
				.desc("Far enough out that the map means nothing.")
				.icon(Items.ELYTRA)
				.guide("A Nether highway covers 8 Overworld blocks per block travelled. 100,000 blocks "
						+ "out is 12,500 in the Nether - roughly an hour of rocket flight along a "
						+ "prepared tunnel.")
				.tools("Elytra", "Rockets", "Nether Highway", "Shulker Boxes")
				.task(new PositionTask("far_out", "Travel 50,000 blocks from spawn",
								PositionTask.Kind.FROM_SPAWN, 50000),
						new CheckmarkTask("outpost", "Build an outpost there and get home alive")));

		addMain(Quest.builder("p6_20_forever_survivor", QuestPhase.PHASE_6)
				.title("Forever Survivor")
				.desc("The last quest. Nothing after this but the world you built.")
				.icon(Items.NETHER_STAR)
				.guide("Look at the Deaths tab, look at how long the list is, and decide whether you "
						+ "are done. Confirm the last objective when you are.")
				.tools("Everything you have")
				.task(new StatTask("hundred_days", "Play 33+ hours in this world (~100 days)",
								2_400_000, Stats.PLAY_TIME),
						new CheckmarkTask("forever", "Declare this world finished")));
	}
}
