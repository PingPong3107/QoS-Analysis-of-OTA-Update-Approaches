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

  val registerCars: ScenarioBuilder = scenario("register to OTA-Service")
    .feed(feederForRegister)
    .exec(
      Metadata.registerCars
    )


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
  private val scndGroupRollout: ScenarioBuilder = scenario("scnd rollout")
    .pause(waitTimeForScndRollout)
    .exec(
      http("createScndRollout")
        .post(
          adminProtocol + "rollout/" + imageId2 + "/0/optional,snapshot,delta"
        )
        .check(status.is(200))
    )

  setUp(Metadata.registerToBroker.inject(atOnceUsers(1)).protocols(Metadata.createAmqpForRegister())
    .andThen(Metadata.deleteRepos.inject(atOnceUsers(1)))
    .andThen(registerCars.inject(atOnceUsers(Metadata.numberOfCars)))
    .andThen(startListener.inject(atOnceUsers(Metadata.numberOfCars)).protocols(Metadata.amqpForListener), groupRollout.inject(atOnceUsers(1)))
    .andThen(startListenerSecondTime.inject(atOnceUsers(Metadata.numberOfCars)).protocols(Metadata.amqpForListener), scndGroupRollout.inject(atOnceUsers(1))))


}
