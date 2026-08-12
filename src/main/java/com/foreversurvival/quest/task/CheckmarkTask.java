package com.foreversurvival.quest.task;

/**
 * Manual objective for goals the game cannot measure ("build something you are
 * proud of", "visit every biome").
 *
 * The value only ever changes when the player presses the checkmark button in
 * the quest screen, which sends a C2S packet. The server still refuses the
 * request unless the quest is actually unlocked, so this cannot be used to skip
 * the progression chain.
 */
public class CheckmarkTask extends QuestTask {

	public CheckmarkTask(String id, String description) {
		super(id, description, 1);
	}

	@Override
	public int computeProgress(TaskContext ctx) {
		// Nothing is auto-detected: keep whatever the player already confirmed.
		return ctx.getStoredProgress();
	}

	@Override
	public String getTypeLabel() {
		return "Confirm";
	}

	@Override
	public boolean isManual() {
		return true;
	}
}
