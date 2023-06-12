package elasticity

import io.gatling.core.Predef.{nothingFor, _}
import io.gatling.core.scenario.Simulation


import scala.concurrent.duration._
import scala.language.postfixOps


class SimBaseLoadThenRollout extends Simulation {
  setUp(Metadata.deleteRepos.inject(atOnceUsers(1))//delete repos
    .andThen(Metadata.register.inject(atOnceUsers(1)))//register the car
    .andThen(Metadata.cGroupAndRollout.inject(atOnceUsers(1)))//create group and rollout
    .andThen(
      Metadata.simplePoll.inject(constantUsersPerSec(Metadata.basePollLoad).during(6.minutes))//start poll load
      , Metadata.pullImage.inject(
        nothingFor(30.seconds),//no pulls
        constantUsersPerSec(Metadata.heavyLoad).during(30.seconds),//getNewImage calls
        nothingFor(30.seconds),//...
        constantUsersPerSec(Metadata.heavyLoad * 0.8).during(30.seconds),
        nothingFor(30.seconds),
        constantUsersPerSec(Metadata.heavyLoad * 0.6).during(30.seconds),
        nothingFor(30.seconds),
        constantUsersPerSec(Metadata.heavyLoad * 0.4).during(30.seconds),
        nothingFor(30.seconds),
        constantUsersPerSec(Metadata.heavyLoad * 0.2).during(30.seconds)
      )))
}
