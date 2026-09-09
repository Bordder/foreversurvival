package com.foreversurvival.death;

import com.foreversurvival.data.DeathRecord;
import com.foreversurvival.data.PlayerQuestData;
import com.foreversurvival.data.QuestDataHolder;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.nbt.ListTag;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;

/**
 * Records where every death happened, what caused it, and what was in the
 * player's inventory at the time.
 *
 * Deliberately text-only in the world: everything is written into player NBT
 * and shown in the "Deaths" tab. There is no HUD element, no waypoint, no
 * particle and no compass - finding the spot again is part of the game.
 */
public final class DeathLogger {

	private DeathLogger() {
	}

	public static void register() {
		// ALLOW_DEATH is the only death hook available in the 1.18.2 Fabric API.
		// It fires after the vanilla totem check, so a Totem of Undying save is
		// never logged as a death. We only observe here and always return true:
		// the death itself is never vetoed.
		ServerPlayerEvents.ALLOW_DEATH.register((player, damageSource, damageAmount) -> {
			BlockPos pos = player.blockPosition();
			String dimension = player.level().dimension().location().toString();

			String cause;
			try {
				cause = damageSource.getLocalizedDeathMessage(player).getString();
			} catch (Exception e) {
				// A modded or unusual damage source should never lose the record.
				cause = damageSource.getMsgId();
			}

			// Snapshot before vanilla scatters everything on the floor.
			ListTag inventory = player.getInventory().writeNbt(new ListTag());

			PlayerQuestData data = QuestDataHolder.get(player);
			DeathRecord record = new DeathRecord(pos.getX(), pos.getY(), pos.getZ(), dimension,
					player.level().getLevelData().getGameTime(), cause, inventory);
			data.addDeath(record);

			int index = data.getDeaths().size();
			player.sendSystemMessage(Component.literal("[ForeverSurvival] ").withStyle(ChatFormatting.DARK_AQUA)
					.append(Component.literal("Death #" + index + " logged: "
							+ record.getX() + ", " + record.getY() + ", " + record.getZ()
							+ " [" + record.getDimensionDisplayName() + "]").withStyle(ChatFormatting.RED)));

			return true;
		});
	}
}
