package withBroker

import io.gatling.core.Predef._
import io.gatling.core.feeder.BatchableFeederBuilder
import io.gatling.core.structure.ScenarioBuilder
import ru.tinkoff.gatling.amqp.Predef._
import io.gatling.http.Predef._

import scala.language.postfixOps

class SimPushDefaultNonDelta extends Simulation {

  val feederForRegister: BatchableFeederBuilder[String] = csv("ids.csv").queue
  val feederForListener: BatchableFeederBuilder[String] = csv("ids.csv").queue




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
          Metadata.adminProtocol + "rollout/"+Metadata.imageId1+"/0/push,snapshot,non-delta"
        )
        .check(status.is(200))
    )

  private val registerTheCarsFeed = scenario("register cars")
    .feed(feederForRegister)
    .exec(Metadata.registerCars)


  setUp(Metadata.registerToBroker.inject(atOnceUsers(1)).protocols(Metadata.createAmqpForRegister())
    .andThen(Metadata.deleteRepos.inject(atOnceUsers(1)))
    .andThen(registerTheCarsFeed.inject(atOnceUsers(Metadata.numberOfCars)))
    .andThen(startListener.inject(atOnceUsers(Metadata.numberOfCars)).protocols(Metadata.amqpForListener), cGroupRollout.inject(atOnceUsers(1))))

}
