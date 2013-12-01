package akka.avionics

import akka.actor.{ Actor, ActorRef, ActorLogging }

trait PilotProvider {
  def newPilot(plane: ActorRef, autopilot: ActorRef, controls: ActorRef, altimeter: ActorRef): Actor = new Pilot(plane, autopilot, controls, altimeter)
  def newCopilot(plane: ActorRef, altimeter: ActorRef): Actor = new Copilot(plane, altimeter)
  def newAutopilot(): Actor = new Autopilot()
}

object Pilot {
  case object ReadyToGo
  case object RelinquishControl
}

class Pilot(plane: ActorRef, autopilot: ActorRef, var controls: ActorRef, altimeter: ActorRef) extends Actor with ActorLogging {
  import Pilot._
  import Plane._
  var controlSurface: ActorRef = context.system.deadLetters
  var copilot: ActorRef = context.system.deadLetters
  var autopilot: ActorRef = context.system.deadLetters
  var copilotName = context.system.settings.config.getString("akka.avionics.flightcrew.copilotName")

  def receive = {
    case ReadyToGo =>
      plane ! GiveMeControl
      copilot = context.actorFor("../" + copilotName)
    case Controls(cs) =>
      controlSurface = cs
  }
}