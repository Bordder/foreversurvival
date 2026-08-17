package com.foreversurvival.quest;

import static com.foreversurvival.quest.QuestRegistry.END;
import static com.foreversurvival.quest.QuestRegistry.OVERWORLD;
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
 * PHASE 5 - THE ENDGAME (18 quests, MAJOR).
 *
 * The End, start to finish: the stronghold, the dragon, the outer islands,
 * Elytra, and the two farms that only exist out here.
 */
final class Phase5Quests {

	private Phase5Quests() {
	}

	static void register() {
		addMain(Quest.builder("p5_01_eyes_of_ender", QuestPhase.PHASE_5)
				.title("Eyes of Ender")
				.desc("Twelve to find the door, twelve to open it.")
				.icon(Items.ENDER_EYE)
				.guide("1 pearl + 1 blaze powder. Thrown eyes shatter 20% of the time, so 14 is the "
						+ "safe number for a 12-frame portal.")
				.tools("Ender Pearls", "Blaze Powder")
				.task(new CraftTask("eyes", "Craft Eyes of Ender", 14, Items.ENDER_EYE)));

		addMain(Quest.builder("p5_02_ender_chest", QuestPhase.PHASE_5)
				.title("The Ender Chest")
				.desc("The same 27 slots, from anywhere in any dimension.")
				.icon(Items.ENDER_CHEST)
				.guide("8 obsidian + 1 eye of ender. Its contents follow you across dimensions and "
						+ "survive death, so it is the safest place to keep your spare gear before a "
						+ "boss fight. Mine it back with any pickaxe.")
				.tools("8 Obsidian", "1 Eye of Ender", "Diamond Pickaxe")
				.task(new CraftTask("ender_chest", "Craft an Ender Chest", 1, Items.ENDER_CHEST)));

		addMain(Quest.builder("p5_03_stronghold", QuestPhase.PHASE_5)
				.title("The Stronghold")
				.desc("Buried, mossy, and guarded by a silverfish Monster Spawner.")
				.icon(Items.END_PORTAL_FRAME)
				.guide("Underground, the first ring roughly 1280-2816 blocks from spawn. Throw an eye, "
						+ "walk to where it lands, repeat - when one flies downward you are on top of "
						+ "it. Break the silverfish Monster Spawner immediately.")
				.tools("Eyes of Ender", "Torches", "Pickaxe")
				.task(new StructureTask("stronghold", "Find a Stronghold", OVERWORLD,
								StructureFeature.STRONGHOLD),
						new CheckmarkTask("library", "Loot the stronghold library")));

		addMain(Quest.builder("p5_04_into_the_end", QuestPhase.PHASE_5)
				.title("Into the End")
				.desc("One way in. Only two ways out.")
				.icon(Items.END_STONE)
				.guide("Fill all 12 frames. Wear a carved pumpkin to walk past endermen. You cannot "
						+ "respawn here, so whatever you die with stays on the island until you come "
						+ "back for it.")
				.tools("Carved Pumpkin", "Building Blocks", "Water Bucket")
				.task(new StructureTask("end", "Enter The End", END, Blocks.END_STONE),
						new ItemTask("end_stone", "Collect End Stone", 16, Items.END_STONE)));

		addMain(Quest.builder("p5_05_dragon_slayer", QuestPhase.PHASE_5)
				.title("Dragon Slayer")
				.desc("The credits roll. The game does not end.")
				.icon(Items.DRAGON_BREATH)
				.guide("Destroy every end crystal first or she heals - caged ones need a bow or a "
						+ "thrown snowball. Melee her when she perches. Carry a water bucket for the "
						+ "knockback and never stand on the portal rim.")
				.tools("Bow", "Diamond Sword", "Water Bucket", "Golden Apples", "Carved Pumpkin")
				.task(new KillTask("dragon", "Defeat the Ender Dragon", 1, EntityType.ENDER_DRAGON),
						new ItemTask("breath", "Collect Dragon's Breath", 1, Items.DRAGON_BREATH)));

		addMain(Quest.builder("p5_06_the_egg", QuestPhase.PHASE_5)
				.title("The Egg")
				.desc("One per world. Ever.")
				.icon(Items.DRAGON_EGG)
				.guide("Punching it teleports it up to 15 blocks. Break the block underneath so it "
						+ "falls onto a torch, or push it with a piston. Do not let it drop into the "
						+ "void.")
				.tools("Torch", "Piston or Shovel")
				.task(new ItemTask("egg", "Obtain the Dragon Egg", 1, Items.DRAGON_EGG)));

		addMain(Quest.builder("p5_07_enderman_farm", QuestPhase.PHASE_5)
				.title("The Enderman Farm")
				.desc("The fastest XP in the game, by a long way.")
				.icon(Items.EXPERIENCE_BOTTLE)
				.guide("Build a platform on the main End island above Y=100 - endermen spawn on end "
						+ "stone in complete darkness at enormous rates. Lure them with an endermite "
						+ "in a minecart and let them fall 43 blocks.")
				.tools("Building Blocks", "Carved Pumpkin", "Water Bucket", "Ender Pearls")
				.task(new KillTask("endermen", "Kill Endermen", 100, EntityType.ENDERMAN),
						new ItemTask("pearls", "Stockpile Ender Pearls", 32, Items.ENDER_PEARL),
						new CheckmarkTask("xp_farm", "Build an enderman XP farm")));

		addMain(Quest.builder("p5_08_end_city", QuestPhase.PHASE_5)
				.title("The Outer Islands")
				.desc("Everything worth having is 1000 blocks past the portal.")
				.icon(Items.PURPUR_BLOCK)
				.guide("Throw a pearl into the gateway that appears after the dragon dies. Shulkers "
						+ "cause levitation, which is fatal over the void - fight them from inside a "
						+ "doorway and always carry a water bucket.")
				.tools("Ender Pearls", "Water Bucket", "Building Blocks", "Bow")
				.task(new StructureTask("city", "Find an End City", END,
								StructureFeature.ENDCITY),
						new ItemTask("rods", "Collect End Rods", 8, Items.END_ROD)));

		addMain(Quest.builder("p5_09_chorus_harvest", QuestPhase.PHASE_5)
				.title("The Chorus Harvest")
				.desc("The only crop that teleports you when you eat it.")
				.icon(Items.CHORUS_FRUIT)
				.guide("Break the bottom block of a chorus plant and the whole thing drops. Smelt the "
						+ "fruit into Popped Chorus Fruit; 4 popped makes 1 purpur block. It replants "
						+ "on end stone anywhere.")
				.tools("Any Tool", "Furnace")
				.task(new ItemTask("chorus", "Collect Chorus Fruit", 16, Items.CHORUS_FRUIT),
						new ItemTask("popped", "Smelt Popped Chorus Fruit", 8,
								Items.POPPED_CHORUS_FRUIT),
						new CraftTask("purpur", "Craft Purpur Blocks", 4, Items.PURPUR_BLOCK)));

		addMain(Quest.builder("p5_10_elytra", QuestPhase.PHASE_5)
				.title("Wings")
				.desc("The last item that changes how you play.")
				.icon(Items.ELYTRA)
				.guide("In an item frame at the back of the End Ship, guarded by shulkers. Break the "
						+ "FRAME, not the block behind it. Repair with phantom membranes on an anvil "
						+ "and put Unbreaking III and Mending on them.")
				.tools("Ender Pearls", "Water Bucket", "Building Blocks", "Bow")
				.task(new ItemTask("elytra", "Obtain Elytra", 1, Items.ELYTRA),
						new CheckmarkTask("repaired", "Put Mending or Unbreaking on your Elytra")));

		addMain(Quest.builder("p5_11_rocket_science", QuestPhase.PHASE_5)
				.title("Rocket Science")
				.desc("Wings without rockets is just falling with style.")
				.icon(Items.FIREWORK_ROCKET)
				.guide("1 paper + 1 gunpowder = 3 rockets, and extra gunpowder means longer flight. "
						+ "Your creeper farm makes this infinite.")
				.tools("Gunpowder", "Paper", "Elytra")
				.task(new ItemTask("gunpowder", "Collect Gunpowder", 12, Items.GUNPOWDER),
						new CraftTask("rockets", "Craft Firework Rockets", 24, Items.FIREWORK_ROCKET),
						new CheckmarkTask("flight", "Fly 1000 blocks in one continuous flight")));

		addMain(Quest.builder("p5_12_shulker_boxes", QuestPhase.PHASE_5)
				.title("Portable Storage")
				.desc("27 slots that survive being mined.")
				.icon(Items.SHULKER_BOX)
				.guide("2 shells + 1 chest. Shells drop 50% of the time, 100% with Looting III. "
						+ "Killing a shulker can spawn another, so leave one alive and the city keeps "
						+ "producing.")
				.tools("Looting III Sword", "Water Bucket")
				.task(new KillTask("shulkers", "Kill Shulkers", 6, EntityType.SHULKER),
						new ItemTask("shells", "Collect Shulker Shells", 4, Items.SHULKER_SHELL),
						new CraftTask("box", "Craft a Shulker Box", 2, Items.SHULKER_BOX)));

		addMain(Quest.builder("p5_13_second_city", QuestPhase.PHASE_5)
				.title("The Second City")
				.desc("One city is luck. Three is a supply line.")
				.icon(Items.END_STONE_BRICKS)
				.guide("End cities generate in dense clusters across the outer islands. Fly along a "
						+ "gateway's heading and you will cross several in a single trip. Every ship "
						+ "carries another Elytra.")
				.tools("Elytra", "Firework Rockets", "Shulker Boxes", "Ender Chest")
				.task(new CheckmarkTask("three_cities", "Loot three separate End Cities"),
						new ItemTask("spare_elytra", "Carry a spare Elytra", 1, Items.ELYTRA)));

		addMain(Quest.builder("p5_14_shulker_farm", QuestPhase.PHASE_5)
				.title("The Shulker Farm")
				.desc("Boxes on demand instead of boxes on luck.")
				.icon(Items.SHULKER_SHELL)
				.guide("A shulker that teleports has a chance to duplicate. Trap two in a small "
						+ "chamber with room to teleport and you have a renewable shell supply - the "
						+ "only one in the game.")
				.tools("Building Blocks", "Elytra", "Looting III Sword")
				.task(new ItemTask("many_shells", "Stockpile Shulker Shells", 16,
								Items.SHULKER_SHELL),
						new CraftTask("many_boxes", "Craft Shulker Boxes", 8, Items.SHULKER_BOX),
						new CheckmarkTask("shulker_farm", "Build a shulker duplication farm")));

		addMain(Quest.builder("p5_15_full_netherite", QuestPhase.PHASE_5)
				.title("Ascended")
				.desc("Every plate and every tool, upgraded.")
				.icon(Items.NETHERITE_CHESTPLATE)
				.guide("8 more netherite ingots: 32 ancient debris and 32 gold. Netherite gear does "
						+ "not burn in lava or fire, so your kit survives even when you do not. "
						+ "Enchant everything to its final state before upgrading.")
				.tools("Ancient Debris", "Gold Ingots", "Smithing Table", "Anvil")
				.task(new CraftTask("n_helm", "Upgrade a Netherite Helmet", 1, Items.NETHERITE_HELMET),
						new CraftTask("n_chest", "Upgrade a Netherite Chestplate", 1,
								Items.NETHERITE_CHESTPLATE),
						new CraftTask("n_legs", "Upgrade Netherite Leggings", 1,
								Items.NETHERITE_LEGGINGS),
						new CraftTask("n_boots", "Upgrade Netherite Boots", 1, Items.NETHERITE_BOOTS),
						new CraftTask("n_sword", "Upgrade a Netherite Sword", 1,
								Items.NETHERITE_SWORD),
						new CraftTask("n_axe", "Upgrade a Netherite Axe", 1, Items.NETHERITE_AXE)));

		addMain(Quest.builder("p5_16_lodestone", QuestPhase.PHASE_5)
				.title("The Lodestone")
				.desc("A compass that points where you tell it to.")
				.icon(Items.LODESTONE)
				.guide("1 netherite ingot + 8 chiselled stone bricks. Right-click a compass on it and "
						+ "that compass points at the lodestone from any dimension - the only reliable "
						+ "way to find your way home in the Nether or the End.")
				.tools("Netherite Ingot", "Chiselled Stone Bricks", "Compass")
				.task(new CraftTask("lodestone", "Craft a Lodestone", 1, Items.LODESTONE),
						new CheckmarkTask("bound", "Bind a compass to a lodestone")));

		addMain(Quest.builder("p5_17_end_colonist", QuestPhase.PHASE_5)
				.title("The End Colonist")
				.desc("Somewhere to come back to on the other side.")
				.icon(Items.END_STONE_BRICKS)
				.guide("An outer-island base with an ender chest, a furnace and a bed's worth of "
						+ "storage saves the 1000 block trip every time. There is no respawning out "
						+ "here, so build it walled and lit.")
				.tools("Building Blocks", "Elytra", "Ender Chest")
				.task(new CraftTask("bricks", "Craft End Stone Bricks", 32, Items.END_STONE_BRICKS),
						new CheckmarkTask("end_base", "Build a permanent base in the End")));

		addMain(Quest.builder("p5_18_dragon_rematch", QuestPhase.PHASE_5)
				.title("Dragon Rematch")
				.desc("She comes back for as long as you keep paying.")
				.icon(Items.END_CRYSTAL)
				.guide("4 end crystals on the exit portal edges respawns her. Each crystal is 7 glass "
						+ "+ 1 eye of ender + 1 ghast tear. Every respawn drops 500 XP and opens "
						+ "another gateway to the outer islands.")
				.tools("End Crystals", "Ghast Tears", "Netherite Gear")
				.task(new CraftTask("crystals", "Craft End Crystals", 4, Items.END_CRYSTAL),
						new KillTask("dragons", "Defeat the Ender Dragon (total)", 2,
								EntityType.ENDER_DRAGON)));
	}
}
