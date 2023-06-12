package com.ota.update.entities;

/**
 * The response of a poll request for a car. It consists of a boolean flag which defines whether a new update is avaiable or not.
 */
public class PollResponse {
	private boolean availability;

	public boolean isAvailability() {
		return availability;
	}

	public void setAvailability(boolean availability) {
		this.availability = availability;
	}

	public PollResponse(boolean availability){
		this.availability=availability;
	}
}
