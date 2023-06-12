package com.ota.update.entities;

import org.springframework.data.annotation.Id;
/**
 * Class that models a car with a unique ID, IDs for it current and its previous installed image and the corresponding group ID.
 */
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
	/**
	 * Creates a car with given ID. Current and previous imageIDs are empty strings initially.
	 */
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
