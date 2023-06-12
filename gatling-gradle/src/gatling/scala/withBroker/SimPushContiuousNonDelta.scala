package withBroker

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

class SimPushContiuousNonDelta extends Simulation {
  val feederForRegister: BatchableFeederBuilder[String] = csv("ids.csv").queue
  val feederForListener: BatchableFeederBuilder[String] = csv("ids.csv").queue

  /**
   * Creates a group and a rollout
   */
  val groupRollout: ScenarioBuilder = scenario("create group and rollout")
    .exec(
      http("createGroup")
        .post(Metadata.adminProtocol + "createGroup/0/" + Metadata.createStringIdListFirstBunch())
        .check(status.is(200))
    ).pause(2.seconds)
    .exec(
      http("createRollout")
        .post(
          Metadata.adminProtocol + "rollout/" + Metadata.imageId1 + "/0/push,continuous,non-delta"
        )
        .check(status.is(200))
    )
  /**
   * Starts all listeners for the queues.
   */
  val startListener: ScenarioBuilder = scenario("start listener")
    .feed(feederForListener)
    .exec(
      amqp("listen to queue").requestReply
        .topicExchange("test_queue_in", "we")
        .replyExchange("${id}")
        .textMessage("test")
        .check(
          bodyString.exists
        )
    )
  /**
   * Starts the listeners for the second bunch
   */
  val startListener2: ScenarioBuilder = scenario("start listener 2")
    .feed(feederForListener)
    .exec(
      amqp("listen to queue second time").requestReply
        .topicExchange("test_queue_in", "we")
        .replyExchange("${id}")
        .textMessage("test")
        .check(
          bodyString.exists)
    )

  /**
   * Starts the listeners for the ith bunch
   * @param i
   * @return
   */
  def startListener(i: String): ScenarioBuilder = {
    scenario(s"start listener $i")
      .feed(feederForListener)//feeder counts further up, that's why the same feeder is used, only works if executed in the correct order
      .exec(
        amqp("listen to queue").requestReply
          .topicExchange("test_queue_in", "we")
          .replyExchange("${id}")
          .textMessage("test")
          .check(
            bodyString.exists,
          )
      )
  }

  /**
   * Register cars at broker
   */
  val registerCars: ScenarioBuilder = scenario("register cars")
    .feed(feederForRegister)
    .exec(Metadata.registerCars)

  setUp(Metadata.registerToBroker.inject(atOnceUsers(1)).protocols(Metadata.createAmqpForRegisterContinuous())//registers cars at the broker
    .andThen(Metadata.deleteRepos.inject(atOnceUsers(1)))//deletes all repos
    .andThen(registerCars.inject(atOnceUsers(Metadata.numberOfCarsFirstBunch + Metadata.numberOfCarsScndBunch + Metadata.numberOfCarsThirdBunch + Metadata.numberOfCarsForthBunch + Metadata.numberOfCarsFifthBunch)))//registers all cars at the database
    .andThen(groupRollout.inject(atOnceUsers(1)), startListener(1.toString).inject(atOnceUsers(Metadata.numberOfCarsFirstBunch)).protocols(Metadata.amqpForListener))//creates a group and starts a rollout
    .andThen(Metadata.addCarsLater(2).inject(atOnceUsers(1)), startListener(2.toString).inject(atOnceUsers(Metadata.numberOfCarsScndBunch)).protocols(Metadata.amqpForListener))//adds second car bunch to the group and starts listening
    .andThen(Metadata.addCarsLater(3).inject(atOnceUsers(1)), startListener(3.toString).inject(atOnceUsers(Metadata.numberOfCarsThirdBunch)).protocols(Metadata.amqpForListener))//adds third ...
    .andThen(Metadata.addCarsLater(4).inject(atOnceUsers(1)), startListener(4.toString).inject(atOnceUsers(Metadata.numberOfCarsForthBunch)).protocols(Metadata.amqpForListener))//...
    .andThen(Metadata.addCarsLater(5).inject(atOnceUsers(1)), startListener(5.toString).inject(atOnceUsers(Metadata.numberOfCarsFifthBunch)).protocols(Metadata.amqpForListener))//...
  )
}
