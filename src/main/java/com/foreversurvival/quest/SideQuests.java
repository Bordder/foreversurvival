package com.foreversurvival.quest;

import static com.foreversurvival.quest.QuestRegistry.OVERWORLD;
import static com.foreversurvival.quest.QuestRegistry.RAW_FISH;
import static com.foreversurvival.quest.QuestRegistry.SAPLINGS;
import static com.foreversurvival.quest.QuestRegistry.addSide;

import com.foreversurvival.quest.task.CheckmarkTask;
import com.foreversurvival.quest.task.CraftTask;
import com.foreversurvival.quest.task.EnchantTask;
import com.foreversurvival.quest.task.ItemTask;
import com.foreversurvival.quest.task.KillTask;
import com.foreversurvival.quest.task.PositionTask;
import com.foreversurvival.quest.task.StatTask;
import com.foreversurvival.quest.task.StructureTask;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.stats.Stats;
import net.minecraft.world.gen.feature.StructureFeature;

/**
 * SIDE CHALLENGES (30) - optional, unlocked from the start, never blocking.
 *
 * These are the goals players set themselves alongside the main line: complete
 * collections, the structures that are not on the critical path, and the
 * self-imposed challenges.
 */
final class SideQuests {

	private SideQuests() {
	}

	static void register() {
		addSide(Quest.builder("s01_rainbow", QuestPhase.SIDE)
				.title("Somewhere Over the Rainbow")
				.desc("All sixteen colours of wool.")
				.icon(Items.MAGENTA_WOOL)
				.guide("Dye a sheep and shear it repeatedly instead of killing it. Blue dye needs "
						+ "lapis, green needs smelted cactus, and cyan is blue plus green.")
				.tools("Shears", "Dyes", "Sheep Pen")
				.task(new ItemTask("white", "White Wool", 1, Items.WHITE_WOOL),
						new ItemTask("orange", "Orange Wool", 1, Items.ORANGE_WOOL),
						new ItemTask("magenta", "Magenta Wool", 1, Items.MAGENTA_WOOL),
						new ItemTask("light_blue", "Light Blue Wool", 1, Items.LIGHT_BLUE_WOOL),
						new ItemTask("yellow", "Yellow Wool", 1, Items.YELLOW_WOOL),
						new ItemTask("lime", "Lime Wool", 1, Items.LIME_WOOL),
						new ItemTask("pink", "Pink Wool", 1, Items.PINK_WOOL),
						new ItemTask("gray", "Gray Wool", 1, Items.GRAY_WOOL),
						new ItemTask("light_gray", "Light Gray Wool", 1, Items.LIGHT_GRAY_WOOL),
						new ItemTask("cyan", "Cyan Wool", 1, Items.CYAN_WOOL),
						new ItemTask("purple", "Purple Wool", 1, Items.PURPLE_WOOL),
						new ItemTask("blue", "Blue Wool", 1, Items.BLUE_WOOL),
						new ItemTask("brown", "Brown Wool", 1, Items.BROWN_WOOL),
						new ItemTask("green", "Green Wool", 1, Items.GREEN_WOOL),
						new ItemTask("red", "Red Wool", 1, Items.RED_WOOL),
						new ItemTask("black", "Black Wool", 1, Items.BLACK_WOOL)));

		addSide(Quest.builder("s02_home_in_every_biome", QuestPhase.SIDE)
				.title("Home Sweet Biome")
				.desc("A house in every biome you can stand in.")
				.icon(Items.OAK_DOOR)
				.guide("Ten distinct biomes, each house built from materials native to it. "
						+ "Confirm it yourself once they are standing and lit.")
				.tools("Building Blocks", "Boat", "Elytra")
				.task(new CheckmarkTask("ten_houses", "Build a proper house in ten different biomes")));

		addSide(Quest.builder("s03_wooden_warrior", QuestPhase.SIDE)
				.title("The Wooden Warrior")
				.desc("A raid, with nothing but wood between you and a Ravager.")
				.icon(Items.WOODEN_SWORD)
				.guide("Clear a full raid using only a wooden sword and no armour. A ravager hits for "
						+ "12 damage. Use doorways and keep the shield up.")
				.tools("Wooden Sword", "Shield", "Nerve")
				.task(new CraftTask("wood_sword", "Craft a Wooden Sword", 1, Items.WOODEN_SWORD),
						new StatTask("raid", "Win a Raid", 1, Stats.RAID_WIN),
						new CheckmarkTask("wooden_only",
								"...using only a wooden sword and no armour")));

		addSide(Quest.builder("s04_speleologist", QuestPhase.SIDE)
				.title("The Speleologist")
				.desc("Amethyst is the prettiest thing underground.")
				.icon(Items.AMETHYST_SHARD)
				.guide("Geodes sit between Y=-64 and Y=30; the calcite shell is the giveaway. Only "
						+ "fully grown clusters drop 4 shards. Never mine budding amethyst.")
				.tools("Any Pickaxe", "Torches")
				.task(new ItemTask("shards", "Collect Amethyst Shards", 8, Items.AMETHYST_SHARD),
						new CraftTask("spyglass", "Craft a Spyglass", 1, Items.SPYGLASS),
						new CraftTask("tinted", "Craft Tinted Glass", 2, Items.TINTED_GLASS),
						new StructureTask("geode", "Stand inside an Amethyst Geode", null,
								Blocks.AMETHYST_BLOCK, Blocks.CALCITE)));

		addSide(Quest.builder("s05_lush_caves", QuestPhase.SIDE)
				.title("The Garden Below")
				.desc("The prettiest biome in the game is underground.")
				.icon(Items.GLOW_BERRIES)
				.guide("Find an azalea tree on the surface and dig under it - its roots point at a "
						+ "Lush Cave. Glow berries light the way and replant themselves.")
				.tools("Any Pickaxe", "Water Bucket", "Shears")
				.task(new StructureTask("lush", "Stand in a Lush Cave", OVERWORLD,
								Blocks.MOSS_BLOCK, Blocks.CAVE_VINES),
						new ItemTask("berries", "Collect Glow Berries", 8, Items.GLOW_BERRIES),
						new ItemTask("moss", "Collect Moss Blocks", 8, Items.MOSS_BLOCK),
						new ItemTask("dripleaf", "Collect a Big Dripleaf", 1, Items.BIG_DRIPLEAF)));

		addSide(Quest.builder("s06_dripstone", QuestPhase.SIDE)
				.title("The Dripstone Caves")
				.desc("Pointy, and it will drop on your head.")
				.icon(Items.POINTED_DRIPSTONE)
				.guide("A stalactite with a water source above it drips into a cauldron and fills it "
						+ "for free. With lava above instead, it makes infinite lava.")
				.tools("Any Pickaxe", "Cauldron")
				.task(new StructureTask("dripstone", "Stand in a Dripstone Cave", OVERWORLD,
								Blocks.DRIPSTONE_BLOCK, Blocks.POINTED_DRIPSTONE),
						new ItemTask("pointed", "Collect Pointed Dripstone", 8, Items.POINTED_DRIPSTONE),
						new CraftTask("cauldron", "Craft a Cauldron", 1, Items.CAULDRON)));

		addSide(Quest.builder("s07_mountaineer", QuestPhase.SIDE)
				.title("The Mountaineer")
				.desc("From bedrock to build limit, 384 blocks straight up.")
				.icon(Items.POWDER_SNOW_BUCKET)
				.guide("Goats ram you off ledges, and powder snow freezes you unless you are wearing "
						+ "leather boots. Powder snow can only be picked up with a bucket, and it "
						+ "cancels all fall damage.")
				.tools("Leather Boots", "Bucket", "Building Blocks")
				.task(new CheckmarkTask("goats", "Get rammed off a ledge by a goat"),
						new ItemTask("powder", "Collect a Powder Snow Bucket", 1,
								Items.POWDER_SNOW_BUCKET),
						new PositionTask("summit", "Reach Y=250 on a mountain peak",
								PositionTask.Kind.ABOVE_Y, 250)));

		addSide(Quest.builder("s08_zoologist", QuestPhase.SIDE)
				.title("The Zoologist")
				.desc("Tame one of everything that will have you.")
				.icon(Items.BONE)
				.guide("Wolves take bones, cats take raw fish, horses take repeated mounting, parrots "
						+ "take seeds. Axolotls are scooped with a bucket in Lush Caves.")
				.tools("Bones", "Raw Fish", "Saddle", "Seeds", "Bucket")
				.task(new CheckmarkTask("wolf", "Tame a Wolf"),
						new CheckmarkTask("cat", "Tame a Cat"),
						new CheckmarkTask("horse", "Tame and saddle a Horse"),
						new CheckmarkTask("parrot", "Tame a Parrot"),
						new ItemTask("axolotl", "Bucket an Axolotl", 1, Items.AXOLOTL_BUCKET)));

		addSide(Quest.builder("s09_angler", QuestPhase.SIDE)
				.title("The Angler")
				.desc("Slow, boring, quietly one of the best loot sources in the game.")
				.icon(Items.FISHING_ROD)
				.guide("Treasure only rolls in open water - a 5x4x5 area of water around the bobber. "
						+ "Luck of the Sea III shifts the table toward treasure.")
				.tools("Fishing Rod", "Luck of the Sea III", "Open Water")
				.task(new ItemTask("fish", "Catch Fish", 24, RAW_FISH),
						new ItemTask("nametag", "Fish up a Name Tag", 1, Items.NAME_TAG),
						new ItemTask("saddle", "Fish up a Saddle", 1, Items.SADDLE)));

		addSide(Quest.builder("s10_botanist", QuestPhase.SIDE)
				.title("The Botanist")
				.desc("One of every sapling.")
				.icon(Items.OAK_SAPLING)
				.guide("Dark oak only grows from a 2x2 of four saplings. Azalea bushes grow into "
						+ "azalea trees.")
				.tools("Bone Meal", "Any Axe")
				.task(new ItemTask("all_saplings", "Collect every Sapling type", 6, SAPLINGS),
						new ItemTask("azalea", "Collect an Azalea", 1,
								Items.AZALEA, Items.FLOWERING_AZALEA)));

		addSide(Quest.builder("s11_flower_child", QuestPhase.SIDE)
				.title("The Flower Child")
				.desc("Every flower, and the dyes they make.")
				.icon(Items.SUNFLOWER)
				.guide("Flower Forests hold nearly every small flower. The two-block flowers - "
						+ "sunflower, lilac, rose bush, peony - only come from specific biomes and "
						+ "each makes two dye.")
				.tools("Shears", "Bone Meal")
				.task(new ItemTask("sunflower", "Collect a Sunflower", 1, Items.SUNFLOWER),
						new ItemTask("lilac", "Collect a Lilac", 1, Items.LILAC),
						new ItemTask("rose", "Collect a Rose Bush", 1, Items.ROSE_BUSH),
						new ItemTask("peony", "Collect a Peony", 1, Items.PEONY),
						new CraftTask("dyes", "Craft Dyes", 16,
								Items.RED_DYE, Items.YELLOW_DYE, Items.BLUE_DYE, Items.PINK_DYE)));

		addSide(Quest.builder("s12_nether_botanist", QuestPhase.SIDE)
				.title("The Nether Botanist")
				.desc("Things grow down here too, in their own way.")
				.icon(Items.WARPED_FUNGUS)
				.guide("Bone meal a fungus on its matching nylium to grow a huge fungus tree. "
						+ "Warped and crimson stems are the only fireproof wood in the game.")
				.tools("Bone Meal", "Any Axe", "Gold Armour")
				.task(new ItemTask("crimson", "Collect Crimson Fungus", 4, Items.CRIMSON_FUNGUS),
						new ItemTask("warped", "Collect Warped Fungus", 4, Items.WARPED_FUNGUS),
						new ItemTask("wart_block", "Collect Nether Wart Blocks", 8,
								Items.NETHER_WART_BLOCK),
						new CraftTask("planks", "Craft Nether Planks", 16,
								Items.CRIMSON_PLANKS, Items.WARPED_PLANKS)));

		addSide(Quest.builder("s13_exterminator", QuestPhase.SIDE)
				.title("The Exterminator")
				.desc("A long, unglamorous body count.")
				.icon(Items.IRON_SWORD)
				.guide("A dark-room farm above an ocean or high in the sky is the only sane way to "
						+ "reach these numbers.")
				.tools("Enchanted Sword", "Mob Farm")
				.task(new KillTask("zombies", "Kill Zombies", 100, EntityType.ZOMBIE),
						new KillTask("skeletons", "Kill Skeletons", 100, EntityType.SKELETON),
						new KillTask("spiders", "Kill Spiders", 60, EntityType.SPIDER),
						new KillTask("creepers", "Kill Creepers", 40, EntityType.CREEPER),
						new KillTask("phantoms", "Kill Phantoms", 10, EntityType.PHANTOM)));

		addSide(Quest.builder("s14_beekeeper", QuestPhase.SIDE)
				.title("The Beekeeper")
				.desc("The only provoke-only mob you actively want nearby.")
				.icon(Items.HONEYCOMB)
				.guide("Nests spawn in Plains, Flower Forests and Birch Forests. Put a campfire "
						+ "underneath before harvesting or the bees turn on you.")
				.tools("Shears", "Campfire", "Glass Bottles")
				.task(new ItemTask("honeycomb", "Collect Honeycomb", 8, Items.HONEYCOMB),
						new ItemTask("honey", "Collect Honey Bottles", 4, Items.HONEY_BOTTLE),
						new CraftTask("hive", "Craft a Beehive", 1, Items.BEEHIVE)));

		addSide(Quest.builder("s15_confectioner", QuestPhase.SIDE)
				.title("The Confectioner")
				.desc("A cake for no reason at all.")
				.icon(Items.CAKE)
				.guide("A cake consumes 3 milk buckets, so you need three. Sugar cane grows on sand or "
						+ "dirt next to water at any light level.")
				.tools("3 Buckets", "Sugar Cane", "Chickens", "Cows")
				.task(new ItemTask("sugar", "Collect Sugar", 16, Items.SUGAR),
						new CraftTask("cake", "Bake a Cake", 1, Items.CAKE),
						new CraftTask("cookies", "Bake Cookies", 16, Items.COOKIE)));

		addSide(Quest.builder("s16_turtle_master", QuestPhase.SIDE)
				.title("The Turtle Master")
				.desc("Five scutes, and a helmet nobody else has.")
				.icon(Items.TURTLE_HELMET)
				.guide("Feed two turtles seagrass to breed them, and one lays eggs on the beach it "
						+ "hatched on. Baby turtles drop a scute when they grow up - five makes the "
						+ "helmet, which gives 10 seconds of water breathing.")
				.tools("Seagrass", "Shears", "Patience")
				.task(new ItemTask("scute", "Collect Scutes", 5, Items.SCUTE),
						new CraftTask("helmet", "Craft a Turtle Shell helmet", 1, Items.TURTLE_HELMET),
						new CheckmarkTask("hatched", "Hatch a turtle egg")));

		addSide(Quest.builder("s17_deep_diver", QuestPhase.SIDE)
				.title("The Deep Diver")
				.desc("Everything the ocean floor is hiding.")
				.icon(Items.TRIDENT)
				.guide("Tridents only drop from drowned that spawn holding one, about 6.25% in ocean "
						+ "biomes. Converted zombies never have them.")
				.tools("Water Breathing Potions", "Depth Strider Boots")
				.task(new KillTask("drowned", "Kill Drowned", 30, EntityType.DROWNED),
						new ItemTask("trident", "Obtain a Trident", 1, Items.TRIDENT),
						new ItemTask("pickle", "Collect Sea Pickles", 8, Items.SEA_PICKLE)));

		addSide(Quest.builder("s18_frozen", QuestPhase.SIDE)
				.title("Frozen")
				.desc("The cold biomes nobody visits on purpose.")
				.icon(Items.BLUE_ICE)
				.guide("Blue ice is the fastest surface in the game for boats - faster than an Elytra "
						+ "over short distances. Strays only spawn in Snowy Plains and shoot Slowness "
						+ "arrows.")
				.tools("Silk Touch Pickaxe", "Bucket", "Boat")
				.task(new KillTask("strays", "Kill Strays", 10, EntityType.STRAY),
						new ItemTask("blue_ice", "Collect Blue Ice", 4, Items.BLUE_ICE),
						new ItemTask("packed", "Collect Packed Ice", 8, Items.PACKED_ICE)));

		addSide(Quest.builder("s19_igloo", QuestPhase.SIDE)
				.title("The Igloo")
				.desc("A rug, a trapdoor, and a basement nobody expects.")
				.icon(Items.SNOW_BLOCK)
				.guide("Half of all igloos have a carpet hiding a trapdoor. Down the ladder is a "
						+ "zombie villager, a golden apple and a splash potion of weakness - "
						+ "everything you need to cure it, laid out for you.")
				.tools("Any Shovel", "Torches")
				.task(new StructureTask("igloo", "Find an Igloo", OVERWORLD, StructureFeature.IGLOO),
						new ItemTask("snow", "Collect Snow Blocks", 16, Items.SNOW_BLOCK),
						new CheckmarkTask("basement", "Find an igloo with a basement")));

		addSide(Quest.builder("s20_ruined_portal", QuestPhase.SIDE)
				.title("The Ruined Portal")
				.desc("Someone else tried this before you.")
				.icon(Items.CRYING_OBSIDIAN)
				.guide("Netherrack in the Overworld means a ruined portal. The chest usually holds "
						+ "obsidian, flint and steel, and gold - occasionally an enchanted golden "
						+ "apple. Crying obsidian only comes from these and from bartering.")
				.tools("Diamond Pickaxe", "Shovel")
				.task(new StructureTask("ruined", "Find a Ruined Portal in the Overworld", OVERWORLD,
								StructureFeature.RUINED_PORTAL),
						new ItemTask("crying", "Collect Crying Obsidian", 2, Items.CRYING_OBSIDIAN)));

		addSide(Quest.builder("s21_librarian", QuestPhase.SIDE)
				.title("The Librarian")
				.desc("Somewhere to put all that paper.")
				.icon(Items.LECTERN)
				.guide("A lectern plus a bookshelf wall is how you re-roll a librarian's trades. "
						+ "Break and replace the lectern until the enchantment you want appears.")
				.tools("Sugar Cane Farm", "Leather", "Lectern")
				.task(new ItemTask("books", "Collect Books", 24, Items.BOOK),
						new CraftTask("shelves", "Craft Bookshelves", 32, Items.BOOKSHELF),
						new CheckmarkTask("library", "Build a library room")));

		addSide(Quest.builder("s22_redstone_engineer", QuestPhase.SIDE)
				.title("The Redstone Engineer")
				.desc("Every component, at least once.")
				.icon(Items.OBSERVER)
				.guide("Observers fire on block updates, comparators read container fullness, and "
						+ "droppers push items without launching them. This is the toolkit behind "
						+ "every farm you will ever build.")
				.tools("Redstone", "Iron", "Quartz")
				.task(new CraftTask("observer", "Craft an Observer", 1, Items.OBSERVER),
						new CraftTask("comparator", "Craft a Comparator", 1, Items.COMPARATOR),
						new CraftTask("dispenser", "Craft a Dispenser", 1, Items.DISPENSER),
						new CraftTask("dropper", "Craft a Dropper", 1, Items.DROPPER),
						new CraftTask("sticky", "Craft a Sticky Piston", 1, Items.STICKY_PISTON),
						new CraftTask("target", "Craft a Target Block", 1, Items.TARGET)));

		addSide(Quest.builder("s23_village_workstations", QuestPhase.SIDE)
				.title("Every Trade in Town")
				.desc("One workstation for every profession.")
				.icon(Items.CARTOGRAPHY_TABLE)
				.guide("Placing an unclaimed workstation next to a jobless villager assigns that "
						+ "profession. Breaking it before the first trade lets you re-roll.")
				.tools("Building Blocks", "Villagers")
				.task(new CraftTask("cartography", "Craft a Cartography Table", 1,
								Items.CARTOGRAPHY_TABLE),
						new CraftTask("fletching", "Craft a Fletching Table", 1, Items.FLETCHING_TABLE),
						new CraftTask("loom", "Craft a Loom", 1, Items.LOOM),
						new CraftTask("grindstone", "Craft a Grindstone", 1, Items.GRINDSTONE),
						new CraftTask("stonecutter", "Craft a Stonecutter", 1, Items.STONECUTTER),
						new CraftTask("composter", "Craft a Composter", 1, Items.COMPOSTER),
						new CraftTask("barrel", "Craft a Barrel", 1, Items.BARREL)));

		addSide(Quest.builder("s24_marksman", QuestPhase.SIDE)
				.title("The Marksman")
				.desc("Everything that fires at range.")
				.icon(Items.CROSSBOW)
				.guide("Crossbows hit harder and can be pre-loaded, but fire slower. Spectral arrows "
						+ "outline the target through walls; tipped arrows apply any potion effect.")
				.tools("Bow", "Crossbow", "Feathers", "Flint")
				.task(new CraftTask("crossbow", "Craft a Crossbow", 1, Items.CROSSBOW),
						new CraftTask("arrows", "Craft Arrows", 32, Items.ARROW),
						new CraftTask("spectral", "Craft Spectral Arrows", 4, Items.SPECTRAL_ARROW),
						new EnchantTask("piercing", "Enchant a crossbow (Piercing or Multishot)", 1, 1,
								Enchantments.PIERCING, Enchantments.MULTISHOT)));

		addSide(Quest.builder("s25_cactus_and_cane", QuestPhase.SIDE)
				.title("The Desert Farmer")
				.desc("Two crops that harvest themselves.")
				.icon(Items.CACTUS)
				.guide("Cactus breaks when it grows into a block beside it, so a single row of sand "
						+ "with fences and a hopper below is a fully automatic farm. Smelt it for "
						+ "green dye.")
				.tools("Sand", "Fences", "Hoppers")
				.task(new ItemTask("cactus", "Collect Cactus", 16, Items.CACTUS),
						new CraftTask("green", "Smelt Green Dye", 4, Items.GREEN_DYE)));

		addSide(Quest.builder("s26_bamboo_and_kelp", QuestPhase.SIDE)
				.title("Renewable Fuel")
				.desc("Two plants that solve fuel forever.")
				.icon(Items.DRIED_KELP_BLOCK)
				.guide("A dried kelp block smelts 20 items - better than coal. Bamboo grows to 16 "
						+ "blocks tall in jungles and is the fastest-growing plant in the game.")
				.tools("Jungle or Ocean", "Furnace", "Shears")
				.task(new ItemTask("bamboo", "Collect Bamboo", 32, Items.BAMBOO),
						new ItemTask("kelp", "Collect Kelp", 32, Items.KELP),
						new CraftTask("kelp_block", "Craft Dried Kelp Blocks", 4,
								Items.DRIED_KELP_BLOCK),
						new CraftTask("scaffold", "Craft Scaffolding", 16, Items.SCAFFOLDING)));

		addSide(Quest.builder("s27_decorator", QuestPhase.SIDE)
				.title("The Decorator")
				.desc("The blocks that exist only to make a base look lived in.")
				.icon(Items.ARMOR_STAND)
				.guide("Armour stands display a spare set of gear, item frames label chests, and "
						+ "paintings pick their size from the empty wall space you give them.")
				.tools("Sticks", "Wool", "Leather")
				.task(new CraftTask("stand", "Craft Armour Stands", 2, Items.ARMOR_STAND),
						new CraftTask("frames", "Craft Item Frames", 8, Items.ITEM_FRAME),
						new CraftTask("paintings", "Craft Paintings", 4, Items.PAINTING),
						new CraftTask("pots", "Craft Flower Pots", 4, Items.FLOWER_POT)));

		addSide(Quest.builder("s28_candlemaker", QuestPhase.SIDE)
				.title("The Candlemaker")
				.desc("Light that comes in sixteen colours.")
				.icon(Items.CANDLE)
				.guide("1 string + 1 honeycomb. Up to four candles stack in one block, and each one "
						+ "adds 3 light levels. A cake with a candle on it is the vanilla birthday "
						+ "cake.")
				.tools("Honeycomb", "String", "Dyes")
				.task(new CraftTask("candles", "Craft Candles", 8, Items.CANDLE),
						new CraftTask("dyed", "Craft Dyed Candles", 4,
								Items.RED_CANDLE, Items.BLUE_CANDLE, Items.GREEN_CANDLE,
								Items.YELLOW_CANDLE),
						new CheckmarkTask("cake_candle", "Put a candle on a cake")));

		addSide(Quest.builder("s29_pacifist", QuestPhase.SIDE)
				.title("The Pacifist Run")
				.desc("A week without killing anything.")
				.icon(Items.SHIELD)
				.guide("Survive ten in-game days without killing a single mob - hostile or passive. "
						+ "Live on crops and fishing, and let the shield and the walls do the work. "
						+ "Confirm it yourself.")
				.tools("Shield", "Crop Farm", "Nerve")
				.task(new CheckmarkTask("pacifist", "Survive ten in-game days without killing anything")));

		addSide(Quest.builder("s30_dragons_head", QuestPhase.SIDE)
				.title("The Dragon's Head")
				.desc("The best-looking block in the game.")
				.icon(Items.DRAGON_HEAD)
				.guide("On the bow of every End Ship, hanging over the void. Bridge out to it or it "
						+ "falls forever. Any tool works.")
				.tools("Elytra", "Firework Rockets", "Building Blocks")
				.task(new ItemTask("head", "Obtain a Dragon Head", 1, Items.DRAGON_HEAD)));

		addSide(Quest.builder("s31_glassblower", QuestPhase.SIDE)
				.title("The Glassblower")
				.desc("Sand, fire, and a bit of dye.")
				.icon(Items.GLASS)
				.guide("Smelt sand into glass. Eight glass around a dye makes eight stained glass; six "
						+ "glass in a row makes sixteen panes. Tinted glass needs amethyst and blocks "
						+ "light without blocking the view.")
				.tools("Furnace", "Sand", "Dyes")
				.task(new ItemTask("glass", "Collect Glass", 32, Items.GLASS),
						new CraftTask("stained", "Craft Stained Glass", 16,
								Items.WHITE_STAINED_GLASS, Items.BLUE_STAINED_GLASS,
								Items.RED_STAINED_GLASS, Items.GREEN_STAINED_GLASS,
								Items.YELLOW_STAINED_GLASS, Items.BLACK_STAINED_GLASS),
						new CraftTask("panes", "Craft Glass Panes", 32, Items.GLASS_PANE)));

		addSide(Quest.builder("s32_concrete", QuestPhase.SIDE)
				.title("Concrete Foundations")
				.desc("The cleanest building block there is.")
				.icon(Items.LIGHT_BLUE_CONCRETE)
				.guide("4 sand + 4 gravel + 1 dye makes 8 concrete powder. Powder hardens into "
						+ "concrete the instant it touches water - drop it in, mine it back.")
				.tools("Sand", "Gravel", "Dyes", "Water")
				.task(new CraftTask("powder", "Craft Concrete Powder", 32,
								Items.WHITE_CONCRETE_POWDER, Items.LIGHT_BLUE_CONCRETE_POWDER,
								Items.GRAY_CONCRETE_POWDER, Items.RED_CONCRETE_POWDER),
						new ItemTask("concrete", "Harden Concrete", 16,
								Items.WHITE_CONCRETE, Items.LIGHT_BLUE_CONCRETE,
								Items.GRAY_CONCRETE, Items.RED_CONCRETE, Items.BLACK_CONCRETE)));

		addSide(Quest.builder("s33_banners", QuestPhase.SIDE)
				.title("The Banner Maker")
				.desc("A flag for the front of your base.")
				.icon(Items.CYAN_BANNER)
				.guide("6 wool + 1 stick makes a banner. A Loom applies patterns from dyes and banner "
						+ "pattern items far more cheaply than the crafting grid.")
				.tools("Wool", "Loom", "Dyes")
				.task(new CraftTask("loom", "Craft a Loom", 1, Items.LOOM),
						new CraftTask("banners", "Craft Banners", 4,
								Items.WHITE_BANNER, Items.CYAN_BANNER, Items.RED_BANNER,
								Items.BLACK_BANNER, Items.BLUE_BANNER)));

		addSide(Quest.builder("s34_pyrotechnician", QuestPhase.SIDE)
				.title("The Pyrotechnician")
				.desc("Not for flying - for the show.")
				.icon(Items.FIREWORK_ROCKET)
				.guide("A firework star is gunpowder + dye, plus optional shape items (fire charge, "
						+ "gold nugget, feather, head). Add stars to a rocket to paint the sky.")
				.tools("Gunpowder", "Dyes", "Paper")
				.task(new CraftTask("stars", "Craft Firework Stars", 8, Items.FIREWORK_STAR),
						new CraftTask("rockets", "Craft Firework Rockets", 32, Items.FIREWORK_ROCKET)));

		addSide(Quest.builder("s35_stonemason", QuestPhase.SIDE)
				.title("The Stonemason")
				.desc("Every polished, chiselled and cut variant of the stone you walk on.")
				.icon(Items.CHISELED_STONE_BRICKS)
				.guide("A Stonecutter turns one block into any of its cut variants with no waste, and "
						+ "does it one-to-one instead of the 4-to-4 of the crafting grid.")
				.tools("Stone", "Stonecutter")
				.task(new CraftTask("stonecutter", "Craft a Stonecutter", 1, Items.STONECUTTER),
						new CraftTask("polished", "Cut Polished Stone", 16,
								Items.POLISHED_ANDESITE, Items.POLISHED_DIORITE,
								Items.POLISHED_GRANITE, Items.SMOOTH_STONE),
						new CraftTask("chiseled", "Craft Chiselled Stone Bricks", 4,
								Items.CHISELED_STONE_BRICKS)));
	}
}
