package com.ota.update.dal;

import java.util.List;

import com.ota.update.entities.Car;

/**
 * Interface defining the offered functionality of the car repository.
 */
public interface CarRepoDAL {
	Car saveCar(Car car);

	List<Car> getAllCars();

	Car findById(int id);

	Car updateCar(Car car);

	void deleteCar(Car car);

	void deleteAll();
}