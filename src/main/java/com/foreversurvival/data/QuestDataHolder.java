package com.foreversurvival.data;

import net.minecraft.server.level.ServerPlayer;

/**
 * Duck-typing interface implemented on {@code ServerPlayer} by our mixin.
 * Gives every server player its own {@link PlayerQuestData} instance that is
 * serialised as part of the vanilla player NBT.
 */
public interface QuestDataHolder {

	PlayerQuestData foreversurvival$getQuestData();

	static PlayerQuestData get(ServerPlayer player) {
		return ((QuestDataHolder) player).foreversurvival$getQuestData();
	}
}
