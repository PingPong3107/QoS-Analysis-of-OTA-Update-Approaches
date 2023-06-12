package withBroker

import io.gatling.core.Predef.atOnceUsers
import io.gatling.core.scenario.Simulation

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
class SimContinuousCompareScenarioPush extends Simulation{

  val feederForRegister: BatchableFeederBuilder[String] = csv("ids.csv").queue
  val feederForListener: BatchableFeederBuilder[String] = csv("ids.csv").queue

  val registerCars: ScenarioBuilder = scenario("register cars")
    .feed(feederForRegister)
    .exec(Metadata.registerCars)

//  def addCarsLaterAndNewRollout(i: Int): ScenarioBuilder = {
//    scenario(s"add cars later $i")
//      .exec(
//        http(s"add cars later $i")
//          .post(Metadata.adminProtocol + "addCarsToGroup/0/" + Metadata.createStringIdListIthBunch(i))
//          .check(status.is(200))
//      ).exec(
//      http("createRollout")
//        .post(
//          Metadata.adminProtocol + "rollout/" + Metadata.imageId1 + "/0/optional,snapshot,non-delta"
//        )
//        .check(status.is(200))
//    )
//  }

  def addCarsAndgroupRollout(i: Int): ScenarioBuilder = {
    val scen: ScenarioBuilder = scenario(s"Create $i th rollout")
    if (i == 1) {
      scen.exec(
        http("createGroup")
          .post(Metadata.adminProtocol + "createGroup/0/" + Metadata.createStringIdListFirstBunch())
          .check(status.is(200))
      )
        .exec(
          http("createRollout")
            .post(
              Metadata.adminProtocol + "rollout/" + Metadata.imageId1 + "/0/push,snapshot,non-delta"
            )
            .check(status.is(200))
        )
    } else {
      scen.exec(
        http(s"add cars later $i")
          .post(Metadata.adminProtocol + "addCarsToGroup/0/" + Metadata.createStringIdListIthBunch(i))
          .check(status.is(200))

      ).exec(
        http("createRollout")
          .post(
            Metadata.adminProtocol + "rollout/" + Metadata.imageId1 + "/0/push,snapshot,non-delta"
          )
          .check(status.is(200))
      )
    }

  }

  def startListener(i: String): ScenarioBuilder = {
    scenario(s"start listener $i")
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
  }



  setUp(Metadata.registerToBroker.inject(atOnceUsers(1)).protocols(Metadata.createAmqpForRegisterContinuous())
    .andThen(Metadata.deleteRepos.inject(atOnceUsers(1)))
    .andThen(registerCars.inject(atOnceUsers(Metadata.numberOfCarsFirstBunch + Metadata.numberOfCarsScndBunch + Metadata.numberOfCarsThirdBunch + Metadata.numberOfCarsForthBunch + Metadata.numberOfCarsFifthBunch)))
    .andThen(addCarsAndgroupRollout(1).inject(atOnceUsers(1)), startListener(1.toString).inject(atOnceUsers(Metadata.numberOfCarsFirstBunch)).protocols(Metadata.amqpForListener))
    .andThen(addCarsAndgroupRollout(2).inject(atOnceUsers(1)), startListener(2.toString).inject(atOnceUsers(Metadata.numberOfCarsScndBunch)).protocols(Metadata.amqpForListener))
    .andThen(addCarsAndgroupRollout(3).inject(atOnceUsers(1)), startListener(3.toString).inject(atOnceUsers(Metadata.numberOfCarsThirdBunch)).protocols(Metadata.amqpForListener))
    .andThen(addCarsAndgroupRollout(4).inject(atOnceUsers(1)), startListener(4.toString).inject(atOnceUsers(Metadata.numberOfCarsForthBunch)).protocols(Metadata.amqpForListener))
    .andThen(addCarsAndgroupRollout(5).inject(atOnceUsers(1)), startListener(5.toString).inject(atOnceUsers(Metadata.numberOfCarsFifthBunch)).protocols(Metadata.amqpForListener))
  )
}
