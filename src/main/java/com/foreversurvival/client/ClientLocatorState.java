package com.foreversurvival.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.foreversurvival.network.PlayerLocation;

/**
 * The most recent set of player positions received from the server.
 * Replaced wholesale every packet; never interpolated.
 */
public final class ClientLocatorState {

	private static volatile List<PlayerLocation> locations = Collections.emptyList();

	private ClientLocatorState() {
	}

	public static List<PlayerLocation> get() {
		return locations;
	}

	public static void accept(List<PlayerLocation> incoming) {
		locations = new ArrayList<>(incoming);
	}

	public static void clear() {
		locations = Collections.emptyList();
	}
}
