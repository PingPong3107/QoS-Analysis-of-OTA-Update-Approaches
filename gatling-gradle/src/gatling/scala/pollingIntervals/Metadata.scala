package pollingIntervals


import io.gatling.core.Predef._
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import io.gatling.http.Predef._

import scala.language.postfixOps
import scala.concurrent.duration._


object Metadata {
  //environment variables
  val host: String = scala.util.Properties.envOrElse("pollingIntervalsHost", "ota-tester-pi-service.ota-tester.svc.cluster.local")
  val numberOfCars: Int = scala.util.Properties.envOrElse("numberOfCars", "1").toInt
  val carProtocol: String = "http://" + host + "/car/"
  val adminProtocol: String = "http://" + host + "/admin/"
  val ids: List[Int] = List.range(0, numberOfCars)
  val imageId1: String = scala.util.Properties.envOrElse("imageId1", "scndSmallestImage.bin")
  val imageId2: String = scala.util.Properties.envOrElse("imageId2", "scndSmallestImage2.bin")
  val waitTimeForScndRollout: Int = scala.util.Properties.envOrElse("waitTimeForScndRollout", "30").toInt
  val simulationTime: Int = scala.util.Properties.envOrElse("simulationTime", "90").toInt
  private val pollingInterval: Int =scala.util.Properties.envOrElse("pollingIntervall", "5").toInt

  /**
   * Creates a list of all ids needed for the simulation.
   *
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
   * Registers a car with the given ID.
   */
  val register: ChainBuilder = exec(
    http("register device")
      .post(carProtocol + "/register/${id}")
      .check(status.is(200))
  )

  /**
   * Deletes all repos.
   */
  val deleteRepos: ScenarioBuilder = scenario("delete repos")
    .exec(
      http("delete repos")
        .post(Metadata.adminProtocol + "deleteRepos")
        .check(status.is(200))
    )
  /**
   * Implements the polling process by starting after a random waiting time inside a polling interval.
   *
   * Checks if an update is available and then calls for a new image if available.
   */
  val polling: ChainBuilder = {
    pause(0.seconds,pollingInterval.seconds)
    .during(simulationTime.seconds) {
      pause(pollingInterval.seconds)
      .exec(
        http("check if update available")
          .get(carProtocol + "/newImageAvailable/" + "${id}")
          .check(status.is(200))
          .check(jmesPath("availability").saveAs("availability"))
      ).doIfEquals("${availability}", "true") {
          exec(
            http("get New Image")
              .get((carProtocol + "/getNewImage/" + "${id}"))
              .check(status.is(200))
              .requestTimeout(3.minutes)
          )
        }
    }
  }


  /**
   * Starts the first rollout by creating a group with ID 0 and starting the rollout itself.
   */
  val cFirstGroupRollout: ChainBuilder = exec(
    http("createGroup")
      .post(adminProtocol + "createGroup/0/" + createStringIdList())
      .check(status.is(200))
  ).exec(
    http("createRollout")
      .post(
        adminProtocol + "rollout/" + imageId1 + "/0/polling_intervals,-,non-delta"
      )
      .check(status.is(200))
  )
  /**
   * Starts the second rollout after a certain number of seconds.
   */
  val cSecondRollout: ChainBuilder =
    pause(waitTimeForScndRollout.seconds)
      .exec(
        http("create second Rollout")
          .post(
            adminProtocol + "rollout/" + imageId2 + "/0/polling_intervals,-,delta"
          )
      )

}
