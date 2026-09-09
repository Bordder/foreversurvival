package com.foreversurvival.quest;

import net.minecraft.ChatFormatting;

/**
 * The six main progression phases plus the parallel "Side Challenges" bucket.
 *
 * {@code major} phases fire the firework celebration when their last quest is
 * finished (Phases 3, 4, 5 and 6 - the milestones that actually feel earned).
 */
public enum QuestPhase {

	PHASE_1("phase1", "Phase 1: The Struggle", ChatFormatting.GRAY, false),
	PHASE_2("phase2", "Phase 2: The Establishment", ChatFormatting.WHITE, false),
	PHASE_3("phase3", "Phase 3: The Expansion", ChatFormatting.RED, true),
	PHASE_4("phase4", "Phase 4: The Mastery", ChatFormatting.GOLD, true),
	PHASE_5("phase5", "Phase 5: The Endgame", ChatFormatting.LIGHT_PURPLE, true),
	PHASE_6("phase6", "Phase 6: The Forever Goals", ChatFormatting.AQUA, true),
	SIDE("side", "Side Challenges", ChatFormatting.GREEN, false);

	private final String id;
	private final String displayName;
	private final ChatFormatting color;
	private final boolean major;

	QuestPhase(String id, String displayName, ChatFormatting color, boolean major) {
		this.id = id;
		this.displayName = displayName;
		this.color = color;
		this.major = major;
	}

	public String getId() {
		return id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public ChatFormatting getColor() {
		return color;
	}

	public boolean isMajor() {
		return major;
	}

	public boolean isSide() {
		return this == SIDE;
	}
}
