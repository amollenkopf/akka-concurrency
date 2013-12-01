package akka.avionics

import akka.actor.{ ActorInitializationException, ActorKilledException, SupervisorStrategy }
import scala.concurrent.duration.Duration

abstract class IsolatedResumeSupervisor(maxNumberOfRetries: Int = -1, withinTimeRange: Duration = Duration.Inf) 
  extends IsolatedLifeCycleSupervisor { this: SupervisionStrategyFactory =>

  override val supervisorStrategy = makeStrategy(maxNumberOfRetries, withinTimeRange) {
    case _: ActorInitializationException => SupervisorStrategy.Stop
    case _: ActorKilledException => SupervisorStrategy.Stop
    case _: Exception => SupervisorStrategy.Resume
    case _ => SupervisorStrategy.Escalate
  }
}

