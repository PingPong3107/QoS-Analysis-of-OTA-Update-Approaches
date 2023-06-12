package elasticity


import io.gatling.core.Predef._
import scala.concurrent.duration._


class SimGetNewImageElasticity extends Simulation {
//apply different levels of load
  setUp(Metadata.deleteRepos.inject(atOnceUsers(1))
    .andThen(Metadata.register.inject(atOnceUsers(1)))
    .andThen(Metadata.cGroupAndRollout.inject(atOnceUsers(1)))
    .andThen(
      Metadata.pullImage.inject(constantUsersPerSec(20).during(6.minutes))
        .throttle(
          reachRps(20).in(1.minutes),
          holdFor(1.minute),
          reachRps(10).in(1.minutes),
          holdFor(1.minutes),
          reachRps(15).in(1.minutes),
          holdFor(1.minutes)
        ))
  ).maxDuration(6.minutes).exponentialPauses
}
