package withBroker


import io.gatling.core.Predef._
import io.gatling.core.feeder.BatchableFeederBuilder
import io.gatling.core.structure.ScenarioBuilder
import ru.tinkoff.gatling.amqp.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import io.gatling.core.feeder.BatchableFeederBuilder
import io.gatling.core.Predef.{Simulation, atOnceUsers, bodyString, csv, scenario}
import io.gatling.core.feeder.BatchableFeederBuilder
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef.{http, status}
import ru.tinkoff.gatling.amqp.Predef.amqp
import io.gatling.core.Predef._
import ru.tinkoff.gatling.amqp.Predef._
import io.gatling.http.Predef._
import withBroker.Metadata.{adminProtocol, numberOfCars, numberOfCarsFirstBunch, numberOfCarsScndBunch, waitTimeForScndRollout}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps


class SimOptionalDefaultNonDelta extends Simulation {


  val feederForRegister: BatchableFeederBuilder[String] = csv("ids.csv").queue
  val feederForListener: BatchableFeederBuilder[String] = csv("ids.csv").queue

  /**
   * Registers cars at the service
   */
  val registerCars: ScenarioBuilder = scenario("register to OTA-Service")
    .feed(feederForRegister)
    .exec(
      http("register device")
        .post(Metadata.carProtocol + "register/${id}")
        .check(status.is(200))
    )

  /**
   * Starts listening to the queues.
   */
  val startListener: ScenarioBuilder = scenario("start listener")
    .feed(feederForListener)
    .exec(
      amqp("listen to queue").requestReply
        .topicExchange("test_queue_in", "we")
        .replyExchange("${id}")
        .textMessage("test")
        .check(
          bodyString.exists,
          bodyString.is("{\"message\":\"New update available for car Group 0\"}")
        )
    ).pause(0, Metadata.waitTimeUntilPull)
    .exec(
      http("call getNewImage")
        .get((Metadata.carProtocol + "/getNewImage/" + "${id}"))
        .check(status.is(200))
        .requestTimeout(3.minutes)
    )

  /**
   * Creates a group and the rollout.
   */
  private val groupRollout: ScenarioBuilder = scenario("create group and rollout")
    .exec(
      http("createGroup")
        .post(Metadata.adminProtocol + "createGroup/0/" + Metadata.createStringIdList())
        .check(status.is(200))
    ) //.pause(2.seconds)
    .exec(
      http("createRollout")
        .post(
          Metadata.adminProtocol + "rollout/" + Metadata.imageId1 + "/0/optional,continuous,non-delta"
        )
        .check(status.is(200))
    )

  setUp(Metadata.registerToBroker.inject(atOnceUsers(1)).protocols(Metadata.createAmqpForRegister())//register cars at broker
    .andThen(Metadata.deleteRepos.inject(atOnceUsers(1)))//delete all repos
    .andThen(registerCars.inject(atOnceUsers(Metadata.numberOfCars)))//register all cars at the service
    .andThen(startListener.inject(atOnceUsers(Metadata.numberOfCars)).protocols(Metadata.amqpForListener), groupRollout.inject(atOnceUsers(1))))//start the rollout and listening to the queues

}
