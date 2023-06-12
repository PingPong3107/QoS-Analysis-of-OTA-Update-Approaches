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
import withBroker.Metadata.{adminProtocol, imageId2, numberOfCars, waitTimeForScndRollout}

import scala.language.postfixOps

class SimPushDefaultDelta extends Simulation {
  val feederForRegister: BatchableFeederBuilder[String] = csv("ids.csv").queue
  val feederForListener: BatchableFeederBuilder[String] = csv("ids.csv").queue
  val feederForListener2: BatchableFeederBuilder[String] = csv("ids.csv").queue

  /**
   * Starts listening to the queues
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
   * Starts listening to the queues for the delta rollout
   */
  val startListener2: ScenarioBuilder = scenario("start listener 2")
    .feed(feederForListener2)
    .exec(
      amqp("listen to queue delta").requestReply
        .topicExchange("test_queue_in", "we")
        .replyExchange("${id}")
        .textMessage("test")
        .check(
          bodyString.exists
        )
    )

  /**
   * Creates a group and a rollout.
   */
  val cGroupRollout: ScenarioBuilder = scenario("create group and rollout")
    .exec(
      http("createGroup")
        .post(Metadata.adminProtocol + "createGroup/0/" + Metadata.createStringIdList())
        .check(status.is(200))
    )
    //    .pause(2.seconds)
    .exec(
      http("createRollout")
        .post(
          Metadata.adminProtocol + "rollout/" + Metadata.imageId1 + "/0/push,snapshot,non-delta"
        )
        .check(status.is(200))
    )
  /**
   * Creates the second rollout used for delta.
   */
  private val scndGroupRollout: ScenarioBuilder = scenario("create second group and rollout")
    .pause(waitTimeForScndRollout)
    .exec(
      http("createScndRollout")
        .post(
          adminProtocol + "rollout/" + Metadata.imageId2 + "/0/push,snapshot,delta"
        )
        .check(status.is(200))
    )
  /**
   * Feeds the car register method with ids.
   */
  private val registerTheCarsFeed = scenario("register cars")
    .feed(feederForRegister)
    .exec(Metadata.registerCars)


  setUp(Metadata.registerToBroker.inject(atOnceUsers(1)).protocols(Metadata.createAmqpForRegister())//registers cars at broker
    .andThen(Metadata.deleteRepos.inject(atOnceUsers(1)))//deletes all repos
    .andThen(registerTheCarsFeed.inject(atOnceUsers(Metadata.numberOfCars)))//registers cars to the database
    .andThen(startListener.inject(atOnceUsers(Metadata.numberOfCars)).protocols(Metadata.amqpForListener),cGroupRollout.inject(atOnceUsers(1)))//Starts the first rollout and listening
    .andThen(startListener2.inject(atOnceUsers(Metadata.numberOfCars)).protocols(Metadata.amqpForListener), scndGroupRollout.inject(atOnceUsers(1)))//Starts the delta rollout and listening
  )


}
