package com.foreversurvival.quest.task;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;
import net.minecraft.world.gen.feature.StructureFeature;

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
	/** Non-null in structure mode. */
	@Nullable
	private final StructureFeature<?> feature;
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

	/** Structure mode - the accurate one. Use this whenever a feature exists. */
	public StructureTask(String id, String description, @Nullable String dimension,
			StructureFeature<?> feature) {
		super(id, description, 1);
		this.dimension = dimension;
		this.blocks = Set.of();
		this.feature = feature;
	}

	@Override
	public int computeProgress(TaskContext ctx) {
		ServerPlayerEntity player = ctx.getPlayer();

		if (dimension != null) {
			String current = player.world.getRegistryKey().getValue().toString();
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
	 * A StructureFeature can have several configured variants - five village
	 * types, several ruined portal types - and only the configured form can be
	 * looked up, so every variant of the wanted feature is checked.
	 */
	private boolean isInsideStructure(ServerPlayerEntity player) {
		if (!(player.world instanceof ServerWorld world)) {
			return false;
		}

		StructureAccessor accessor = world.getStructureAccessor();
		BlockPos pos = player.getBlockPos();

		for (ConfiguredStructureFeature<?, ?> configured
				: world.getRegistryManager().get(Registry.CONFIGURED_STRUCTURE_FEATURE_KEY)) {

			if (configured.feature != feature) {
				continue;
			}

			StructureStart start = accessor.getStructureAt(pos, configured);
			if (start != null && start != StructureStart.DEFAULT && start.hasChildren()) {
				return true;
			}
		}

		return false;
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
