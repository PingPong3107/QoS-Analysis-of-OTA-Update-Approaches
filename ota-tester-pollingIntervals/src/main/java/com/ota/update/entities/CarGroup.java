package com.ota.update.entities;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A car group is defined by an ID, and ID list of the cars that are part of that group, the previously installed image ID of that group as well as the current one.
 */
@Document("carGroups")
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
	 * @param cars Cars to be added.
	 */
	public void addCars(List<Integer> carIds) {
		this.carIds.addAll(carIds);
	}

}
