package akka.avionics

import akka.actor.Actor

object IsolatedLifeCycleSupervisor {
  case object WaitForStart
  case object Started
}

trait IsolatedLifeCycleSupervisor extends Actor {
  import IsolatedLifeCycleSupervisor._

  def receive = {
    case WaitForStart => sender ! Started
    case m => throw new Exception(s"Don't call ${self.path.name} directly ($m).")
  }

  def childStarter(): Unit //to be implemented by subclass
  final override def preStart() { childStarter() }  //only start children when we are started
  final override def postRestart(reason: Throwable) { }  //Don't call preStart(), which would be default behavior
  final override def preRestart(reason: Throwable, message: Option[Any]) { }  //Don't stop the children, which would be default behavior 
}