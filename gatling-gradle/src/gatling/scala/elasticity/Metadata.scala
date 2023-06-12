package elasticity

import io.gatling.core.Predef._
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Refactorable attributes and operations for elasticity tests.
 */
object Metadata {

  //Environment variables and initializations
  val host: String = scala.util.Properties.envOrElse("pollingIntervalsHost", "ota-tester-pi-service.ota-tester.svc.cluster.local")
  val numberOfCars: Int = scala.util.Properties.envOrElse("numberOfCars", "1").toInt
  val carProtocol: String = "http://" + host + "/car/"
  val adminProtocol: String = "http://" + host + "/admin/"
  val ids: List[Int] = List.range(0, numberOfCars)
  val imageId1: String = scala.util.Properties.envOrElse("imageId1", "scndSmallestImage.bin")
  val imageId2: String = scala.util.Properties.envOrElse("imageId2", "scndSmallestImage2.bin")
  val waitTimeForFirstRollout: Int = scala.util.Properties.envOrElse("waitTimeForFirstRollout", "0").toInt
  val waitTimeForScndRollout: Int = scala.util.Properties.envOrElse("waitTimeForScndRollout", "30").toInt
  val simulationTime: Int = scala.util.Properties.envOrElse("simulationTime", "90").toInt
  private val pollingInterval: Int = scala.util.Properties.envOrElse("pollingInterval", "5").toInt
  val basePollLoad: Int = scala.util.Properties.envOrElse("basePollLoad", "20").toInt
  val heavyLoad: Int = scala.util.Properties.envOrElse("heavyLoad", "50").toInt


  /**
   * Creates a list of all ids needed for the simulation.
   * @return
   */
  def createStringIdList(): String = {
    var idString: String = ""
    val stringBuilder: StringBuilder = new StringBuilder();
    ids.foreach(id => {
      stringBuilder.append(id + ",")
    })
    idString = stringBuilder.toString()
    idString

  }

  /**
   * Deletes all repos of the service.
   */
  val deleteRepos: ScenarioBuilder = scenario("delete repos")
    .exec(
      http("delete repos")
        .post(Metadata.adminProtocol + "deleteRepos")
        .check(status.is(200))
    )

  /**
   * Registers the car with id 0
   */
  val register: ScenarioBuilder = scenario("register car")
    .exec(
      http("register device")
        .post(carProtocol + "/register/0")
        .check(status.is(200))
    )

  /**
   * Pulls the image for car 0.
   */
  val pullImage: ScenarioBuilder = scenario("pull Image")
    .exec(
      http("get New Image")
        .get(carProtocol + "getNewImage/" + "0")
        .check(status.is(200))
        .requestTimeout(3.minutes)
    )

  /**
   * Polls if a new image is available for car 0.
   */
  val simplePoll: ScenarioBuilder = scenario("simple poll to create base load")
    .exec(
      http("new Image available")
        .get(carProtocol + "newImageAvailable/" + "0")
        .check(status.is(200))
    )

  /**
   * Creates a car group with only one car and starts a rollout for that car in polling intervals mode.
   */
  val cGroupAndRollout: ScenarioBuilder = scenario("create group and rollout")
    .exec(
      http("createGroup")
        .post(adminProtocol + "createGroup/0/" + "0")
        .check(status.is(200))
    ).pause(Metadata.waitTimeForFirstRollout).exec(
    http("createRollout")
      .post(
        adminProtocol + "rollout/" + imageId1 + "/0/polling_intervals,-,non-delta"
      )
      .check(status.is(200))
  )



}
