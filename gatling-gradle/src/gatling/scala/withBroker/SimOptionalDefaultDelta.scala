package withBroker

import io.gatling.core.Predef._
import io.gatling.core.feeder.BatchableFeederBuilder
import io.gatling.core.structure.ScenarioBuilder
import ru.tinkoff.gatling.amqp.Predef._
import io.gatling.http.Predef._
import withBroker.Metadata.{adminProtocol, imageId2, waitTimeForScndRollout}
import scala.concurrent.duration._
import scala.language.postfixOps

class SimOptionalDefaultDelta extends Simulation {

  val feederForRegister: BatchableFeederBuilder[String] = csv("ids.csv").queue
  val feederForListener: BatchableFeederBuilder[String] = csv("ids.csv").queue
  val feederForListener2: BatchableFeederBuilder[String] = csv("ids.csv").queue
  /**
   * Registers cars to the service.
   */
  val registerCars: ScenarioBuilder = scenario("register to OTA-Service")
    .feed(feederForRegister)
    .exec(
      Metadata.registerCars
    )

  /**
   * Starts listening to the queues and pulling the new image if a new one is available
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
      .requestTimeout(3.minutes)
  )
  /**
   * Starts the second listening round for delta.
   */
  val startListenerSecondTime: ScenarioBuilder = scenario("start listener2")
    .feed(feederForListener2)
    .exec(
      amqp("listen to queue").requestReply
        .topicExchange("test_queue_in", "we")
        .replyExchange("${id}")
        .textMessage("test")
        .check(
          bodyString.exists,
          bodyString.is("{\"message\":\"New delta update available for car Group 0\"}")
        )
    ).exec(
    http("call getNewImage delta")
      .get((Metadata.carProtocol + "/getNewImage/" + "${id}"))
      .check(status.is(200))
      .requestTimeout(3.minutes)
  )

  /**
   * Creates a group and a rollout
   */
  private val groupRollout: ScenarioBuilder = scenario("create group and rollout")
    .exec(
      http("createGroup")
        .post(Metadata.adminProtocol + "createGroup/0/" + Metadata.createStringIdList())
        .check(status.is(200))
    ).pause(2.seconds)
    .exec(
      http("createRollout")
        .post(
          Metadata.adminProtocol + "rollout/" + Metadata.imageId1 + "/0/optional,continuous,non-delta"
        )
        .check(status.is(200))
    )

  /**
   * Creates the second group rollout for the delta mode.
   */
  private val scndGroupRollout: ScenarioBuilder = scenario("scnd rollout")
    .pause(waitTimeForScndRollout)
    .exec(
      http("createScndRollout")
        .post(
          adminProtocol + "rollout/" + imageId2 + "/0/optional,snapshot,delta"
        )
        .check(status.is(200))
    )

  setUp(Metadata.registerToBroker.inject(atOnceUsers(1)).protocols(Metadata.createAmqpForRegister())//register cars to the broker
    .andThen(Metadata.deleteRepos.inject(atOnceUsers(1)))//delete all repos at the server
    .andThen(registerCars.inject(atOnceUsers(Metadata.numberOfCars)))//register all cars to the database
    .andThen(startListener.inject(atOnceUsers(Metadata.numberOfCars)).protocols(Metadata.amqpForListener), groupRollout.inject(atOnceUsers(1)))//starts first rollout and listening
    .andThen(startListenerSecondTime.inject(atOnceUsers(Metadata.numberOfCars)).protocols(Metadata.amqpForListener), scndGroupRollout.inject(atOnceUsers(1))))//starts second rollout and listening


}
