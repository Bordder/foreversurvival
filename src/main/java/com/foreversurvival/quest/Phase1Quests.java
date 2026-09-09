package com.foreversurvival.quest;

import static com.foreversurvival.quest.QuestRegistry.BEDS;
import static com.foreversurvival.quest.QuestRegistry.COOKED_FOOD;
import static com.foreversurvival.quest.QuestRegistry.HOES;
import static com.foreversurvival.quest.QuestRegistry.LOGS;
import static com.foreversurvival.quest.QuestRegistry.PLANKS;
import static com.foreversurvival.quest.QuestRegistry.RAW_MEAT;
import static com.foreversurvival.quest.QuestRegistry.SAPLINGS;
import static com.foreversurvival.quest.QuestRegistry.WOOL;
import static com.foreversurvival.quest.QuestRegistry.addMain;

import com.foreversurvival.quest.task.CraftTask;
import com.foreversurvival.quest.task.ItemTask;
import com.foreversurvival.quest.task.KillTask;
import com.foreversurvival.quest.task.PositionTask;
import com.foreversurvival.quest.task.StatTask;
import com.foreversurvival.quest.task.StructureTask;
import com.foreversurvival.quest.task.UseTask;

import net.minecraft.level().level.block.Blocks;
import net.minecraft.level().entity.EntityType;
import net.minecraft.level().entity.EntityTypes;
import net.minecraft.level().item.DyeColor;
import net.minecraft.level().item.Items;
import net.minecraft.stats.Stats;

/**
 * PHASE 1 - THE STRUGGLE (18 quests).
 *
 * The first handful of in-game days: bare hands to a stack of iron. Every beat
 * here is something a real player does in their first session or two.
 */
final class Phase1Quests {

	private Phase1Quests() {
	}

	static void register() {
		addMain(Quest.builder("p1_01_first_blood", QuestPhase.PHASE_1)
				.title("First Blood")
				.desc("Everything starts with a tree and a sore fist.")
				.icon(Items.OAK_LOG)
				.guide("Hold left click on tree trunks until the logs pop out.")
				.tools("Hands")
				.task(new ItemTask("logs", "Collect Logs", 24, LOGS)));

		addMain(Quest.builder("p1_02_crafting_table", QuestPhase.PHASE_1)
				.title("A Proper Workbench")
				.desc("Two-by-two only gets you so far.")
				.icon(Items.CRAFTING_TABLE)
				.guide("1 log makes 4 planks. 2 stacked planks make 4 sticks. 4 planks in a square "
						+ "make a Crafting Table.")
				.tools("Logs")
				.task(new CraftTask("planks", "Craft Planks", 32, PLANKS),
						new CraftTask("sticks", "Craft Sticks", 8, Items.STICK),
						new CraftTask("table", "Craft a Crafting Table", 1, Items.CRAFTING_TABLE)));

		addMain(Quest.builder("p1_03_stone_age", QuestPhase.PHASE_1)
				.title("The Stone Age")
				.desc("Wood breaks. Stone does not - at least, not as fast.")
				.icon(Items.COBBLESTONE)
				.guide("Stone is anywhere at Y=0 or above. Dig into a hillside, never straight down. "
						+ "A furnace and a full tool set costs about 20, so gather a spare stack.")
				.tools("Wooden Pickaxe")
				.task(new CraftTask("wood_pick", "Craft a Wooden Pickaxe", 1, Items.WOODEN_PICKAXE),
						new ItemTask("cobble", "Collect Cobblestone", 48,
								Items.COBBLESTONE, Items.COBBLED_DEEPSLATE)));

		addMain(Quest.builder("p1_04_tools_of_the_trade", QuestPhase.PHASE_1)
				.title("Tools of the Trade")
				.desc("A full stone kit is the first real quality-of-life jump.")
				.icon(Items.STONE_PICKAXE)
				.guide("Same recipes as wood, using cobblestone. A Stone Pickaxe is required to mine "
						+ "iron and coal ore.")
				.tools("Crafting Table", "Cobblestone", "Sticks")
				.task(new CraftTask("s_pick", "Craft a Stone Pickaxe", 1, Items.STONE_PICKAXE),
						new CraftTask("s_axe", "Craft a Stone Axe", 1, Items.STONE_AXE),
						new CraftTask("s_sword", "Craft a Stone Sword", 1, Items.STONE_SWORD),
						new CraftTask("s_shovel", "Craft a Stone Shovel", 1, Items.STONE_SHOVEL)));

		addMain(Quest.builder("p1_05_the_hunt", QuestPhase.PHASE_1)
				.title("The Hunt")
				.desc("Nothing here is going to feed you for free.")
				.icon(Items.PORKCHOP)
				.guide("Cows, pigs, sheep and chickens all drop raw meat. Eating it raw works but "
						+ "wastes most of the hunger it is worth.")
				.tools("Stone Sword")
				.task(new KillTask("animals", "Kill Animals", 10,
								EntityTypes.COW, EntityTypes.PIG, EntityTypes.SHEEP,
								EntityTypes.CHICKEN, EntityTypes.RABBIT),
						new ItemTask("raw_meat", "Carry Raw Meat", 10, RAW_MEAT)));

		addMain(Quest.builder("p1_06_a_warm_meal", QuestPhase.PHASE_1)
				.title("A Warm Meal")
				.desc("Raw chicken is a bad plan.")
				.icon(Items.FURNACE)
				.guide("8 cobblestone in a ring makes a Furnace. Fuel goes bottom-left, food top-left.")
				.tools("Furnace", "Fuel")
				.task(new CraftTask("furnace", "Craft a Furnace", 1, Items.FURNACE),
						new ItemTask("cooked", "Carry Cooked Food", 12, COOKED_FOOD)));

		addMain(Quest.builder("p1_07_charcoal_burner", QuestPhase.PHASE_1)
				.title("The Charcoal Burner")
				.desc("You never actually needed to find coal.")
				.icon(Items.CHARCOAL)
				.guide("Smelt logs in a furnace to get Charcoal. It burns and crafts torches exactly "
						+ "like coal, so a single tree makes you self-sufficient.")
				.tools("Furnace", "Logs")
				.task(new ItemTask("charcoal", "Smelt Charcoal", 16, Items.CHARCOAL),
						new CraftTask("campfire", "Craft a Campfire", 1, Items.CAMPFIRE)));

		addMain(Quest.builder("p1_08_let_there_be_light", QuestPhase.PHASE_1)
				.title("Let There Be Light")
				.desc("Darkness is not scary. What lives in it is.")
				.icon(Items.TORCH)
				.guide("Coal is most common near Y=96 and sits exposed on mountain faces. "
						+ "1 coal + 1 stick = 4 torches.")
				.tools("Stone Pickaxe")
				.task(new ItemTask("coal", "Collect Coal", 16, Items.COAL),
						new CraftTask("torches", "Craft Torches", 48, Items.TORCH)));

		addMain(Quest.builder("p1_09_wool_gathering", QuestPhase.PHASE_1)
				.title("Wool Gathering")
				.desc("Skipping the night is the biggest safety upgrade you get for free.")
				.icon(Items.WOOL.pick(DyeColor.WHITE))
				.guide("3 wool of the same colour + 3 planks = Bed. Never sleep in the Nether or the "
						+ "End - the bed explodes.")
				.tools("Shears or any Sword")
				.task(new ItemTask("wool", "Collect Wool", 6, WOOL),
						new CraftTask("bed", "Craft a Bed", 1, BEDS)));

		addMain(Quest.builder("p1_10_good_night", QuestPhase.PHASE_1)
				.title("Good Night")
				.desc("The first night you sleep through instead of hiding through.")
				.icon(Items.BED.pick(DyeColor.RED))
				.guide("Place the bed and use it after dusk. Sleeping sets your respawn point and "
						+ "clears Phantoms for three days.")
				.tools("Bed", "A lit, enclosed room")
				.task(new StatTask("slept", "Sleep through a night", 1, Stats.SLEEP_IN_BED)));

		addMain(Quest.builder("p1_11_four_walls", QuestPhase.PHASE_1)
				.title("Four Walls and a Roof")
				.desc("Somewhere to put your things and not die.")
				.icon(Items.CHEST)
				.guide("Place a crafting table, a furnace and a chest in an enclosed, lit room, then "
						+ "stand inside it.")
				.tools("Crafting Table", "Furnace", "Chest", "Torches")
				.task(new CraftTask("chest", "Craft a Chest", 1, Items.CHEST),
						new StructureTask("shelter", "Stand in your base (table, furnace and chest)",
								null, Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.CHEST)));

		addMain(Quest.builder("p1_12_hide_and_bone", QuestPhase.PHASE_1)
				.title("Leather Bound")
				.desc("The first armour you will ever wear, and it is barely armour.")
				.icon(Items.LEATHER_CHESTPLATE)
				.guide("Leather comes from cows. A full leather set is only 7 armour points but it is "
						+ "the difference between surviving a creeper and not.")
				.tools("Stone Sword", "Leather")
				.task(new ItemTask("leather", "Collect Leather", 8, Items.LEATHER),
						new CraftTask("l_chest", "Craft a Leather Chestplate", 1,
								Items.LEATHER_CHESTPLATE),
						new CraftTask("l_boots", "Craft Leather Boots", 1, Items.LEATHER_BOOTS)));

		addMain(Quest.builder("p1_13_night_watch", QuestPhase.PHASE_1)
				.title("The Night Watch")
				.desc("Learn what each of them does before it learns what you do.")
				.icon(Items.ROTTEN_FLESH)
				.guide("Hit a creeper once and step back - it detonates 1.5 seconds after it hisses.")
				.tools("Stone Sword", "Torches")
				.task(new KillTask("zombies", "Kill Zombies", 15, EntityTypes.ZOMBIE),
						new KillTask("skeletons", "Kill Skeletons", 12, EntityTypes.SKELETON),
						new KillTask("spiders", "Kill Spiders", 8, EntityTypes.SPIDER),
						new KillTask("creepers", "Kill Creepers", 5, EntityTypes.CREEPER)));

		addMain(Quest.builder("p1_14_arrows_and_aim", QuestPhase.PHASE_1)
				.title("Arrows and Aim")
				.desc("The first weapon that kills things before they reach you.")
				.icon(Items.BOW)
				.guide("A bow is 3 sticks + 3 string from spiders. Arrows need flint, which drops "
						+ "from about 1 gravel block in 10.")
				.tools("String", "Flint", "Feathers")
				.task(new ItemTask("flint", "Collect Flint", 6, Items.FLINT),
						new CraftTask("bow", "Craft a Bow", 1, Items.BOW),
						new CraftTask("arrows", "Craft Arrows", 32, Items.ARROW)));

		addMain(Quest.builder("p1_15_seeds_and_soil", QuestPhase.PHASE_1)
				.title("Seeds and Soil")
				.desc("The moment you stop hunting for every meal.")
				.icon(Items.WHEAT_SEEDS)
				.guide("Breaking tall grass drops seeds. Till dirt with a hoe next to water - "
						+ "farmland stays hydrated within 4 blocks of a water source.")
				.tools("Hoe", "Water nearby")
				.task(new CraftTask("hoe", "Craft a Hoe", 1, HOES),
						new ItemTask("seeds", "Collect Wheat Seeds", 12, Items.WHEAT_SEEDS),
						new UseTask("tilled", "Till Farmland", 12, HOES)));

		addMain(Quest.builder("p1_16_saplings", QuestPhase.PHASE_1)
				.title("Renewable Wood")
				.desc("Replant, or spend the rest of the world walking further for logs.")
				.icon(Items.OAK_SAPLING)
				.guide("Leaves drop saplings when broken. Plant them with 1 block of space around "
						+ "each and they grow back in a few minutes with light.")
				.tools("Axe", "Saplings")
				.task(new ItemTask("saplings", "Collect Saplings", 8, SAPLINGS),
						new UseTask("planted", "Plant Saplings", 8, SAPLINGS)));

		addMain(Quest.builder("p1_17_into_the_dark", QuestPhase.PHASE_1)
				.title("Into the Dark")
				.desc("The first cave you go into on purpose.")
				.icon(Items.LANTERN)
				.guide("Torch every corner behind you and place them on the RIGHT wall going in - "
						+ "then follow the left wall to get out. Never dig straight down.")
				.tools("Stone Pickaxe", "Torches", "Cooked Food", "Sword")
				.task(new ItemTask("more_coal", "Collect Coal", 24, Items.COAL),
						new PositionTask("deep", "Descend to Y=20 or lower",
								PositionTask.Kind.BELOW_Y, 20)));

		addMain(Quest.builder("p1_18_down_the_rabbit_hole", QuestPhase.PHASE_1)
				.title("Down the Rabbit Hole")
				.desc("The struggle ends the moment you are holding iron.")
				.icon(Items.RAW_IRON)
				.guide("Iron has two bands: underground peaking at Y=16, and mountains peaking at "
						+ "Y=232 where it is often on the surface in Jagged Peaks.")
				.tools("Stone Pickaxe", "Torches", "Food")
				.task(new ItemTask("raw_iron", "Collect Raw Iron", 24, Items.RAW_IRON)));
	}
}
