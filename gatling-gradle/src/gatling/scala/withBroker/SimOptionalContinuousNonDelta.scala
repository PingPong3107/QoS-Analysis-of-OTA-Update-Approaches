package withBroker

import io.gatling.core
import io.gatling.core.Predef.{Simulation, atOnceUsers, bodyString, csv, scenario}
import io.gatling.core.feeder.BatchableFeederBuilder
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef.{http, status}
import ru.tinkoff.gatling.amqp.Predef.amqp
import io.gatling.core.Predef._
import ru.tinkoff.gatling.amqp.Predef._
import io.gatling.http.Predef._
import withBroker.Metadata.{adminProtocol, deleteRepos, numberOfCars, numberOfCarsFirstBunch, numberOfCarsScndBunch, registerCars, waitTimeForScndRollout}

import scala.concurrent.duration.DurationInt

class SimOptionalContinuousNonDelta extends Simulation {

  val feederForRegister: BatchableFeederBuilder[String] = csv("ids.csv").queue
  val feederForListener: BatchableFeederBuilder[String] = csv("ids.csv").queue


  /**
   * Creates a group and a rollout.
   */
  val groupRollout: ScenarioBuilder = scenario("create group and rollout")
    .exec(
      http("createGroup")
        .post(Metadata.adminProtocol + "createGroup/0/" + Metadata.createStringIdListFirstBunch())
        .check(status.is(200))
    )
    .exec(
      http("createRollout")
        .post(
          Metadata.adminProtocol + "rollout/" + Metadata.imageId1 + "/0/optional,continuous,non-delta"
        )
        .check(status.is(200))
    )
  /**
   * Starts listening to a queue, when a new update is available it gets pulled.
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
    ).exec(
    http("call getNewImage")
      .get((Metadata.carProtocol + "/getNewImage/" + "${id}"))
      .check(status.is(200))
  )
  /**
   * Creates listener for the second run. When the second bunch of cars is added a new listener is needed, such that the waiting times are grouped by bunch. If the same method was used all test listening times would be added to one mean that is used for all listening.
   */
  val startListener2: ScenarioBuilder = scenario("start listener 2")
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
    )
    .exec(
      http("call getNewImage for added cars")
        .get((Metadata.carProtocol + "/getNewImage/" + "${id}"))
        .check(status.is(200))
    )

  /**
   * Starts ith listening round.
   * @param i
   * @return
   */
  def startListener(i: String): ScenarioBuilder = {
    scenario(s"start listener $i")
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
      )
      .exec(
        http("call getNewImage for added cars")
          .get((Metadata.carProtocol + "/getNewImage/" + "${id}"))
          .check(status.is(200))
          .requestTimeout(3.minutes)
      )
  }

  /**
   * Registers cars at the database.
   */
  val registerCars: ScenarioBuilder = scenario("register cars")
    .feed(feederForRegister)
    .exec(Metadata.registerCars)


  setUp(Metadata.registerToBroker.inject(atOnceUsers(1)).protocols(Metadata.createAmqpForRegisterContinuous())//registers all cars at the broker
    .andThen(Metadata.deleteRepos.inject(atOnceUsers(1)))//deletes all repos at the server
    .andThen(registerCars.inject(atOnceUsers(Metadata.numberOfCarsFirstBunch + Metadata.numberOfCarsScndBunch + Metadata.numberOfCarsThirdBunch + Metadata.numberOfCarsForthBunch + Metadata.numberOfCarsFifthBunch)))//registers all cars at the database
    .andThen(groupRollout.inject(atOnceUsers(1)), startListener(1.toString).inject(atOnceUsers(Metadata.numberOfCarsFirstBunch)).protocols(Metadata.amqpForListener))//creates first car bunch and a continuous rollout
    .andThen(Metadata.addCarsLater(2).inject(atOnceUsers(1)), startListener(2.toString).inject(atOnceUsers(Metadata.numberOfCarsScndBunch)).protocols(Metadata.amqpForListener))//adds second car bunch and starts listening
    .andThen(Metadata.addCarsLater(3).inject(atOnceUsers(1)), startListener(3.toString).inject(atOnceUsers(Metadata.numberOfCarsThirdBunch)).protocols(Metadata.amqpForListener))//adds third ...
    .andThen(Metadata.addCarsLater(4).inject(atOnceUsers(1)), startListener(4.toString).inject(atOnceUsers(Metadata.numberOfCarsForthBunch)).protocols(Metadata.amqpForListener))//...
    .andThen(Metadata.addCarsLater(5).inject(atOnceUsers(1)), startListener(5.toString).inject(atOnceUsers(Metadata.numberOfCarsFifthBunch)).protocols(Metadata.amqpForListener))//...
  )



}


