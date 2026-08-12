package com.foreversurvival.quest;

import static com.foreversurvival.quest.QuestRegistry.NETHER;
import static com.foreversurvival.quest.QuestRegistry.addMain;

import com.foreversurvival.quest.task.CheckmarkTask;
import com.foreversurvival.quest.task.CraftTask;
import com.foreversurvival.quest.task.ItemTask;
import com.foreversurvival.quest.task.KillTask;
import com.foreversurvival.quest.task.StructureTask;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Items;
import net.minecraft.world.gen.feature.StructureFeature;

/**
 * PHASE 3 - THE EXPANSION (22 quests, MAJOR).
 *
 * The Nether, biome by biome.
 *
 * ORDER MATTERS: the safe work comes first. Magma cream is gathered in the
 * Nether Wastes, then the fortress gives blaze rods, then brewing, then Fire
 * Resistance - and only after that do the quests that genuinely need it
 * (glowstone over lava, the Basalt Deltas, striders, bastions, debris mining).
 */
final class Phase3Quests {

	private Phase3Quests() {
	}

	static void register() {
		addMain(Quest.builder("p3_01_obsidian_gate", QuestPhase.PHASE_3)
				.title("The Obsidian Gate")
				.desc("Ten blocks of cooled lava and a spark.")
				.icon(Items.OBSIDIAN)
				.guide("A cornerless frame needs 10 obsidian. Pour water on lava to make it. "
						+ "Build the portal inside a walled room - things come through both ways.")
				.tools("Diamond Pickaxe", "Water Bucket", "Flint and Steel")
				.task(new ItemTask("obsidian", "Collect Obsidian", 10, Items.OBSIDIAN),
						new CraftTask("fns", "Craft Flint and Steel", 1, Items.FLINT_AND_STEEL),
						new StructureTask("portal", "Stand next to a lit Nether Portal", null,
								Blocks.NETHER_PORTAL)));

		addMain(Quest.builder("p3_02_into_the_fire", QuestPhase.PHASE_3)
				.title("Into the Fire")
				.desc("No water, no beds, no mercy.")
				.icon(Items.NETHERRACK)
				.guide("Wear at least one piece of gold armour so piglins stay neutral. "
						+ "Write down your portal coordinates before you take a single step.")
				.tools("Diamond Armour", "Gold Helmet", "Blocks to bridge", "Food")
				.task(new StructureTask("enter", "Enter the Nether", NETHER),
						new ItemTask("netherrack", "Collect Netherrack", 32, Items.NETHERRACK)));

		addMain(Quest.builder("p3_03_quartz_and_gold", QuestPhase.PHASE_3)
				.title("Quartz and Gold")
				.desc("The two things the Nether gives up without a fight.")
				.icon(Items.QUARTZ)
				.guide("Nether Quartz Ore is everywhere at any height and only needs a wooden "
						+ "pickaxe. Gold ore here is far denser than in the Overworld.")
				.tools("Any Pickaxe", "Gold Armour")
				.task(new ItemTask("quartz", "Collect Nether Quartz", 16, Items.QUARTZ),
						new ItemTask("gold", "Collect Gold Ingots", 8, Items.GOLD_INGOT),
						new CraftTask("detector", "Craft a Daylight Detector", 1,
								Items.DAYLIGHT_DETECTOR)));

		addMain(Quest.builder("p3_04_ghasts_and_magma", QuestPhase.PHASE_3)
				.title("Ghasts and Magma")
				.desc("One screams at you from 100 blocks. The other bounces.")
				.icon(Items.GHAST_TEAR)
				.guide("Both spawn in the open Nether Wastes. Punch or shoot a ghast fireball back "
						+ "at it for a one-hit kill. Magma cubes split when killed - deal with the "
						+ "small ones first. Their cream is what Fire Resistance is brewed from.")
				.tools("Bow", "Shield", "Diamond Armour")
				.task(new KillTask("ghasts", "Kill Ghasts", 3, EntityType.GHAST),
						new ItemTask("tears", "Collect Ghast Tears", 2, Items.GHAST_TEAR),
						new KillTask("magma", "Kill Magma Cubes", 8, EntityType.MAGMA_CUBE),
						new ItemTask("cream", "Collect Magma Cream", 4, Items.MAGMA_CREAM)));

		addMain(Quest.builder("p3_05_crimson_forest", QuestPhase.PHASE_3)
				.title("The Crimson Forest")
				.desc("Red fungus, red trees, and something that wants to gore you.")
				.icon(Items.CRIMSON_FUNGUS)
				.guide("Hoglins are the only renewable food in the Nether. They are afraid of warped "
						+ "fungus and of nether portals - put one of either between you and them. "
						+ "Crimson and warped stems are the only fireproof wood in the game.")
				.tools("Diamond Sword", "Gold Armour", "Axe")
				.task(new KillTask("hoglins", "Kill Hoglins", 6, EntityType.HOGLIN),
						new ItemTask("crimson", "Collect Crimson Stems", 12, Items.CRIMSON_STEM),
						new ItemTask("fungus", "Collect Crimson Fungus", 4, Items.CRIMSON_FUNGUS)));

		addMain(Quest.builder("p3_06_warped_forest", QuestPhase.PHASE_3)
				.title("The Warped Forest")
				.desc("The safest biome in the Nether, and the strangest.")
				.icon(Items.WARPED_FUNGUS)
				.guide("Nothing hostile spawns here except endermen, which makes it the best place "
						+ "to farm pearls. Bone meal a fungus on its matching nylium to grow a huge "
						+ "fungus tree.")
				.tools("Diamond Sword", "Carved Pumpkin", "Bone Meal")
				.task(new ItemTask("warped", "Collect Warped Stems", 12, Items.WARPED_STEM),
						new ItemTask("w_fungus", "Collect Warped Fungus", 4, Items.WARPED_FUNGUS),
						new ItemTask("shroomlight", "Collect Shroomlights", 4, Items.SHROOMLIGHT)));

		addMain(Quest.builder("p3_07_soul_sand_valley", QuestPhase.PHASE_3)
				.title("The Soul Sand Valley")
				.desc("A skeleton graveyard the size of a country.")
				.icon(Items.SOUL_SAND)
				.guide("Soul sand slows you to a crawl and skeletons spawn here faster than anywhere "
						+ "else in the game. Soul torches and soul lanterns are the only light that "
						+ "scares piglins away.")
				.tools("Shovel", "Bow", "Shield", "Gold Armour")
				.task(new ItemTask("soul_sand", "Collect Soul Sand", 12, Items.SOUL_SAND),
						new ItemTask("bones", "Collect Bone Blocks", 8, Items.BONE_BLOCK),
						new CraftTask("soul_torch", "Craft Soul Torches", 8, Items.SOUL_TORCH)));

		addMain(Quest.builder("p3_08_fortress_hunt", QuestPhase.PHASE_3)
				.title("The Fortress Hunt")
				.desc("Blaze rods only come from one place.")
				.icon(Items.NETHER_BRICKS)
				.guide("Fortresses run in long straight corridors - travel in a straight line to cross "
						+ "one. Blazes spawn in fortresses only, never bastions.")
				.tools("Diamond Armour", "Bow", "Shield")
				.task(new StructureTask("fortress", "Find a Nether Fortress", NETHER,
								StructureFeature.FORTRESS),
						new ItemTask("nether_brick", "Collect Nether Bricks", 16,
								Items.NETHER_BRICKS)));

		addMain(Quest.builder("p3_09_blaze_of_glory", QuestPhase.PHASE_3)
				.title("A Blaze of Glory")
				.desc("Every potion you will ever brew starts here.")
				.icon(Items.BLAZE_ROD)
				.guide("Fight from behind a wall with a 1 block gap. Snowballs damage them. "
						+ "Expect about 2 kills per rod; 7 rods covers a stand and 12 eyes.")
				.tools("Diamond Sword", "Shield", "Diamond Armour")
				.task(new KillTask("blazes", "Kill Blazes", 12, EntityType.BLAZE),
						new ItemTask("rods", "Collect Blaze Rods", 7, Items.BLAZE_ROD)));

		addMain(Quest.builder("p3_10_nether_wart", QuestPhase.PHASE_3)
				.title("The Wart Farm")
				.desc("Take the crop home before you take anything else.")
				.icon(Items.NETHER_WART)
				.guide("Nether wart grows on soul sand in the fortress staircases. It grows in any "
						+ "dimension and at any light level, so bring soul sand back and farm it in "
						+ "your base - every potion needs it.")
				.tools("Soul Sand", "Any Tool")
				.task(new ItemTask("wart", "Collect Nether Wart", 6, Items.NETHER_WART),
						new CheckmarkTask("wart_farm", "Plant a nether wart farm at your base")));

		addMain(Quest.builder("p3_11_potion_master", QuestPhase.PHASE_3)
				.title("The Potion Master")
				.desc("Brewing is the difference between hard and trivial.")
				.icon(Items.BREWING_STAND)
				.guide("Brewing Stand = 1 blaze rod + 3 cobblestone. Nether wart in a water bottle "
						+ "makes an Awkward Potion, which is the base of everything useful.")
				.tools("Blaze Rods", "Nether Wart", "Glass Bottles")
				.task(new CraftTask("stand", "Craft a Brewing Stand", 1, Items.BREWING_STAND),
						new CraftTask("bottles", "Craft Glass Bottles", 6, Items.GLASS_BOTTLE),
						new ItemTask("potions", "Carry Brewed Potions", 3, Items.POTION)));

		addMain(Quest.builder("p3_12_fire_resistance", QuestPhase.PHASE_3)
				.title("Fireproof")
				.desc("The potion that turns the Nether from lethal to tedious.")
				.icon(Items.MAGMA_CREAM)
				.guide("Awkward Potion + Magma Cream = Fire Resistance. Add redstone for 8 minutes. "
						+ "Brew a few before you go anywhere near the Basalt Deltas or a bastion.")
				.tools("Brewing Stand", "Magma Cream", "Redstone")
				.task(new CheckmarkTask("brewed_fire_res", "Brew a Potion of Fire Resistance"),
						new ItemTask("stock", "Carry Brewed Potions", 5, Items.POTION)));

		addMain(Quest.builder("p3_13_glowstone", QuestPhase.PHASE_3)
				.title("Ceiling Light")
				.desc("The brightest block in the game, hanging over lava.")
				.icon(Items.GLOWSTONE)
				.guide("Glowstone clusters hang from the Nether ceiling. Break one and the dust "
						+ "scatters - place a slab underneath first or you lose it to the lava.")
				.tools("Fire Resistance Potion", "Blocks to bridge", "Slabs")
				.task(new ItemTask("glowstone", "Collect Glowstone Dust", 16, Items.GLOWSTONE_DUST),
						new CraftTask("blocks", "Craft Glowstone Blocks", 4, Items.GLOWSTONE),
						new CraftTask("lantern", "Craft a Lantern", 1, Items.LANTERN)));

		addMain(Quest.builder("p3_14_basalt_deltas", QuestPhase.PHASE_3)
				.title("The Basalt Deltas")
				.desc("Sharp, black, and on fire in every direction.")
				.icon(Items.BASALT)
				.guide("The most dangerous Nether biome: magma cubes everywhere, lava lakes, and "
						+ "blackstone that looks exactly like the floor. Blackstone replaces "
						+ "cobblestone in every recipe. Do not come here without Fire Resistance.")
				.tools("Fire Resistance Potion", "Diamond Armour", "Blocks")
				.task(new ItemTask("basalt", "Collect Basalt", 16, Items.BASALT),
						new ItemTask("blackstone", "Collect Blackstone", 16, Items.BLACKSTONE)));

		addMain(Quest.builder("p3_15_striders", QuestPhase.PHASE_3)
				.title("Riding the Lava")
				.desc("A lava lake is a highway if you have the right mount.")
				.icon(Items.WARPED_FUNGUS_ON_A_STICK)
				.guide("Saddle a strider and steer it with a Warped Fungus on a Stick. They walk on "
						+ "lava at full speed and are the only safe way across an open lava sea.")
				.tools("Saddle", "Warped Fungus", "Fire Resistance Potion")
				.task(new CraftTask("fungus_stick", "Craft a Warped Fungus on a Stick", 1,
								Items.WARPED_FUNGUS_ON_A_STICK),
						new CheckmarkTask("rode", "Ride a strider across a lava lake")));

		addMain(Quest.builder("p3_16_pearl_diver", QuestPhase.PHASE_3)
				.title("The Pearl Diver")
				.desc("Twelve pearls minimum, and Endermen do not hand them over.")
				.icon(Items.ENDER_PEARL)
				.guide("Warped Forests have the highest density. Endermen are 3 blocks tall, so a "
						+ "2 block tunnel is safe. A carved pumpkin stops them aggroing.")
				.tools("Diamond Sword", "Carved Pumpkin")
				.task(new KillTask("endermen", "Kill Endermen", 16, EntityType.ENDERMAN),
						new ItemTask("pearls", "Collect Ender Pearls", 12, Items.ENDER_PEARL)));

		addMain(Quest.builder("p3_17_bastion_raider", QuestPhase.PHASE_3)
				.title("The Bastion Raider")
				.desc("The best loot in the Nether, guarded by the worst neighbours.")
				.icon(Items.GILDED_BLACKSTONE)
				.guide("Keep gold armour on the whole time. Never open a chest or mine gold in front "
						+ "of a piglin - that aggros the entire structure anyway.")
				.tools("Gold Armour", "Fire Resistance Potions", "Building Blocks")
				.task(new StructureTask("bastion", "Find a Bastion Remnant", NETHER,
								StructureFeature.BASTION_REMNANT),
						new ItemTask("gilded", "Collect Gilded Blackstone", 2,
								Items.GILDED_BLACKSTONE)));

		addMain(Quest.builder("p3_18_piglin_barter", QuestPhase.PHASE_3)
				.title("The Barter")
				.desc("They will take your gold. They might give something back.")
				.icon(Items.GOLD_NUGGET)
				.guide("Drop or right-click a gold ingot at an adult piglin. Bartering is the only "
						+ "renewable source of Ender Pearls, Fire Resistance potions and obsidian.")
				.tools("Gold Ingots", "Gold Armour")
				.task(new CheckmarkTask("bartered", "Barter with a piglin"),
						new ItemTask("nuggets", "Collect Gold Nuggets", 16, Items.GOLD_NUGGET)));

		addMain(Quest.builder("p3_19_respawn_anchor", QuestPhase.PHASE_3)
				.title("The Respawn Anchor")
				.desc("The Nether's answer to a bed, and it does not explode.")
				.icon(Items.RESPAWN_ANCHOR)
				.guide("6 crying obsidian + 3 glowstone. Crying obsidian comes from bartering and "
						+ "ruined portals. Charge the anchor with glowstone; it only works in the "
						+ "Nether and detonates anywhere else.")
				.tools("Crying Obsidian", "Glowstone", "Diamond Pickaxe")
				.task(new ItemTask("crying", "Collect Crying Obsidian", 6, Items.CRYING_OBSIDIAN),
						new CraftTask("anchor", "Craft a Respawn Anchor", 1, Items.RESPAWN_ANCHOR)));

		addMain(Quest.builder("p3_20_nether_highway", QuestPhase.PHASE_3)
				.title("The Nether Highway")
				.desc("One block here is eight at home. Build accordingly.")
				.icon(Items.POWERED_RAIL)
				.guide("Divide your Overworld coordinates by 8 to get the Nether coordinates for a "
						+ "linked portal. A tunnel network at Y=128, above the ceiling or along it, "
						+ "turns a 4000 block trip into a 500 block walk.")
				.tools("Building Blocks", "Pickaxe", "Obsidian", "Flint and Steel")
				.task(new CheckmarkTask("second_portal", "Link a second Overworld portal through the Nether"),
						new CheckmarkTask("tunnel", "Build a Nether tunnel between two portals")));

		addMain(Quest.builder("p3_21_ancient_debris", QuestPhase.PHASE_3)
				.title("Ancient Debris")
				.desc("The rarest ore in the game, and it will not show itself.")
				.icon(Items.ANCIENT_DEBRIS)
				.guide("Peaks at Y=15 and never generates exposed to air, so caving finds nothing. "
						+ "Tunnel at Y=15 and blast with beds or TNT. 4 debris makes 1 ingot.")
				.tools("Diamond Pickaxe", "Fire Resistance Potions", "Beds or TNT")
				.task(new ItemTask("debris", "Collect Ancient Debris", 4, Items.ANCIENT_DEBRIS)));

		addMain(Quest.builder("p3_22_netherite", QuestPhase.PHASE_3)
				.title("Netherite")
				.desc("It floats in lava. So will your gear.")
				.icon(Items.NETHERITE_INGOT)
				.guide("4 scrap + 4 gold ingots = 1 netherite ingot. Enchant the diamond item FIRST - "
						+ "the smithing table keeps enchantments and there is no way back.")
				.tools("Furnace", "4 Gold Ingots", "Smithing Table")
				.task(new CraftTask("smithing", "Craft a Smithing Table", 1, Items.SMITHING_TABLE),
						new CraftTask("ingot", "Craft a Netherite Ingot", 1, Items.NETHERITE_INGOT),
						new CraftTask("n_pick", "Upgrade to a Netherite Pickaxe", 1,
								Items.NETHERITE_PICKAXE)));
	}
}
