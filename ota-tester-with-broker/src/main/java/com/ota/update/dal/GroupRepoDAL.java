package com.ota.update.dal;

import java.util.List;

import com.ota.update.entities.CarGroup;

/**
 * Interface defining the offered functionality of the car group repository.
 */
public interface GroupRepoDAL {
	CarGroup saveCarGroup(CarGroup group);

	List<CarGroup> getAllGroups();

	CarGroup findById(int id);

	CarGroup updateCarGroup(CarGroup group);

	void deleteCarGroup(CarGroup group);

	void deleteAll();

}
