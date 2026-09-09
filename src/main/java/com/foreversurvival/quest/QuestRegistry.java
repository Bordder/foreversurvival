package com.foreversurvival.quest;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Shared plumbing for the quest tree. The quests themselves live in the
 * per-phase classes so no single file becomes unmanageable.
 *
 * DESIGN RULES for anything added here:
 *
 *  1. Every quest is a real milestone of a Minecraft survival world - something
 *     a player would naturally do on the way from a punched tree to a finished
 *     world. No invented busywork, no filler collections.
 *  2. There are NO rewards. No items, no XP. The mod tells you what to do next;
 *     vanilla provides everything you get.
 *  3. An objective asks for what the next step actually consumes plus a small
 *     buffer. Length comes from the NUMBER of milestones, never from inflated
 *     stack counts.
 *  4. Guides are static, hand-written for 1.18.2, and short: where to go, what
 *     tool you need, and the one thing that will kill you.
 *
 * Main quests form one strict chain - each quest's parent is the quest declared
 * immediately before it, across phase boundaries too. Side challenges have no
 * parent and never block the main line.
 */
public final class QuestRegistry {

	// ------------------------------------------------------------------
	// Shared item groups
	// ------------------------------------------------------------------

	static final Item[] LOGS = {
			Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG, Items.JUNGLE_LOG,
			Items.ACACIA_LOG, Items.DARK_OAK_LOG
	};

	static final Item[] PLANKS = {
			Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS, Items.JUNGLE_PLANKS,
			Items.ACACIA_PLANKS, Items.DARK_OAK_PLANKS, Items.CRIMSON_PLANKS, Items.WARPED_PLANKS
	};

	static final Item[] BEDS = {
			Items.WHITE_BED, Items.ORANGE_BED, Items.MAGENTA_BED, Items.LIGHT_BLUE_BED,
			Items.YELLOW_BED, Items.LIME_BED, Items.PINK_BED, Items.GRAY_BED,
			Items.LIGHT_GRAY_BED, Items.CYAN_BED, Items.PURPLE_BED, Items.BLUE_BED,
			Items.BROWN_BED, Items.GREEN_BED, Items.RED_BED, Items.BLACK_BED
	};

	static final Item[] WOOL = {
			Items.WHITE_WOOL, Items.ORANGE_WOOL, Items.MAGENTA_WOOL, Items.LIGHT_BLUE_WOOL,
			Items.YELLOW_WOOL, Items.LIME_WOOL, Items.PINK_WOOL, Items.GRAY_WOOL,
			Items.LIGHT_GRAY_WOOL, Items.CYAN_WOOL, Items.PURPLE_WOOL, Items.BLUE_WOOL,
			Items.BROWN_WOOL, Items.GREEN_WOOL, Items.RED_WOOL, Items.BLACK_WOOL
	};

	static final Item[] RAW_MEAT = {
			Items.BEEF, Items.PORKCHOP, Items.CHICKEN, Items.MUTTON, Items.RABBIT
	};

	static final Item[] COOKED_FOOD = {
			Items.COOKED_BEEF, Items.COOKED_PORKCHOP, Items.COOKED_CHICKEN, Items.COOKED_MUTTON,
			Items.COOKED_RABBIT, Items.COOKED_COD, Items.COOKED_SALMON, Items.BAKED_POTATO
	};

	static final Item[] MUSIC_DISCS = {
			Items.MUSIC_DISC_13, Items.MUSIC_DISC_CAT, Items.MUSIC_DISC_BLOCKS, Items.MUSIC_DISC_CHIRP,
			Items.MUSIC_DISC_FAR, Items.MUSIC_DISC_MALL, Items.MUSIC_DISC_MELLOHI, Items.MUSIC_DISC_STAL,
			Items.MUSIC_DISC_STRAD, Items.MUSIC_DISC_WARD, Items.MUSIC_DISC_11, Items.MUSIC_DISC_WAIT,
			Items.MUSIC_DISC_PIGSTEP, Items.MUSIC_DISC_OTHERSIDE
	};

	static final Item[] RAW_FISH = {
			Items.COD, Items.SALMON, Items.TROPICAL_FISH, Items.PUFFERFISH
	};

	static final Item[] BOATS = {
			Items.OAK_BOAT, Items.SPRUCE_BOAT, Items.BIRCH_BOAT, Items.JUNGLE_BOAT,
			Items.ACACIA_BOAT, Items.DARK_OAK_BOAT
	};

	static final Item[] FENCES = {
			Items.OAK_FENCE, Items.SPRUCE_FENCE, Items.BIRCH_FENCE, Items.JUNGLE_FENCE,
			Items.ACACIA_FENCE, Items.DARK_OAK_FENCE
	};

	static final Item[] HOES = {
			Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.DIAMOND_HOE
	};

	static final Item[] SAPLINGS = {
			Items.OAK_SAPLING, Items.SPRUCE_SAPLING, Items.BIRCH_SAPLING, Items.JUNGLE_SAPLING,
			Items.ACACIA_SAPLING, Items.DARK_OAK_SAPLING
	};

	static final String OVERWORLD = "minecraft:overworld";
	static final String NETHER = "minecraft:the_nether";
	static final String END = "minecraft:the_end";

	/** Rolling parent pointer that welds the main line into one strict chain. */
	private static String previousMainId = null;

	private QuestRegistry() {
	}

	/** Registers a main-line quest, chaining it behind the previous one. */
	static void addMain(Quest.Builder builder) {
		Quest quest = builder.parent(previousMainId).build();
		QuestManager.get().register(quest);
		previousMainId = quest.getId();
	}

	/** Registers an optional challenge. Never has a parent, never blocks anything. */
	static void addSide(Quest.Builder builder) {
		QuestManager.get().register(builder.parent(null).build());
	}

	public static void registerAll() {
		previousMainId = null;

		Phase1Quests.register();
		Phase2Quests.register();
		Phase3Quests.register();
		Phase4Quests.register();
		Phase5Quests.register();
		Phase6Quests.register();
		SideQuests.register();
	}
}
