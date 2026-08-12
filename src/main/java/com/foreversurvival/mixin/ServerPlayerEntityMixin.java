package com.foreversurvival.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.foreversurvival.ForeverSurvival;
import com.foreversurvival.data.PlayerQuestData;
import com.foreversurvival.data.QuestDataHolder;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Attaches a {@link PlayerQuestData} to every server player and hooks it into
 * the vanilla player save/load path, which is exactly "saving to player NBT".
 */
@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements QuestDataHolder {

	@Unique
	private final PlayerQuestData foreversurvival$questData = new PlayerQuestData();

	@Override
	public PlayerQuestData foreversurvival$getQuestData() {
		return foreversurvival$questData;
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	private void foreversurvival$write(NbtCompound nbt, CallbackInfo ci) {
		nbt.put(ForeverSurvival.NBT_ROOT_KEY, foreversurvival$questData.writeNbt());
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	private void foreversurvival$read(NbtCompound nbt, CallbackInfo ci) {
		if (nbt.contains(ForeverSurvival.NBT_ROOT_KEY, NbtElement.COMPOUND_TYPE)) {
			foreversurvival$questData.readNbt(nbt.getCompound(ForeverSurvival.NBT_ROOT_KEY));
		}
	}
}
