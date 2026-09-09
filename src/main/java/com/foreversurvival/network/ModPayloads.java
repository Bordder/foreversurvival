package com.foreversurvival.network;

import java.util.List;

import com.foreversurvival.ForeverSurvival;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * The three packets as payload records.
 *
 * 26.2 removed raw PacketByteBuf sends: a packet is now a typed record with a
 * StreamCodec, registered up front so both sides agree on the wire format.
 */
public final class ModPayloads {

	private ModPayloads() {
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ForeverSurvival.MOD_ID, path);
	}

	/** S2C: the player's whole quest state, plus the stats the Stats tab shows. */
	public record SyncData(CompoundTag root) implements CustomPacketPayload {
		public static final Type<SyncData> TYPE = new Type<>(id("sync_data"));

		public static final StreamCodec<RegistryFriendlyByteBuf, SyncData> CODEC =
				StreamCodec.composite(
						ByteBufCodecs.COMPOUND_TAG, SyncData::root,
						SyncData::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** S2C: every online player's position, for the locator bar. */
	public record PlayerLocations(List<Entry> entries) implements CustomPacketPayload {
		public static final Type<PlayerLocations> TYPE = new Type<>(id("player_locations"));

		public record Entry(String name, double x, double y, double z, String dimension) {
			public static final StreamCodec<RegistryFriendlyByteBuf, Entry> CODEC =
					StreamCodec.composite(
							ByteBufCodecs.STRING_UTF8, Entry::name,
							ByteBufCodecs.DOUBLE, Entry::x,
							ByteBufCodecs.DOUBLE, Entry::y,
							ByteBufCodecs.DOUBLE, Entry::z,
							ByteBufCodecs.STRING_UTF8, Entry::dimension,
							Entry::new);
		}

		public static final StreamCodec<RegistryFriendlyByteBuf, PlayerLocations> CODEC =
				StreamCodec.composite(
						Entry.CODEC.apply(ByteBufCodecs.list()), PlayerLocations::entries,
						PlayerLocations::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** C2S: "I finished this manual objective". */
	public record Checkmark(String questId, String taskId, boolean set)
			implements CustomPacketPayload {
		public static final Type<Checkmark> TYPE = new Type<>(id("checkmark"));

		public static final StreamCodec<RegistryFriendlyByteBuf, Checkmark> CODEC =
				StreamCodec.composite(
						ByteBufCodecs.STRING_UTF8, Checkmark::questId,
						ByteBufCodecs.STRING_UTF8, Checkmark::taskId,
						ByteBufCodecs.BOOL, Checkmark::set,
						Checkmark::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
