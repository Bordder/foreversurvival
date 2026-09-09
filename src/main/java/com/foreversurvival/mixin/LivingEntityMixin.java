package com.foreversurvival.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.foreversurvival.data.PlayerQuestData;
import com.foreversurvival.data.QuestDataHolder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Registry;

/**
 * Records how much damage the player deals to each mob type, for the Combat Log
 * in the Stats tab.
 *
 * The hook is on the victim's {@code damage} call: {@code this} is the mob being
 * hit and {@code source.getAttacker()} is who hit it. Server side only, and only
 * when the attacker is a player. The raw incoming amount is stored (pre-armour),
 * which is the "hit for X" number players expect.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

	@Inject(method = "damage", at = @At("HEAD"))
	private void foreversurvival$recordDamage(DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self.level().isClientSide() || amount <= 0.0F) {
			return;
		}

		Entity attacker = source.getAttacker();
		if (!(attacker instanceof ServerPlayer player)) {
			return;
		}

		String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(self.getType()).toString();
		PlayerQuestData data = QuestDataHolder.get(player);
		data.addDamageDealt(typeId, amount);
	}
}
