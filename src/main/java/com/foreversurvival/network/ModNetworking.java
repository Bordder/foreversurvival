package com.foreversurvival.network;

import java.util.List;

import com.foreversurvival.ForeverSurvival;
import com.foreversurvival.data.PlayerQuestData;
import com.foreversurvival.data.QuestDataHolder;
import com.foreversurvival.quest.QuestManager;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.resources.Identifier;

/**
 * Three packets:
 *  - S2C sync: the player's whole {@link PlayerQuestData} as NBT,
 *  - S2C locations: every online player's position, for the locator bar,
 *  - C2S checkmark: "I finished this manual objective".
 */
public final class ModNetworking {

	public static final Identifier SYNC_DATA = new Identifier(ForeverSurvival.MOD_ID, "sync_data");
	public static final Identifier PLAYER_LOCATIONS =
			new Identifier(ForeverSurvival.MOD_ID, "player_locations");
	public static final Identifier CHECKMARK = new Identifier(ForeverSurvival.MOD_ID, "checkmark");

	private ModNetworking() {
	}

	public static void registerServerReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(CHECKMARK, (server, player, handler, buf, responseSender) -> {
			String questId = buf.readString(128);
			String taskId = buf.readString(128);
			boolean set = buf.readBoolean();

			// Always bounce back onto the server thread before touching game state.
			server.execute(() -> QuestManager.get().handleCheckmark(player, questId, taskId, set));
		});
	}

	/** Pushes the full quest state to its owner. Progress stays per-player. */
	public static void syncToClient(ServerPlayer player) {
		PlayerQuestData data = QuestDataHolder.get(player);

		// Vanilla stats only reach the client when it explicitly asks for them,
		// so the handful the Stats tab shows ride along with the quest sync.
		CompoundTag root = new CompoundTag();
		root.put("Data", data.writeNbt());
		root.put("Stats", buildStats(player));

		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeNbt(root);

		ServerPlayNetworking.send(player, SYNC_DATA, buf);
		data.clearDirty();
	}

	private static CompoundTag buildStats(ServerPlayer player) {
		var handler = player.getStatHandler();
		CompoundTag stats = new CompoundTag();

		stats.putInt("PlayTime", handler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME)));
		stats.putInt("SinceDeath", handler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_DEATH)));
		stats.putInt("WalkCm", handler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.WALK_ONE_CM)));
		stats.putInt("SprintCm", handler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.SPRINT_ONE_CM)));
		stats.putInt("FlyCm", handler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.FLY_ONE_CM)));
		stats.putInt("Deaths", handler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.DEATHS)));
		stats.putInt("MobKills", handler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.MOB_KILLS)));
		stats.putInt("Jumps", handler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.JUMP)));
		stats.putInt("DamageTaken", handler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_TAKEN)));
		stats.putInt("Slept", handler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.SLEEP_IN_BED)));

		return stats;
	}

	/**
	 * Broadcasts every online player's position to every online player.
	 *
	 * Clients filter themselves out. Sent a few times a second - the bar only
	 * needs a direction, not a smooth interpolation.
	 */
	public static void syncPlayerLocations(MinecraftServer server) {
		List<ServerPlayer> players = server.getPlayerManager().getPlayerList();
		if (players.size() < 2) {
			// Nothing worth drawing when you are the only one online.
			return;
		}

		// A fresh buffer per recipient: a FriendlyByteBuf is released once sent,
		// so the same instance must never be handed to two sends.
		for (ServerPlayer recipient : players) {
			FriendlyByteBuf buf = PacketByteBufs.create();
			buf.writeVarInt(players.size());

			for (ServerPlayer player : players) {
				buf.writeString(player.getGameProfile().getName());
				buf.writeDouble(player.getX());
				buf.writeDouble(player.getY());
				buf.writeDouble(player.getZ());
				buf.writeString(player.world.getRegistryKey().getValue().toString());
			}

			ServerPlayNetworking.send(recipient, PLAYER_LOCATIONS, buf);
		}
	}
}
