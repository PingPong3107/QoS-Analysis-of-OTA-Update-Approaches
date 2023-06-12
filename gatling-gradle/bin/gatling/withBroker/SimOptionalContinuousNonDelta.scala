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

  val registerCars: ScenarioBuilder = scenario("register cars")
    .feed(feederForRegister)
    .exec(Metadata.registerCars)



  setUp(Metadata.registerToBroker.inject(atOnceUsers(1)).protocols(Metadata.createAmqpForRegisterContinuous())
    .andThen(Metadata.deleteRepos.inject(atOnceUsers(1)))
    .andThen(registerCars.inject(atOnceUsers(Metadata.numberOfCarsFirstBunch + Metadata.numberOfCarsScndBunch + Metadata.numberOfCarsThirdBunch + Metadata.numberOfCarsForthBunch + Metadata.numberOfCarsFifthBunch)))
    .andThen(groupRollout.inject(atOnceUsers(1)), startListener(1.toString).inject(atOnceUsers(Metadata.numberOfCarsFirstBunch)).protocols(Metadata.amqpForListener))
    .andThen(Metadata.addCarsLater(2).inject(atOnceUsers(1)), startListener(2.toString).inject(atOnceUsers(Metadata.numberOfCarsScndBunch)).protocols(Metadata.amqpForListener))
    .andThen(Metadata.addCarsLater(3).inject(atOnceUsers(1)), startListener(3.toString).inject(atOnceUsers(Metadata.numberOfCarsThirdBunch)).protocols(Metadata.amqpForListener))
    .andThen(Metadata.addCarsLater(4).inject(atOnceUsers(1)), startListener(4.toString).inject(atOnceUsers(Metadata.numberOfCarsForthBunch)).protocols(Metadata.amqpForListener))
    .andThen(Metadata.addCarsLater(5).inject(atOnceUsers(1)), startListener(5.toString).inject(atOnceUsers(Metadata.numberOfCarsFifthBunch)).protocols(Metadata.amqpForListener))
  )


  //  setUp(Metadata.registerToBroker.inject(atOnceUsers(1)).protocols(Metadata.createAmqpForRegisterContinuous())
  //    .andThen(deleteRepos.inject(atOnceUsers(1)))
  //    .andThen(registerCars.inject(atOnceUsers(numberOfCarsFirstBunch + numberOfCarsScndBunch)))
  //    .andThen(groupRollout.inject(atOnceUsers(1)))
  //    .andThen(Metadata.addCarsLater.inject(atOnceUsers(1)))
  //  )

}


