package withBroker


import com.rabbitmq.client.BuiltinExchangeType
import io.gatling.core.Predef._
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import ru.tinkoff.gatling.amqp.Predef._
import ru.tinkoff.gatling.amqp.protocol.AmqpProtocolBuilder
import io.gatling.http.Predef._

import scala.language.postfixOps
import io.gatling.core.Predef.scenario
import io.gatling.http.Predef.{http, status}
import ru.tinkoff.gatling.amqp.Predef.{amqp, queue, rabbitmq}
import ru.tinkoff.gatling.amqp.protocol

import scala.concurrent.duration.DurationInt


object Metadata {

//variables
  val host: String = scala.util.Properties.envOrElse("wBHost", "ota-tester-service.ota-tester.svc.cluster.local")
  private val brokerHost: String = scala.util.Properties.envOrElse("brokerHost", "rabbit-service.ota-tester.svc.cluster.local")

  val numberOfCars: Int = scala.util.Properties.envOrElse("numberOfCars", "1").toInt
  val numberOfCarsFirstBunch: Int = scala.util.Properties.envOrElse("numberOfCarsFirstBunch", "20").toInt
  val numberOfCarsScndBunch: Int = scala.util.Properties.envOrElse("numberOfCarsSecondBunch", "10").toInt
  val numberOfCarsThirdBunch: Int = scala.util.Properties.envOrElse("numberOfCarsThirdBunch", "10").toInt
  val numberOfCarsForthBunch: Int = scala.util.Properties.envOrElse("numberOfCarsForthBunch", "10").toInt
  val numberOfCarsFifthBunch: Int = scala.util.Properties.envOrElse("numberOfCarsFifthBunch", "10").toInt

  val waitTimeUntilPull: Int = scala.util.Properties.envOrElse("waitTimeUntilPull", "5").toInt


  val carProtocol: String = "http://" + host + "/car/"
  val adminProtocol: String = "http://" + host + "/admin/"

  val ids: List[Int] = List.range(0, numberOfCars)
  val idsFirstBunch: List[Int] = List.range(0, numberOfCarsFirstBunch)
  val idsSecondBunch: List[Int] = List.range(numberOfCarsFirstBunch, numberOfCarsFirstBunch + numberOfCarsScndBunch)
  val idsThirdBunch: List[Int] = List.range(numberOfCarsFirstBunch+numberOfCarsScndBunch, numberOfCarsFirstBunch+numberOfCarsScndBunch + numberOfCarsThirdBunch)
  val idsForthBunch: List[Int] = List.range(numberOfCarsFirstBunch+numberOfCarsScndBunch + numberOfCarsThirdBunch, numberOfCarsFirstBunch+numberOfCarsScndBunch + numberOfCarsThirdBunch + numberOfCarsForthBunch)
  val idsFifthBunch: List[Int] = List.range(numberOfCarsFirstBunch+numberOfCarsScndBunch + numberOfCarsThirdBunch + numberOfCarsForthBunch,numberOfCarsFirstBunch+numberOfCarsScndBunch + numberOfCarsThirdBunch + numberOfCarsForthBunch + numberOfCarsFifthBunch)


  val imageId1: String = scala.util.Properties.envOrElse("imageId1", "scndSmallestImage.bin")
  val imageId2: String = scala.util.Properties.envOrElse("imageId2", "scndSmallestImage2.bin")

  val waitTimeForScndRollout: Int = scala.util.Properties.envOrElse("waitTimeForScndRollout", "0").toInt

  private val inQueue: protocol.AmqpQueue = queue("in")
  private val topic: protocol.AmqpExchange = exchange("test_queue_in", BuiltinExchangeType.TOPIC)

  /**
   * Creates a list of string ids for every id needed.
   * @return
   */
  def createStringIdList(): String = {
    var idString: String = ""
    val stringBuilder: StringBuilder = new StringBuilder();
    ids.foreach(id => {
      stringBuilder.append(id + ",")
    })
    idString = stringBuilder.toString()
    idString

  }

  /**
   * Creates a string list for the first cars that are added to the group.
   * @return
   */
  def createStringIdListFirstBunch(): String = {
    var idString: String = ""
    val stringBuilder: StringBuilder = new StringBuilder();
    idsFirstBunch.foreach(id => {
      stringBuilder.append(id + ",")
    })
    idString = stringBuilder.toString()
    idString

  }


  /**
   * Returns the string id list for the ith bunch of cars added to the group.
   * @param i
   * @return
   */
  private def idOfIthBunch(i: Int): List[Int] = {
    if (i == 1) {
      return idsFirstBunch
    } else if (i == 2) {
      return idsSecondBunch
    } else if (i == 3) {
      return idsThirdBunch
    } else if (i == 4) {
      return idsForthBunch
    } else if (i == 5) {
      return idsFifthBunch
    }
    idsFifthBunch
  }

  /**
   * debug method
   * @param i
   * @return
   */
  def createStringIdListIthBunch(i: Int): String = {
    var idString: String = ""
    val stringBuilder: StringBuilder = new StringBuilder();
    idOfIthBunch(i).foreach(id => {
      stringBuilder.append(id + ",")
    })
    idString = stringBuilder.toString()
    println(idsFirstBunch)
    println(idsSecondBunch)
    println(idsThirdBunch)
    println(idsForthBunch)
    println(idsFifthBunch)
    idString
  }

  /**
   * Creates an amqp builder for the registration of the cars. The queues that are needed are declared here.
   * @return
   */
  def createAmqpForRegister(): AmqpProtocolBuilder = {
    var amqpBuilder: AmqpProtocolBuilder = amqp.connectionFactory(
      rabbitmq
        .host(brokerHost)
        .port(5672)
        .username("guest")
        .password("guest")
        .vhost("/")
    ).usePersistentDeliveryMode
    for (i <- 0 until numberOfCars) {
      amqpBuilder = amqpBuilder.declare(queue(i.toString, durable = false, exclusive = false, autoDelete = false))
    }

    amqpBuilder
  }

  /**
   * Creates an amqp builder for the registration of the cars in continuous mode. The queues that are needed are declared here.
   *
   * @return
   */
  def createAmqpForRegisterContinuous(): AmqpProtocolBuilder = {
    var amqpBuilder: AmqpProtocolBuilder = amqp.connectionFactory(
      rabbitmq
        .host(brokerHost)
        .port(5672)
        .username("guest")
        .password("guest")
        .vhost("/")
    ).usePersistentDeliveryMode
    for (i <- 0 until numberOfCarsFirstBunch + numberOfCarsScndBunch + numberOfCarsThirdBunch + numberOfCarsForthBunch + numberOfCarsFifthBunch) {
      amqpBuilder = amqpBuilder.declare(queue(i.toString, durable = false, exclusive = false, autoDelete = false))
    }

    amqpBuilder
  }

  /**
   * Adds a predefined bunch of cars to the group.
   * @param i number of the bunch
   * @return
   */
  def addCarsLater(i: Int): ScenarioBuilder = {
    scenario(s"add cars later $i")
      .exec(
        http(s"add cars later $i")
          .post(Metadata.adminProtocol + "addCarsToGroup/0/" + Metadata.createStringIdListIthBunch(i))
          .check(status.is(200))
      )//.pause(10)
  }

  /**
   * Creates the amqp builder for the process of listening to the queues
   */
  val amqpForListener: AmqpProtocolBuilder = amqp
    .connectionFactory(
      rabbitmq
        .host(brokerHost)
        .port(5672)
        .username("guest")
        .password("guest")
        .vhost("/")
    ).usePersistentDeliveryMode
    .replyTimeout(180000)
    .declare(topic)
    .declare(inQueue)
    .declare(queue("out"))
    .bindQueue(inQueue, topic, "we")

  /**
   * Registers the cars to the broker by publishing a message to a dead exchange.
   */
  val registerToBroker: ScenarioBuilder = scenario("register to broker")
    .exec(
      amqp("register Cars to broker").publish
        .queueExchange("test")
        .textMessage("test")
    )
  /**
   * Deletes all entries in the repos by calling deleteRepos.
   */
  val deleteRepos: ScenarioBuilder = scenario("delete repos")
    .exec(
      http("delete repos")
        .post(Metadata.adminProtocol + "deleteRepos")
        .check(status.is(200))
    )

  /**
   * Registers cars in the database.
   */
  val registerCars: ChainBuilder = exec(
    http("register device")
      .post(carProtocol + "/register/${id}")
      .check(status.is(200))
  )



}
