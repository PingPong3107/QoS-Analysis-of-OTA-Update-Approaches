package com.ota.update.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A car is defined by an ID, the previously installed image ID, the currently installed image ID, and the ID of the group the car belongs to.
 */
@Document("cars")
public class Car {

	@Id
	private int id;
	private String prevImageId;
	private String curImageId;
	private int groupId;

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public String getPrevImageId() {
		return prevImageId;
	}

	public void setPrevImageId(String prevImageId) {
		this.prevImageId = prevImageId;
	}

	public String getCurImageId() {
		return curImageId;
	}

	public void setCurImageId(String curImageId) {
		this.curImageId = curImageId;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Car(int id) {
		this.id = id;
		this.prevImageId = "";
		this.curImageId = "";
		this.groupId = -1;
	}

	@Override
	public String toString() {
		return String.format(
				"Car[id=%s, prevImageId=%s, curImageId=%s, groupId=%s]",
				id, prevImageId, curImageId, groupId);
	}

}
