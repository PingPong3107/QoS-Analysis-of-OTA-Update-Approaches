package pollingIntervals

import io.gatling.core.Predef._
import io.gatling.core.feeder.BatchableFeederBuilder
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import io.gatling.http.Predef._

import scala.concurrent.duration._

class SimPollingIntervalsDefaultTestNonDelta extends Simulation {



  val feederForRegister: BatchableFeederBuilder[String] = csv("ids.csv").queue
  val feederForPolling: BatchableFeederBuilder[String] = csv("ids.csv").queue

  /**
   * Creates a group with id 0 and starts a rollout for that group in polling-intervals mode without delta.
   */
  val cGroupRollout: ChainBuilder = exec(
    http("createGroup")
      .post(Metadata.adminProtocol + "createGroup/0/" + Metadata.createStringIdList())
      .check(status.is(200))
  ).exec(
    http("createRollout")
      .post(
        Metadata.adminProtocol + "rollout/"+Metadata.imageId1+"/0/polling_intervals,-,non-delta"
      )
      .check(status.is(200))
  )

  // scenario
  val registerScen: ScenarioBuilder = scenario("register")
    .feed(feederForRegister)
    .exec(Metadata.register)

  val adminDeletesRepos: ScenarioBuilder = scenario("Admin deletes repos")
    .exec(Metadata.deleteRepos)

  val pollingScen: ScenarioBuilder = scenario("Polling")
    .feed(feederForPolling)
    .exec(Metadata.polling)

  val createGroupAndRollout: ScenarioBuilder = scenario("Admin creates Group and Rollout")
    .exec(cGroupRollout)

  setUp(
    adminDeletesRepos
      .inject(atOnceUsers(1))//delete repos
      .andThen(registerScen.inject(atOnceUsers(Metadata.numberOfCars)))//register cars
      .andThen(createGroupAndRollout.inject(atOnceUsers(1)))//create the group and rollout
      .andThen(pollingScen.inject(atOnceUsers(Metadata.numberOfCars)))//start polling
  )


}


