package com.ota.update.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Delta flag, used to define if the service uses delta dissemination for the rollout. ID is needed to save it in the database.
 */
@Document("deltas")
public class DeltaFlag {

	@Id
	private int id;
	
	private boolean delta;

	public DeltaFlag(int id, boolean delta) {
		this.id = id;
		this.delta = delta;
	}

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public boolean isDelta() {
		return delta;
	}
	public void setDelta(boolean delta) {
		this.delta = delta;
	}
}
