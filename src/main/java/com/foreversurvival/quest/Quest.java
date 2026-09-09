package com.foreversurvival.quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.foreversurvival.quest.task.QuestTask;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A single quest definition. Immutable and shared by both logical sides, so the
 * client can render titles, guides and tool lists without any extra networking.
 */
public class Quest {

	private final String id;
	private final String title;
	private final String description;
	private final Item icon;
	/** Parent that must be 100% complete before this quest is even clickable. */
	@Nullable
	private final String parentId;
	/** Static, hand-written 1.18.2 instructions. Never generated at runtime. */
	private final String guideText;
	/** Explicit "bring this with you" list shown above the guide. */
	private final List<String> requiredTools;
	private final List<QuestTask> tasks;
	private final QuestPhase phase;

	private Quest(Builder builder) {
		this.id = builder.id;
		this.title = builder.title;
		this.description = builder.description;
		this.icon = builder.icon;
		this.parentId = builder.parentId;
		this.guideText = builder.guideText;
		this.requiredTools = Collections.unmodifiableList(new ArrayList<>(builder.requiredTools));
		this.tasks = Collections.unmodifiableList(new ArrayList<>(builder.tasks));
		this.phase = builder.phase;
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public Item getIcon() {
		return icon;
	}

	public ItemStack getIconStack() {
		return new ItemStack(icon);
	}

	@Nullable
	public String getParentId() {
		return parentId;
	}

	public String getGuideText() {
		return guideText;
	}

	public List<String> getRequiredTools() {
		return requiredTools;
	}

	public List<QuestTask> getTasks() {
		return tasks;
	}

	public QuestPhase getPhase() {
		return phase;
	}

	public boolean hasManualTask() {
		for (QuestTask task : tasks) {
			if (task.isManual()) {
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------

	public static Builder builder(String id, QuestPhase phase) {
		return new Builder(id, phase);
	}

	public static class Builder {

		private final String id;
		private final QuestPhase phase;
		private String title = "Untitled";
		private String description = "";
		private Item icon = Items.PAPER;
		private String parentId = null;
		private String guideText = "";
		private final List<String> requiredTools = new ArrayList<>();
		private final List<QuestTask> tasks = new ArrayList<>();

		private Builder(String id, QuestPhase phase) {
			this.id = id;
			this.phase = phase;
		}

		public Builder title(String title) {
			this.title = title;
			return this;
		}

		public Builder desc(String description) {
			this.description = description;
			return this;
		}

		public Builder icon(Item icon) {
			this.icon = icon;
			return this;
		}

		public Builder parent(@Nullable String parentId) {
			this.parentId = parentId;
			return this;
		}

		public Builder guide(String guideText) {
			this.guideText = guideText;
			return this;
		}

		public Builder tools(String... tools) {
			Collections.addAll(this.requiredTools, tools);
			return this;
		}

		public Builder task(QuestTask... tasks) {
			Collections.addAll(this.tasks, tasks);
			return this;
		}

		public Quest build() {
			return new Quest(this);
		}
	}
}
