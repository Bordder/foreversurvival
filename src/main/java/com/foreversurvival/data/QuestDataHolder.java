package com.foreversurvival.data;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Duck-typing interface implemented on {@code ServerPlayerEntity} by our mixin.
 * Gives every server player its own {@link PlayerQuestData} instance that is
 * serialised as part of the vanilla player NBT.
 */
public interface QuestDataHolder {

	PlayerQuestData foreversurvival$getQuestData();

	static PlayerQuestData get(ServerPlayerEntity player) {
		return ((QuestDataHolder) player).foreversurvival$getQuestData();
	}
}
