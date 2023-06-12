package com.ota.update.repos;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import com.ota.update.dal.CarRepoDAL;
import com.ota.update.entities.Car;

/**
 * Repository that uses the MongoTemplate to communicate with the database.
 */
@Repository
public class CarRepo implements CarRepoDAL {

	private final MongoTemplate mongoTemplate;

	@Autowired
	public CarRepo(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public Car saveCar(Car car) {
		mongoTemplate.save(car);
		return car;
	}

	@Override
	public List<Car> getAllCars() {
		return mongoTemplate.findAll(Car.class);
	}

	@Override
	public Car findById(int id) {
		return mongoTemplate.findById(id, Car.class);
	}

	@Override
	public Car updateCar(Car car) {
		return mongoTemplate.save(car);
	}

	@Override
	public void deleteCar(Car car) {
		mongoTemplate.remove(car);

	}

	@Override
	public void deleteAll() {
		mongoTemplate.dropCollection(Car.class);
	}

}
