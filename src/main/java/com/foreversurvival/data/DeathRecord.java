package com.foreversurvival.data;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

/**
 * One recorded death: where it happened, what killed you, and what you were
 * carrying at the time.
 *
 * Purely informational - the mod never renders a marker for it. The coordinates
 * only ever show up as text in the "Deaths" tab, and the inventory snapshot only
 * when you open that death.
 */
public class DeathRecord {

	private final int x;
	private final int y;
	private final int z;
	/** Full dimension identifier, e.g. "minecraft:overworld". */
	private final String dimension;
	/** World time (game ticks) at the moment of death, used only for ordering. */
	private final long time;
	/** The vanilla death message, e.g. "Steve fell from a high place". */
	private final String cause;

	/**
	 * Inventory contents at the moment of death, in vanilla PlayerInventory
	 * format. Dropped from older records to keep player NBT from ballooning.
	 */
	@Nullable
	private NbtList inventory;

	public DeathRecord(int x, int y, int z, String dimension, long time, String cause,
			@Nullable NbtList inventory) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.dimension = dimension;
		this.time = time;
		this.cause = cause == null ? "" : cause;
		this.inventory = inventory;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	public String getDimension() {
		return dimension;
	}

	public long getTime() {
		return time;
	}

	public String getCause() {
		return cause;
	}

	@Nullable
	public NbtList getInventory() {
		return inventory;
	}

	public boolean hasInventory() {
		return inventory != null && !inventory.isEmpty();
	}

	/** Called on older records so long-lived worlds do not bloat player NBT. */
	public void clearInventory() {
		this.inventory = null;
	}

	/** Turns "minecraft:the_nether" into "The Nether" for display purposes. */
	public String getDimensionDisplayName() {
		String name = dimension;
		int colon = name.indexOf(':');
		if (colon >= 0) {
			name = name.substring(colon + 1);
		}

		StringBuilder out = new StringBuilder();
		for (String part : name.split("_")) {
			if (part.isEmpty()) {
				continue;
			}
			if (out.length() > 0) {
				out.append(' ');
			}
			out.append(Character.toUpperCase(part.charAt(0)));
			out.append(part.substring(1));
		}
		return out.toString();
	}

	public NbtCompound writeNbt() {
		NbtCompound nbt = new NbtCompound();
		nbt.putInt("X", x);
		nbt.putInt("Y", y);
		nbt.putInt("Z", z);
		nbt.putString("Dim", dimension);
		nbt.putLong("Time", time);
		nbt.putString("Cause", cause);
		if (inventory != null && !inventory.isEmpty()) {
			nbt.put("Inv", inventory);
		}
		return nbt;
	}

	public static DeathRecord fromNbt(NbtCompound nbt) {
		NbtList inventory = null;
		if (nbt.contains("Inv", NbtElement.LIST_TYPE)) {
			inventory = nbt.getList("Inv", NbtElement.COMPOUND_TYPE);
		}

		return new DeathRecord(
				nbt.getInt("X"),
				nbt.getInt("Y"),
				nbt.getInt("Z"),
				nbt.getString("Dim"),
				nbt.getLong("Time"),
				nbt.getString("Cause"),
				inventory);
	}
}
