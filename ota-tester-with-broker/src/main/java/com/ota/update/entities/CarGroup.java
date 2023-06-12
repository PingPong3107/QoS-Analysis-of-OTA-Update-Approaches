package com.ota.update.entities;

import java.util.List;

import org.springframework.data.annotation.Id;

/**
 * This class models a group of cars with a specific groupID and the
 * corresponding carIds and the previous and current imageID that should be
 * installed.
 */
public class CarGroup {

	@Id
	private int id;
	private List<Integer> carIds;
	private String prevImageId;
	private String curImageId;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<Integer> getCars() {
		return carIds;
	}

	public void setCarIds(List<Integer> carIds) {
		this.carIds = carIds;
	}

	public String getPrevImageId() {
		return prevImageId;
	}

	public void setPrevImageId(String prevImageName) {
		this.prevImageId = prevImageName;
	}

	public String getCurImageId() {
		return curImageId;
	}

	public void setCurImageId(String curImageName) {
		this.curImageId = curImageName;
	}

	/**
	 * Creates a car group with the specified unique ID and the corresponding car
	 * IDs.
	 * 
	 * It is assumed for simplicity reasons that a car can only belong to one group.
	 * 
	 * @param id     An ID that has not been used before. This is not catched since
	 *               for performance tests IDs will be chosen by a script anyway.
	 * @param carIds List of carIDs in that group.
	 */
	public CarGroup(int id, List<Integer> carIds) {
		this.id = id;
		this.carIds = carIds;
		this.curImageId = "";
		this.prevImageId = "";
	}

	@Override
	public String toString() {
		StringBuffer idBuffer = new StringBuffer();
		idBuffer.append("{");
		carIds.stream().forEach(s -> idBuffer.append(s + ";"));
		idBuffer.append("}");
		String listOfIds = idBuffer.toString();
		return String.format(
				"CarGroup[groupId=%s, carIds=%s, prevImage=%s, curImage=%s]",
				id, listOfIds, prevImageId, curImageId);
	}

	/**
	 * Adds cars to the existing group.
	 * 
	 * @param cars IDs of the cars to be added.
	 */
	public void addCars(List<Integer> carIds) {
		this.carIds.addAll(carIds);
	}

}
