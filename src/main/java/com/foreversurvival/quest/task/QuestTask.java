package com.foreversurvival.quest.task;

/**
 * Base class for every objective type.
 *
 * A task is evaluated server side only. The client receives nothing but the
 * resulting integer progress, which keeps the sync packet tiny.
 */
public abstract class QuestTask {

	protected final String id;
	protected final String description;
	protected final int required;

	protected QuestTask(String id, String description, int required) {
		this.id = id;
		this.description = description;
		this.required = Math.max(1, required);
	}

	public String getId() {
		return id;
	}

	public String getDescription() {
		return description;
	}

	public int getRequired() {
		return required;
	}

	/**
	 * @return the raw progress value for this tick. The manager clamps it to
	 *         {@link #getRequired()} and keeps the highest value ever seen.
	 */
	public abstract int computeProgress(TaskContext ctx);

	/** Short type tag shown in the GUI, e.g. "Collect", "Craft", "Kill". */
	public abstract String getTypeLabel();

	/** True when only the player can decide this is done (checkmark button). */
	public boolean isManual() {
		return false;
	}
}
