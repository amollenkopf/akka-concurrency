package akka.avionics

import akka.actor.{ SupervisorStrategy, OneForOneStrategy, AllForOneStrategy }
import akka.actor.SupervisorStrategy._
import scala.concurrent.duration.Duration

trait SupervisionStrategyFactory {
  def makeStrategy(maxNumberOfRetries: Int, withinTimeRange: Duration)(decider : Decider): SupervisorStrategy
}

trait OneForOneStrategyFactory extends SupervisionStrategyFactory {
  def makeStrategy(maxNumberOfRetries: Int, withinTimeRange: Duration)(decider : Decider): SupervisorStrategy = 
    OneForOneStrategy(maxNumberOfRetries, withinTimeRange)(decider)
}

trait AllForOneStrategyFactory extends SupervisionStrategyFactory {
  def makeStrategy(maxNumberOfRetries: Int, withinTimeRange: Duration)(decider : Decider): SupervisorStrategy =
    AllForOneStrategy(maxNumberOfRetries, withinTimeRange)(decider)
}