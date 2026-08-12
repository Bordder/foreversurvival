package com.foreversurvival.network;

/**
 * One other player's position, as broadcast to every client for the locator bar.
 *
 * The vanilla client only knows about entities inside its tracking range, so a
 * bar that works across the whole world has to be fed by the server.
 */
public class PlayerLocation {

	private final String name;
	private final double x;
	private final double y;
	private final double z;
	/** Full dimension identifier, e.g. "minecraft:overworld". */
	private final String dimension;

	public PlayerLocation(String name, double x, double y, double z, String dimension) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.dimension = dimension;
	}

	public String getName() {
		return name;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getZ() {
		return z;
	}

	public String getDimension() {
		return dimension;
	}
}
