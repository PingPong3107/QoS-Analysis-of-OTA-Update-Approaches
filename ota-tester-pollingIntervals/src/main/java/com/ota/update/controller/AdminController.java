package com.ota.update.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URLConnection;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.client.util.IOUtils;
import com.jlefebure.spring.boot.minio.MinioException;
import com.jlefebure.spring.boot.minio.MinioService;
import com.ota.update.entities.DeltaFlag;
import com.ota.update.entities.Car;
import com.ota.update.entities.CarGroup;
import com.ota.update.repos.DeltaFlagRepo;
import com.ota.update.repos.CarRepo;
import com.ota.update.repos.GroupRepo;

/**
 * RestController which offers endpoints that a fleet administrator can use to
 * start rollouts and manage his fleet(s) in general.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

	@Autowired
	private GroupRepo groupRepo;

	@Autowired
	private CarRepo carRepo;

	@Autowired
	private DeltaFlagRepo deltaRepo;

	@Autowired
	private MinioService minioService;

	private double deltaSimilarity;

	private long deltaCreationTime;

	@Autowired
	private Environment env;

	/**
	 * Initializes the databases at startup. All entries get deleted, the delta
	 * similarity as well as the mocked delta creation time is set from the
	 * environment variables and the service starts in
	 * non-delta mode
	 */
	@PostConstruct
	public void init() {
		groupRepo.deleteAll();
		carRepo.deleteAll();
		deltaRepo.deleteAll();
		deltaCreationTime = Long.parseLong(env.getProperty("delta.creationTime"));
		deltaSimilarity = Double.parseDouble(env.getProperty("delta.similarity"));
		deltaRepo.save(new DeltaFlag(0, false));
	}

	/**
	 * Endpoint which deletes all entries in the repos.
	 */
	@PostMapping("/deleteRepos")
	public void deleteRepos() {
		groupRepo.deleteAll();
		carRepo.deleteAll();
	}

	/**
	 * Returns all registered cars as a list.
	 */
	@GetMapping("/cars")
	public List<Car> getCars() {
		return carRepo.getAllCars();
	}

	/**
	 * Creates a car group with the specified ID and the given car IDs.
	 * 
	 * @param groupId unique Integer ID
	 * @param carIds  encoded in the form: id1,id2,id3,...
	 */
	@PostMapping("/createGroup/{groupId}/{carIds}")
	public void createGroup(@PathVariable("groupId") int groupId,
			@PathVariable("carIds") String carIds) {
		List<Integer> cars = convertStringToList(carIds);
		groupRepo.saveCarGroup(new CarGroup(groupId, cars));
		cars.stream().forEach(c -> {
			Car car = carRepo.findById((int) c);
			if (car.getGroupId() != -1) {
				throw new IllegalArgumentException("Car " + c + " belongs already to another group!");
			}
			car.setGroupId(groupId);
			carRepo.updateCar(car);

		});
	}

	/**
	 * Returns a specific group. Mainly for debugging.
	 */
	@GetMapping("/getGroup")
	public CarGroup getGroup(@RequestParam(value = "groupId") int groupId) {
		return groupRepo.findById(groupId);
	}

	/**
	 * Returns the cars that are part of the given group.
	 * 
	 * @param groupId
	 * @return Cars as ID list.
	 */
	@GetMapping("/carsInGroup")
	public List<Integer> carsInGroup(@RequestParam(value = "groupId") int groupId) {
		CarGroup cargroup = groupRepo.findById(groupId);
		return cargroup.getCars();
	}

	/**
	 * Adds the cars with the specified IDs to the given group.
	 * 
	 * @param groupId
	 * @param carIds  encoded in the form: id1,id2,id3,...
	 */
	@PostMapping("/addCarsToGroup")
	public void addCarsToGroup(@RequestParam(value = "groupId") int groupId,
			@RequestParam(value = "carIds") String carIds) {
		CarGroup cargroup = groupRepo.findById(groupId);
		List<Integer> carIdList = convertStringToList(carIds);
		cargroup.addCars(carIdList);
		groupRepo.updateCarGroup(cargroup);
		carIdList.stream().forEach(c -> {
			Car car = carRepo.findById((int) c);
			if (car.getGroupId() != -1) {
				throw new IllegalArgumentException("Car " + c + " belongs already to another group!");
			}
			car.setGroupId(groupId);
			carRepo.updateCar(car);
		});
	}

	/**
	 * Returns the update image with the specified ID.
	 * 
	 * @param imageId
	 * @param response
	 * @throws MinioException
	 * @throws IOException
	 */
	@GetMapping("/getImage/{imageId}")
	public void getObject(@PathVariable("imagId") String imageId, HttpServletResponse response)
			throws MinioException, IOException {
		InputStream inputStream = minioService.get(Path.of(imageId));
		// Set the content type and attachment header.
		response.addHeader("Content-disposition", "attachment;filename=" + imageId);
		response.setContentType(URLConnection.guessContentTypeFromName(imageId));

		// Copy the stream to the response's output stream.
		IOUtils.copy(inputStream, response.getOutputStream());
		response.flushBuffer();
	}

	/**
	 * Uploads an update image file and stores it in the database.
	 * 
	 * @param file
	 * @throws IOException
	 * @throws MinioException
	 */
	@PostMapping("/uploadImage")
	public void addAttachement(@RequestParam(value = "file") MultipartFile file) throws MinioException, IOException {
		Path path = Path.of(file.getOriginalFilename());
		Map<String, String> header = new HashMap<>();
		header.put("X-Incident-Id", "C918371984");

		minioService.upload(path, file.getInputStream(), file.getContentType(), header);

	}

	/**
	 * Starts a rollout with the specified image, for the given group and with the
	 * given strategy. (Here only polling intervals)
	 * 
	 * @param imageId
	 * @param groupId
	 * @param strategy Strategy string construction given in the thesis
	 * @throws IOException
	 * @throws MinioException
	 * @throws UnsupportedCharsetException
	 * @throws ParseException
	 * @throws InterruptedException
	 */
	@PostMapping("/rollout/{imageId}/{groupId}/{strategy}")
	public void rollout(@PathVariable("imageId") String imageId,
			@PathVariable("groupId") int groupId,
			@PathVariable("strategy") String strategy)
			throws IOException, MinioException, UnsupportedCharsetException, ParseException,
			InterruptedException {
		if (groupRepo.findById(groupId) == null) {
			throw new IllegalArgumentException("Group does not exist!");
		}
		System.out.println(strategy);
		String[] strategyTokens = strategy.split(",");
		switch (strategyTokens[0]) {
			case "optional":
				if (strategyTokens[1].equals("continuous")) {
					if (strategyTokens[2].equals("delta")) {
						// optional;continuous;delta
					} else {
						// optional;continuous;non-delta
					}
				} else {
					if (strategyTokens[2].equals("delta")) {
						// optional;snapshot;delta
					} else {
						// optional;snapshot;non-delta
					}
				}
				break;
			case "push":
				if (strategyTokens[1].equals("continuous")) {
					if (strategyTokens[2].equals("delta")) {
						// push;continuous;delta
					} else {
						// push;continuous;non-delta
					}
				} else {
					if (strategyTokens[2].equals("delta")) {
						// push;snapshot;delta
					} else {
						// push;snapshot;non-delta
					}
				}
				break;
			case "polling_intervals":
				if (strategyTokens[2].equals("delta")) {
					// polling_intervals;-;delta
					CarGroup carGroup = moveImageIdsAndReturnGroup(groupId, imageId);
					String prevImageId = carGroup.getPrevImageId();
					File file = createDeltaFile(prevImageId, imageId);
					sendDeltaFileToDB(prevImageId, imageId, file);
					file.delete();
					groupRepo.updateCarGroup(carGroup);
					deltaRepo.updateDeltaFlag(new DeltaFlag(0, true));
				} else {
					// polling_intervals;-;non-delta
					CarGroup carGroup = groupRepo.findById(groupId);
					carGroup.setPrevImageId(carGroup.getCurImageId());
					carGroup.setCurImageId(imageId);
					groupRepo.updateCarGroup(carGroup);
					deltaRepo.updateDeltaFlag(new DeltaFlag(0, false));
				}
				break;
			default:
				throw new IllegalArgumentException("Incorrect strategy string!");
		}

	}

	/**
	 * Sends a delta file to the database with the correct naming convention.
	 * 
	 * @param prevImageId image ID of the previously installed image
	 * @param imageId     image ID of the image that is to be installed
	 * @param deltaFile   delta file
	 * @throws FileNotFoundException
	 * @throws UnsupportedCharsetException
	 * @throws ParseException
	 * @throws MinioException
	 */
	private void sendDeltaFileToDB(String prevImageId, String imageId, File deltaFile)
			throws FileNotFoundException, UnsupportedCharsetException, ParseException, MinioException {
		InputStream inputStream = new FileInputStream(deltaFile);
		Map<String, String> header = new HashMap<>();
		header.put("X-Incident-Id", "C918371984");
		minioService.upload(
				Path.of(prevImageId + "_" + imageId + ".delta"),
				inputStream,
				ContentType.parse("application/octet-stream"),
				header);
	}

	/**
	 * Mocks the creation of a delta file by creating a random image with a size
	 * that is determined by the delta similarity that is given by the environment
	 * variables. Also the time for that needed is emulated by a predefined waiting
	 * time
	 * 
	 * @param prevImageId image ID of the previously installed image
	 * @param imageId     image ID of the image that is to be installed
	 * @return mocked delta file of the both images
	 * @throws IOException
	 * @throws MinioException
	 * @throws InterruptedException
	 */
	private File createDeltaFile(String prevImageId, String imageId)
			throws IOException, MinioException, InterruptedException {
		File file = new File(prevImageId + "_" + imageId + ".delta");
		if (prevImageId.equals("")) {
			throw new IllegalArgumentException(
					"Delta not possible if prevImage is empty");
		}
		byte[] image = minioService.get(Path.of(imageId))
				.readAllBytes();
		byte[] prevImage = minioService.get(Path.of(prevImageId))
				.readAllBytes();
		file.createNewFile();
		Thread.sleep(deltaCreationTime);
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		raf.setLength((int) Math.ceil(image.length * deltaSimilarity));
		raf.close();
		return file;
	}

	/**
	 * Sets the previous and the current image of a car group when a rollout is
	 * started.
	 * 
	 * @param groupId effected group by ID
	 * @param imageId ID of the new image
	 * @return the modified car group
	 */
	private CarGroup moveImageIdsAndReturnGroup(int groupId, String imageId) {
		CarGroup carGroup = groupRepo.findById(groupId);
		carGroup.setPrevImageId(carGroup.getCurImageId());
		carGroup.setCurImageId(imageId);
		return carGroup;
	}

	/**
	 * Converts a car ID string input into a car ID list.
	 * 
	 * @param groupString of the form id1,id2,id3
	 * @return car IDs as Integer list
	 */
	private List<Integer> convertStringToList(String groupString) {
		List<Integer> carIdList = new ArrayList<>();
		String[] cars = groupString.split(",");
		for (String string : cars) {
			carIdList.add(Integer.parseInt(string));
		}
		return carIdList;
	}

}
