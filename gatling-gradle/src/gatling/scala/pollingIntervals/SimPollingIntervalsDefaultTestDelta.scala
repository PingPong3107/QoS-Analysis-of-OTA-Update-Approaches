package pollingIntervals

import io.gatling.core.Predef._
import io.gatling.core.feeder.BatchableFeederBuilder
import io.gatling.core.structure.ScenarioBuilder

/**
 * Polling Intervals Delta simulation.
 */
class SimPollingIntervalsDefaultTestDelta extends Simulation {


  //feeders needed for the polling and registering
  val feederForRegister: BatchableFeederBuilder[String] = csv("ids.csv").queue
  val feederForPolling: BatchableFeederBuilder[String] = csv("ids.csv").queue

  //Here only the chains are put into scenarios and fed with data.

  val registerScen: ScenarioBuilder = scenario("register")
    .feed(feederForRegister)
    .exec(Metadata.register)

  val adminDeletesRepos: ScenarioBuilder = scenario("Admin deletes repos")
    .exec(Metadata.deleteRepos)

  val pollingScen: ScenarioBuilder = scenario("Polling")
    .feed(feederForPolling)
    .exec(Metadata.polling)

  val createGroupAndRollout: ScenarioBuilder = scenario("Admin creates group and first rollout")
    .exec(Metadata.cFirstGroupRollout)

  private val createSecondRollout = scenario("Admin creates second rollout")
    .exec(Metadata.cSecondRollout)

  setUp(
    adminDeletesRepos
      .inject(atOnceUsers(1))
      .andThen(registerScen.inject(atOnceUsers(Metadata.numberOfCars)))//cars are registered
      .andThen(createGroupAndRollout.inject(atOnceUsers(1)))//admin creates group and rollout
      .andThen(pollingScen.inject(atOnceUsers(Metadata.numberOfCars)),createSecondRollout.inject(atOnceUsers(1)))//polling starts and after a certain time the second rollout is started as a delta rollout
  )

}
