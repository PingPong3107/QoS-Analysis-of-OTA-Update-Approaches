package com.ota.update.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.client.util.IOUtils;
import com.jlefebure.spring.boot.minio.MinioException;
import com.jlefebure.spring.boot.minio.MinioService;
import com.ota.update.entities.Car;
import com.ota.update.entities.CarGroup;
import com.ota.update.repos.DeltaFlagRepo;
import com.ota.update.repos.CarRepo;
import com.ota.update.entities.PollResponse;
import com.ota.update.repos.GroupRepo;


/**
 * REST-API provided for the cars to communicate with. Fulfills functionality like registration, polling and image provision.
 */
@RestController
@RequestMapping("/car")
public class CarController {

	@Autowired
	private GroupRepo groupRepo;

	@Autowired
	private CarRepo carRepo;

	@Autowired
	private DeltaFlagRepo deltaRepo;

	@Autowired
	private MinioService minioService;

	/**
	 * Registers a car with the specified ID.
	 * @param carId unique ID (assumed but not enforced, since cars in tests are created in a loop anyway)
	 */
	@PostMapping("/register/{carId}")
	public void registerCar(@PathVariable("carId") int carId) {
		carRepo.saveCar(new Car(carId));
	}

	/**
	 * Checks if a new image is available for the specified car.
	 * 
	 * For that it is checked if the corersponding car fleet's image does not match the car's.
	 * @param carId
	 * @return true if new image is available, false if car is not in a group or no new image is available
	 */
	@GetMapping("/newImageAvailable/{carId}")
	public PollResponse newImageAvailable(@PathVariable("carId") int carId) {
		Car car = carRepo.findById(carId);
		if (car.getGroupId() == -1) {
			return new PollResponse(false);
		}
		CarGroup carGroup = groupRepo.findById(car.getGroupId());
		String curCarImageId = car.getCurImageId();
		String curGroupImageId = carGroup.getCurImageId();
		if (curCarImageId.equals(curGroupImageId)) {
			return new PollResponse(false);
		} else {
			return new PollResponse(true);
		}
	}

	/**
	 * Returns the newest imagefile that should be installed on the car. In delta
	 * mode it returns a (mocked) delta file between new and old image.
	 * 
	 * @param carId    ID of the car that is to be updated
	 * @param response The response template used to return the image file.
	 * @throws MinioException
	 * @throws IOException
	 */
	@GetMapping("/getNewImage/{carId}")
	public void newImage(@PathVariable("carId") int carId, HttpServletResponse response)
			throws MinioException, IOException {
		Car car = carRepo.findById(carId);

		if (car.getGroupId() == -1) {
			throw new IllegalArgumentException("Car is not in a group!");
		}

		CarGroup carGroup = groupRepo.findById(car.getGroupId());
		String curImageId = carGroup.getCurImageId();
		car.setPrevImageId(car.getCurImageId());
		car.setCurImageId(curImageId);
		String prevImageId = carGroup.getPrevImageId();

		InputStream inputStream;
		if (deltaRepo.findById(0).isDelta()) {
			inputStream = minioService.get(Path.of(prevImageId + "_" + curImageId + ".delta"));
		} else {
			inputStream = minioService.get(Path.of(curImageId));
		}

		response.addHeader("Content-disposition", "attachment;filename=" + curImageId);
		response.setContentType("application/octet-stream");
		IOUtils.copy(inputStream, response.getOutputStream());
		response.flushBuffer();

		carRepo.updateCar(car);

	}


}
