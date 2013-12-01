package akka.avionics

import akka.actor.{ Actor, ActorRef }

object ControlSurface { //carries messages for controlling the plane
  case class StickBack(amount: Float) //amount is any value between -1 and 1
  case class StickForward(amount: Float) //the altimeter ensures out of range values or truncated
}

class ControlSurface(altimeter: ActorRef) extends Actor { //sends control messages from the plane to the altimeter 
  import ControlSurface._
  import Altimeter._

  def receive = {
    case StickBack(amount) => altimeter ! RateChange(amount) //pilot pulled stick back by a certain amount to climb
    case StickForward(amount) => altimeter ! RateChange(-1 * amount)  //pilot pushed stick forward by a certain amount to descend
  }
}