package akka.avionics

import akka.actor.Actor
import scala.concurrent.duration._

trait AttendantResponsiveness { //different flight attendants have different levels of responsiveness
  val maxResponseTime: Int = 1000 //in milliseconds
  val responseDuration = scala.util.Random.nextInt(maxResponseTime).millis
}

object FlightAttendant {
  case class GetDrink(drinkName: String)
  case class Drink(drinkName: String)
  def apply() = new FlightAttendant with AttendantResponsiveness
}

class FlightAttendant extends Actor { this: AttendantResponsiveness =>
  import FlightAttendant._
  implicit val ec = context.dispatcher //configuring the execution context for the scheduler

  def receive = {
    case GetDrink(drinkName) => //flight attendants eventually respond
      //println(s"MRT = $maxResponseTime")
      //println(s" RD = $responseDuration")
      //println(s" Sending Drink($drinkName)")
      context.system.scheduler.scheduleOnce(responseDuration, sender, Drink(drinkName))
  }
}