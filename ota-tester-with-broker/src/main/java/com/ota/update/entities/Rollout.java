package com.ota.update.entities;

// import java.sql.Timestamp;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Class used to model continuous rollouts. It is defined by the group's ID this
 * rollout effects, as well as the image ID that is used to update the group.
 * The rollout type is defined either as push or optional.
 */
@Document("rollouts")
public class Rollout {
	@Id
	@Field("groupId")
	private int groupId;
	private String imageId;
	// private Timestamp rolloutStartTime;
	private RolloutType rolloutType;

	public Rollout(int groupId, String imageId, /* Timestamp rolloutStartTime, */RolloutType rolloutType) {
		this.groupId = groupId;
		this.imageId = imageId;
		// this.rolloutStartTime = rolloutStartTime;
		this.rolloutType = rolloutType;
	}

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public String getImageId() {
		return imageId;
	}

	public void setImageId(String imageId) {
		this.imageId = imageId;
	}

	// public Timestamp getRolloutStartTime() {
	// return rolloutStartTime;
	// }

	// public void setRolloutStartTime(Timestamp rolloutStartTime) {
	// this.rolloutStartTime = rolloutStartTime;
	// }

	public RolloutType getRolloutType() {
		return rolloutType;
	}

	public void setRolloutType(RolloutType rolloutType) {
		this.rolloutType = rolloutType;
	}

}
