package com.foreversurvival.quest.task;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.Block;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.core.BlockPos;
import java.util.function.Predicate;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * "Be somewhere" task, in two flavours.
 *
 * <p><b>Structure mode</b> asks the game directly whether the player is standing
 * inside a generated structure. This is exact: a Swamp Hut is a Swamp Hut, not
 * "somewhere with spruce planks and a cauldron", so your own base can never
 * satisfy it by accident.
 *
 * <p><b>Block mode</b> scans a small box around the player for a set of blocks,
 * and is used for the things that are not generated structures at all - a
 * player-built shelter, a lit nether portal, an amethyst geode, a cave biome.
 *
 * An optional dimension filter lets a task mean "simply be in the Nether".
 */
public class StructureTask extends QuestTask {

	private final Set<Block> blocks;
	/**
	 * Non-null in structure mode. 26.2 has no StructureFeature: structures are
	 * data-driven, so a task matches either one structure by key or a whole
	 * family by tag (VILLAGE covers all five village variants).
	 */
	@Nullable
	private final Predicate<Holder<Structure>> feature;
	/** Full dimension id, e.g. "minecraft:the_nether". Null means any. */
	@Nullable
	private final String dimension;

	/** Block-scan mode, for things the game does not model as a structure. */
	public StructureTask(String id, String description, @Nullable String dimension, Block... blocks) {
		super(id, description, 1);
		this.dimension = dimension;
		this.blocks = new LinkedHashSet<>(Arrays.asList(blocks));
		this.feature = null;
	}

	/** Structure mode, one specific structure. */
	public StructureTask(String id, String description, @Nullable String dimension,
			ResourceKey<Structure> structure) {
		super(id, description, 1);
		this.dimension = dimension;
		this.blocks = Set.of();
		this.feature = holder -> holder.is(structure);
	}

	/** Structure mode, a whole family - every village type, every mineshaft. */
	public StructureTask(String id, String description, @Nullable String dimension,
			TagKey<Structure> structures) {
		super(id, description, 1);
		this.dimension = dimension;
		this.blocks = Set.of();
		this.feature = holder -> holder.is(structures);
	}

	@Override
	public int computeProgress(TaskContext ctx) {
		ServerPlayer player = ctx.getPlayer();

		if (dimension != null) {
			String current = player.level().dimension().location().toString();
			if (!dimension.equals(current)) {
				return 0;
			}
		}

		if (feature != null) {
			return isInsideStructure(player) ? 1 : 0;
		}

		// A dimension-only task (no blocks listed) is done as soon as you arrive.
		if (blocks.isEmpty()) {
			return 1;
		}

		// Every listed block must be present in the scanned box at the same time.
		for (Block block : blocks) {
			if (!ctx.getNearbyBlocks().contains(block)) {
				return 0;
			}
		}
		return 1;
	}

	/**
	 * getStructureWithPieceAt is stricter than the old bounding-box check: it
	 * requires the player to be inside an actual generated piece, so standing in
	 * the empty air above a buried mineshaft no longer counts.
	 */
	private boolean isInsideStructure(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel world)) {
			return false;
		}

		BlockPos pos = player.blockPosition();
		StructureStart start = world.structureManager().getStructureWithPieceAt(pos, feature);
		return start != null && start.isValid();
	}

	/** Empty in structure mode - only block mode contributes to the world scan. */
	public Set<Block> getBlocks() {
		return blocks;
	}

	@Override
	public String getTypeLabel() {
		return "Locate";
	}
}
