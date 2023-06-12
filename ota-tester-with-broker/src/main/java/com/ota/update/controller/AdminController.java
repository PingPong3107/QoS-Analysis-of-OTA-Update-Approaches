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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.ParseException;
import org.apache.http.entity.ContentType;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
import com.ota.update.entities.Rollout;
import com.ota.update.entities.RolloutType;
import com.ota.update.messageTypes.UpdateNotification;
import com.ota.update.messageTypes.UpdatePayload;
import com.ota.update.repos.DeltaFlagRepo;
import com.ota.update.repos.CarRepo;
import com.ota.update.repos.GroupRepo;
import com.ota.update.repos.RolloutRepo;

/**
 * The REST conroller an admin can connect to to create groups, start rollouts
 * and
 * do other administrational tasks.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

	@Autowired
	private GroupRepo groupRepo;

	@Autowired
	private CarRepo carRepo;

	@Autowired
	private RolloutRepo rolloutRepo;

	@Autowired
	private DeltaFlagRepo deltaRepo;

	@Autowired
	private MinioService minioService;

	@Autowired
	private AmqpAdmin admin;

	@Autowired
	private RabbitTemplate template;

	@Autowired
	private Environment env;

	private double deltaSimilarity;

	private long deltaCreationTime;

	/**
	 * Endpoint that allows the deletion of all repos and that resets the mode to
	 * non-delta.
	 */
	@PostMapping("/deleteRepos")
	public void deleteRepos() {
		groupRepo.deleteAll();
		carRepo.deleteAll();
		rolloutRepo.deleteAll();
		deltaRepo.deleteAll();
		deltaRepo.save(new DeltaFlag(0, false));
	}

	/**
	 * Initializes the databases at startup by deleting all entries. This is useful
	 * if only the server is newly deployed after a test. Also delta is initially
	 * set to false and the mocked delta similarity is extracted from the
	 * environment
	 * variables.
	 */
	@PostConstruct
	public void init() {
		groupRepo.deleteAll();
		carRepo.deleteAll();
		rolloutRepo.deleteAll();
		deltaSimilarity = Double.parseDouble(env.getProperty("delta.similarity"));
		deltaRepo.deleteAll();
		deltaCreationTime=Long.parseLong(env.getProperty("delta.creationTime"));
		deltaRepo.save(new DeltaFlag(0, false));
	}

	/**
	 * Lists all cars in the database.
	 * 
	 * @return List of all registered cars
	 */
	@GetMapping("/cars")
	public List<Car> getCars() {
		return carRepo.getAllCars();
	}

	/**
	 * Creates a car group by saving the corresponding carIDs in a CarGroup object
	 * and storing it in the database.
	 * It also declares an exchange in the broker and binds it to the queues of the
	 * effected cars.
	 * 
	 * @param groupId
	 * @param carIds
	 */
	@PostMapping("/createGroup/{groupId}/{carIds}")
	public void createGroup(@PathVariable("groupId") int groupId,
			@PathVariable("carIds") String carIds) {
		List<Integer> cars = convertStringToList(carIds);
		groupRepo.saveCarGroup(new CarGroup(groupId, cars));
		ExchangeBuilder builder = new ExchangeBuilder(String.valueOf(groupId), ExchangeTypes.FANOUT);
		builder.durable(false);
		admin.declareExchange(builder.build());
		cars.stream().forEach(c -> {
			Car car = carRepo.findById((int) c);
			if (car.getGroupId() != -1) {
				throw new IllegalArgumentException("Car " + c + " belongs already to another group!");
			}
			car.setGroupId(groupId);
			carRepo.updateCar(car);
			admin.declareBinding(new Binding(String.valueOf(c), DestinationType.QUEUE,
					String.valueOf(groupId), String.valueOf(c), null));
		});
	}

	/**
	 * Returns the group with the specified groupId. Mainly for debugging reasons...
	 * 
	 * @param groupId
	 * @return Car group with groupId
	 */
	@GetMapping("/getGroup")
	public CarGroup getGroup(@RequestParam(value = "groupId") int groupId) {
		return groupRepo.findById(groupId);
	}

	/**
	 * Returns the cars of the specified group. Mainly for debugging reasons...
	 * 
	 * @param groupId
	 * @return carsthat are in the group as ID list
	 */
	@GetMapping("/carsInGroup")
	public List<Integer> carsInGroup(@RequestParam(value = "groupId") int groupId) {
		CarGroup cargroup = groupRepo.findById(groupId);
		return cargroup.getCars();
	}

	/**
	 * Adds the cars with the specified IDs to the group.
	 * 
	 * Also handles that later added cars are supplied with the dedicated group
	 * image.
	 * 
	 * @param groupId
	 * @param carIds  CarIDs are provided as a string formated like that:
	 *                "carId0;carId1;carId2;..."
	 * @throws MinioException
	 * @throws IOException
	 */
	@PostMapping("/addCarsToGroup/{groupId}/{carIds}")
	public void addCarsToGroup(@PathVariable("groupId") int groupId,
			@PathVariable("carIds") String carIds) throws IOException, MinioException {
		CarGroup cargroup = groupRepo.findById(groupId);
		List<Integer> carIdList = convertStringToList(carIds);
		// carIdList.stream().forEach(c->System.out.println(c));
		cargroup.addCars(carIdList);
		groupRepo.updateCarGroup(cargroup);
		carIdList.stream().forEach(c -> {
			Car car = carRepo.findById((int) c);
			if (car.getGroupId() != -1) {
				throw new IllegalArgumentException("Car " + c + " belongs already to another group!");
			}
			car.setGroupId(groupId);
			carRepo.updateCar(car);
			// bind new cars to group exchange
			admin.declareBinding(new Binding(String.valueOf(c), DestinationType.QUEUE,
					String.valueOf(groupId), String.valueOf(c), null));
		});
		Rollout rollout = rolloutRepo.findById(groupId);
		// rolloutRepo.getAllRollouts().stream().forEach(r ->
		// System.out.println(r.getGroupId()));
		if (rollout != null) {
			ExchangeBuilder builder = new ExchangeBuilder(groupId + "alternative", ExchangeTypes.FANOUT);
			admin.declareExchange(builder.build());
			carIdList.stream().forEach(c -> {
				// bind new cars also to an alternative exchange to supply them with the new
				// image
				admin.declareBinding(new Binding(String.valueOf(c), DestinationType.QUEUE,
						groupId + "alternative", String.valueOf(c), null));

			});
			if (rollout.getRolloutType() == RolloutType.OPTIONAL) {
				if (deltaRepo.findById(0).isDelta()) {
					UpdateNotification message = new UpdateNotification(
							"New delta update available for car Group " + groupId);
					template.convertAndSend(groupId + "alternative", ExchangeTypes.FANOUT, message);
				} else {
					UpdateNotification message = new UpdateNotification(
							"New update available for car Group " + groupId);
					template.convertAndSend(groupId + "alternative", ExchangeTypes.FANOUT, message);
				}
			} else {
				if (deltaRepo.findById(0).isDelta()) {
					byte[] message = minioService.get(
							Path.of(cargroup.getPrevImageId() + "_" + rollout.getImageId()
									+ ".delta"))
							.readAllBytes();
					int fileSize = message.length;
					UpdatePayload payload = new UpdatePayload(fileSize, message);
					updateGroupSubset(carIdList, cargroup);
					template.convertAndSend(groupId + "alternative", null, payload);
				} else {
					byte[] message = minioService.get(Path.of(rollout.getImageId()))
							.readAllBytes();
					int fileSize = message.length;
					UpdatePayload payload = new UpdatePayload(fileSize, message);
					updateGroupSubset(carIdList, cargroup);
					template.convertAndSend(groupId + "alternative", null, payload);
				}

			}
			admin.deleteExchange(groupId + "alternative");
		}
	}

	/**
	 * Updates a subset of car entries in the database with the image specified in
	 * the car group.
	 * 
	 * @param carIdList
	 * @param cargroup
	 */
	private void updateGroupSubset(List<Integer> carIdList, CarGroup cargroup) {
		carIdList.stream().forEach(c -> {
			Car car = carRepo.findById((int) c);
			car.setPrevImageId(car.getCurImageId());
			car.setCurImageId(cargroup.getCurImageId());
			carRepo.updateCar(car);
		});
	}

	/**
	 * Returns the image with the specified name. (for debugging purposes)
	 * 
	 * @param object
	 * @param response
	 * @throws MinioException
	 * @throws IOException
	 */
	@GetMapping("/getImage/{object}")
	public void getObject(@PathVariable("object") String object, HttpServletResponse response)
			throws MinioException, IOException {
		InputStream inputStream = minioService.get(Path.of(object));
		// Set the content type and attachment header.
		response.addHeader("Content-disposition", "attachment;filename=" + object);
		response.setContentType(URLConnection.guessContentTypeFromName(object));

		// Copy the stream to the response's output stream.
		IOUtils.copy(inputStream, response.getOutputStream());
		response.flushBuffer();
	}

	/**
	 * Uploads an image to the Minio object storage.
	 * 
	 * @param file
	 * @throws IOException
	 * @throws MinioException
	 */
	@PostMapping("/uploadImage")
	public void addAttachement(@RequestParam(value = "file") MultipartFile file)
			throws MinioException, IOException {
		Path path = Path.of(file.getOriginalFilename());
		Map<String, String> header = new HashMap<>();
		header.put("X-Incident-Id", "C918371984");
		minioService.upload(path, file.getInputStream(), file.getContentType(), header);

	}

	/**
	 * Starts an update rollout with the defined strategy.
	 * 
	 * @param imageId
	 * @param groupId
	 * @param strategy Strategy strings are defined in the thesis document.
	 * @throws MinioException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@PostMapping("/rollout/{imageId}/{groupId}/{strategy}")
	public void rollout(@PathVariable("imageId") String imageId,
			@PathVariable(value = "groupId") int groupId,
			@PathVariable(value = "strategy") String strategy) throws IOException, MinioException, InterruptedException {
		if (groupRepo.findById(groupId) == null) {
			throw new IllegalArgumentException("Group does not exist!");
		}
		String[] strategyTokens = strategy.split(",");
		switch (strategyTokens[0]) {
			case "optional":
				if (strategyTokens[1].equals("continuous")) {
					if (strategyTokens[2].equals("delta")) {
						// optional;continuous;delta
						createContinuousRollout(groupId, imageId, RolloutType.OPTIONAL);
						CarGroup carGroup = moveImageIdsAndReturnGroup(groupId, imageId);
						String prevImageId = carGroup.getPrevImageId();
						File file = createDeltaFile(prevImageId, imageId);
						sendDeltaFileToDB(prevImageId, imageId, file);
						file.delete();
						groupRepo.updateCarGroup(carGroup);
						sendNotificationDelta(groupId);
						deltaRepo.updateDeltaFlag(new DeltaFlag(0, true));
					} else {
						// optional;continuous;non-delta
						createContinuousRollout(groupId, imageId, RolloutType.OPTIONAL);
						CarGroup carGroup = moveImageIdsAndReturnGroup(groupId, imageId);
						groupRepo.updateCarGroup(carGroup);
						deltaRepo.updateDeltaFlag(new DeltaFlag(0, false));
						sendNotificationNonDelta(groupId);
					}
				} else {
					if (strategyTokens[2].equals("delta")) {
						// optional;snapshot;delta
						CarGroup carGroup = moveImageIdsAndReturnGroup(groupId, imageId);
						String prevImageId = carGroup.getPrevImageId();
						File file = createDeltaFile(prevImageId, imageId);
						sendDeltaFileToDB(prevImageId, imageId, file);
						file.delete();
						groupRepo.updateCarGroup(carGroup);
						sendNotificationDelta(groupId);
						deltaRepo.updateDeltaFlag(new DeltaFlag(0, true));
					} else {
						// optional;snapshot;non-delta
						CarGroup carGroup = moveImageIdsAndReturnGroup(groupId, imageId);
						groupRepo.updateCarGroup(carGroup);
						sendNotificationNonDelta(groupId);
						deltaRepo.updateDeltaFlag(new DeltaFlag(0, false));
					}
				}
				break;
			case "push":
				if (strategyTokens[1].equals("continuous")) {
					if (strategyTokens[2].equals("delta")) {
						// push;continuous;delta
						createContinuousRollout(groupId, imageId, RolloutType.PUSH);
						CarGroup carGroup = moveImageIdsAndReturnGroup(groupId, imageId);
						String prevImageId = carGroup.getPrevImageId();
						File file = createDeltaFile(prevImageId, imageId);
						sendDeltaFileToDB(prevImageId, imageId, file);
						file.delete();
						groupRepo.updateCarGroup(carGroup);
						sendPayloadToCarsDelta(prevImageId, imageId, groupId);
						updateCarStatus(carGroup, imageId);
						deltaRepo.updateDeltaFlag(new DeltaFlag(0, true));
					} else {
						// push;continuous;non-delta
						createContinuousRollout(groupId, imageId, RolloutType.PUSH);
						CarGroup carGroup = moveImageIdsAndReturnGroup(groupId, imageId);
						groupRepo.updateCarGroup(carGroup);
						sendPayloadToCarsNonDelta(imageId, groupId);
						updateCarStatus(carGroup, imageId);
						deltaRepo.updateDeltaFlag(new DeltaFlag(0, false));
					}
				} else {
					if (strategyTokens[2].equals("delta")) {
						// push;snapshot;delta
						CarGroup carGroup = moveImageIdsAndReturnGroup(groupId, imageId);
						String prevImageId = carGroup.getPrevImageId();
						File file = createDeltaFile(prevImageId, imageId);
						sendDeltaFileToDB(prevImageId, imageId, file);
						file.delete();
						groupRepo.updateCarGroup(carGroup);
						sendPayloadToCarsDelta(prevImageId, imageId, groupId);
						updateCarStatus(carGroup, imageId);
						deltaRepo.updateDeltaFlag(new DeltaFlag(0, true));
					} else {
						// push;snapshot;non-delta
						CarGroup carGroup = moveImageIdsAndReturnGroup(groupId, imageId);
						groupRepo.updateCarGroup(carGroup);
						sendPayloadToCarsNonDelta(imageId, groupId);
						updateCarStatus(carGroup, imageId);
						deltaRepo.updateDeltaFlag(new DeltaFlag(0, false));
					}
				}
				break;
			// all down here is not necessary in this version of the service
			case "polling_intervals":
				if (strategyTokens[2].equals("delta")) {
					// polling_intervals;-;delta

				} else {
					// polling_intervals;-;non-delta

				}
				break;
			default:
				throw new IllegalArgumentException("Incorrect strategy string!");
		}

	}

	/**
	 * Stops the continuous rollout that is associated with the specified group by
	 * deleting the respective entry in the database.
	 * 
	 * @param groupId
	 */
	@PostMapping("/stopContinuousRollout")
	public void stopContinuousRollout(@RequestParam(value = "groupId") int groupId) {
		rolloutRepo.deleteById(groupId);
	}

	/**
	 * Returns all currently running continuous rollouts.
	 */
	@GetMapping("/getAllContinuousRollouts")
	public List<Rollout> getAllContinuousRollouts() {
		return rolloutRepo.getAllRollouts();
	}

	/**
	 * Creates an continuous rollout and saves it in the database.
	 * 
	 * @param groupId     group that gets the rollout
	 * @param imageId     ID of the image that is supplied
	 * @param rolloutType Typ of the rollout (PUSH or OPTIONAL)
	 */
	private void createContinuousRollout(int groupId, String imageId, RolloutType rolloutType) {
		Rollout rollout = new Rollout(groupId, imageId,
				/* new Timestamp(System.currentTimeMillis()), */ rolloutType);
		rolloutRepo.saveRollout(rollout);
	}

	/**
	 * Updates the image ID of every car in the car group.
	 * 
	 * @param carGroup
	 * @param imageId
	 */
	private void updateCarStatus(CarGroup carGroup, String imageId) {
		carGroup.getCars().stream().forEach(c -> {
			Car car = carRepo.findById((int) c);
			car.setPrevImageId(car.getCurImageId());
			car.setCurImageId(imageId);
			carRepo.updateCar(car);
		});
	}

	/**
	 * Sends the specified image to all cars in the group with the specified ID in
	 * non-delta mode.
	 * 
	 * @param imageId
	 * @param groupId
	 * @throws IOException
	 * @throws MinioException
	 */
	private void sendPayloadToCarsNonDelta(String imageId, int groupId) throws IOException, MinioException {
		byte[] message = minioService.get(Path.of(imageId))
				.readAllBytes();
		int fileSize = message.length;
		UpdatePayload payload = new UpdatePayload(fileSize, message);
		template.convertAndSend(String.valueOf(groupId), null, payload);
	}

	/**
	 * Sends the specified delta image to all cars in the group with the specified
	 * ID.
	 * 
	 * @param imageId
	 * @param groupId
	 * @throws IOException
	 * @throws MinioException
	 */
	private void sendPayloadToCarsDelta(String prevImageId, String imageId, int groupId)
			throws IOException, MinioException {
		byte[] message = minioService.get(
				Path.of(prevImageId + "_" + imageId + ".delta"))
				.readAllBytes();
		int fileSize = message.length;
		UpdatePayload payload = new UpdatePayload(fileSize, message);
		template.convertAndSend(String.valueOf(groupId), null, payload);
	}

	/**
	 * Sends out a notification that a new delta update is available to all cars,
	 * that are part of the group.
	 * 
	 * @param groupId
	 */
	private void sendNotificationDelta(int groupId) {
		UpdateNotification notification = new UpdateNotification(
				"New delta update available for car Group " + groupId);
		template.convertAndSend(String.valueOf(groupId), null, notification);
	}

	/**
	 * Sends out a notification that a new update is available to all cars, that are
	 * part of the group.
	 * 
	 * @param groupId
	 */
	private void sendNotificationNonDelta(int groupId) {
		UpdateNotification notification = new UpdateNotification(
				"New update available for car Group " + groupId);
		template.convertAndSend(String.valueOf(groupId), null, notification);
	}

	/**
	 * Sends a delta file to the database with the correct naming.
	 * 
	 * @param prevImageId
	 * @param imageId
	 * @param updateImage
	 * @throws FileNotFoundException
	 * @throws UnsupportedCharsetException
	 * @throws ParseException
	 * @throws MinioException
	 */
	private void sendDeltaFileToDB(String prevImageId, String imageId, File updateImage)
			throws FileNotFoundException, UnsupportedCharsetException, ParseException, MinioException {
		InputStream inputStream = new FileInputStream(updateImage);
		Map<String, String> header = new HashMap<>();
		header.put("X-Incident-Id", "C918371984");
		minioService.upload(
				Path.of(prevImageId + "_" + imageId + ".delta"),
				inputStream,
				ContentType.parse("application/octet-stream"),
				header);
	}

	/**
	 * Mocks the creation of a delta file by creating a random image that only has
	 * 1-Deltasimilarity percent of the original file size. Also the creation time is mocked by a specified waiting time.
	 * 
	 * @param prevImageId
	 * @param imageId
	 * @return
	 * @throws IOException
	 * @throws MinioException
	 * @throws InterruptedException
	 */
	private File createDeltaFile(String prevImageId, String imageId) throws IOException, MinioException, InterruptedException {
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
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		raf.setLength((int) Math.ceil(image.length * (1-deltaSimilarity)));
		raf.close();
		Thread.sleep(deltaCreationTime);
		return file;
	}

	/**
	 * Switches the current image ID to the previous image ID and sets the new one. Then the modified group is returned.
	 */
	private CarGroup moveImageIdsAndReturnGroup(int groupId, String imageId) {
		CarGroup carGroup = groupRepo.findById(groupId);
		carGroup.setPrevImageId(carGroup.getCurImageId());
		carGroup.setCurImageId(imageId);
		return carGroup;
	}

	/**
	 * Converts the carIds string input into a list.
	 * 
	 * @param groupString
	 * @return List of carIds as Integer list.
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
