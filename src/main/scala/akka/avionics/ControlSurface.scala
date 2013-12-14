package akka.avionics

import akka.actor.{ Actor, ActorRef }

object ControlSurface { //carries messages for controlling the plane
  case class StickBack(amount: Float) //amount is any value between -1 and 1
  case class StickForward(amount: Float) //the altimeter ensures out of range values or truncated
  case class StickLeft(amount: Float)
  case class StickRight(amount: Float)
  case class HasControl(actor: ActorRef)
}

class ControlSurface(plane: ActorRef, altimeter: ActorRef, headingIndicator: ActorRef) extends Actor {
  import ControlSurface._
  import Altimeter._
  import HeadingIndicator._

  def receive = controlledBy(context.system.deadLetters)

  def controlledBy(somePilot: ActorRef): Receive = {
    case StickBack(amount) if (sender == somePilot) => altimeter ! RateChange(amount) //climb
    case StickForward(amount) if (sender == somePilot) => altimeter ! RateChange(-1 * amount) //descend
    case StickLeft(amount) if (sender == somePilot) => headingIndicator ! BankChange(-1 * amount) //bank left
    case StickRight(amount) if (sender == somePilot) => headingIndicator ! BankChange(amount) //bank right
    case HasControl(actor) if (sender == plane) => context.become(controlledBy(actor))
  }
}