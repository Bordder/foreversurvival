package com.foreversurvival.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.foreversurvival.ForeverSurvival;
import com.foreversurvival.data.PlayerQuestData;
import com.foreversurvival.data.QuestDataHolder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Attaches a {@link PlayerQuestData} to every server player and hooks it into
 * the vanilla player save/load path, which is exactly "saving to player NBT".
 */
@Mixin(ServerPlayer.class)
public class ServerPlayerEntityMixin implements QuestDataHolder {

	@Unique
	private final PlayerQuestData foreversurvival$questData = new PlayerQuestData();

	@Override
	public PlayerQuestData foreversurvival$getQuestData() {
		return foreversurvival$questData;
	}

	// 26.2 saves through ValueOutput/ValueInput rather than a raw CompoundTag,
	// so the tag goes through CompoundTag.CODEC. The stored shape is unchanged.
	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void foreversurvival$write(ValueOutput output, CallbackInfo ci) {
		output.store(ForeverSurvival.NBT_ROOT_KEY, CompoundTag.CODEC,
				foreversurvival$questData.writeNbt());
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void foreversurvival$read(ValueInput input, CallbackInfo ci) {
		input.read(ForeverSurvival.NBT_ROOT_KEY, CompoundTag.CODEC)
				.ifPresent(foreversurvival$questData::readNbt);
	}
}
