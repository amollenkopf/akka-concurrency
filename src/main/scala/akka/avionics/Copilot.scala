package akka.avionics

import akka.actor.{ Actor, ActorRef, Terminated }

class Copilot(plane: ActorRef, altimeter: ActorRef) extends Actor {
  import Pilot._
  import Plane._

  var controls: ActorRef = context.system.deadLetters
  var pilot: ActorRef = context.system.deadLetters
  var autopilot: ActorRef = context.system.deadLetters
  var pilotName = context.system.settings.config.getString("akka.avionics.flightcrew.pilotName")

  def receive = {
    case ReadyToGo =>
      autopilot = context.actorFor("../Autopilot")
      pilot = context.actorFor("../"+pilotName)
      context.watch(pilot)
    case Terminated(_) =>
      plane ! GiveMeControl
  }
}