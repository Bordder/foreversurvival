package com.foreversurvival.death;

import com.foreversurvival.data.DeathRecord;
import com.foreversurvival.data.PlayerQuestData;
import com.foreversurvival.data.QuestDataHolder;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
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
			String dimension = player.level().dimension().identifier().toString();

			String cause;
			try {
				cause = damageSource.getLocalizedDeathMessage(player).getString();
			} catch (Exception e) {
				// A modded or unusual damage source should never lose the record.
				cause = damageSource.getMsgId();
			}

			// Snapshot before vanilla scatters everything on the floor.
			//
			// 26.2 stores item data as components, so a stack no longer has a
			// plain NBT form: it has to go through a codec with registry access
			// to resolve the component types. Inventory.save now wants a
			// ValueOutput list, so the entries are written here directly, which
			// also keeps DeathRecord's stored shape under this mod's control.
			RegistryOps<Tag> ops = player.registryAccess()
					.createSerializationContext(NbtOps.INSTANCE);
			Inventory playerInventory = player.getInventory();
			ListTag inventory = new ListTag();

			// 0-35 main, 36-39 armour, 40 offhand. The slots above that are
			// mount equipment and are not the player's to lose.
			for (int slot = 0; slot <= Inventory.SLOT_OFFHAND; slot++) {
				ItemStack stack = playerInventory.getItem(slot);
				if (stack.isEmpty()) {
					continue;
				}
				ItemStackWithSlot.CODEC
						.encodeStart(ops, new ItemStackWithSlot(slot, stack))
						.result()
						.ifPresent(inventory::add);
			}

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
