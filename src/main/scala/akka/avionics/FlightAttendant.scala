package akka.avionics

import akka.actor.{ Actor, ActorRef, Cancellable }
import scala.concurrent.duration._

trait AttendantResponsiveness { //different flight attendants have different levels of responsiveness
  val maxResponseTime: Int = 1000 //in milliseconds
  val responseDuration = scala.util.Random.nextInt(maxResponseTime).millis
}

trait FlightAttendantProvider {
  def newFlightAttendant(): Actor = {
    new FlightAttendant with AttendantResponsiveness {
      override val maxResponseTime = 300000
    }
  }
}

object FlightAttendant {
  case class GetDrink(drinkName: String)
  case class Drink(drinkName: String)
  case class Assist(passenger: ActorRef)
  case object Busy
  case object Yes
  case object No
  def apply() = new FlightAttendant with AttendantResponsiveness
}

class FlightAttendant extends Actor { this: AttendantResponsiveness =>
  import FlightAttendant._
  implicit val ec = context.dispatcher //configuring the execution context for the scheduler

  case class DeliverDrink(drink: Drink) //signals drink delivery can happen
  var pendingDelivery: Option[Cancellable] = None
  def scheduleDelivery(drinkName: String): Cancellable = {
    context.system.scheduler.scheduleOnce(responseDuration, self, DeliverDrink(Drink(drinkName)))
  }

  def handleDrinkRequests: Receive = {
    case GetDrink(drinkName) =>
      pendingDelivery = Some(scheduleDelivery(drinkName))
      context.become(assistInjuredPassenger orElse handleSpecificPerson(sender))
    case Busy =>
      sender ! No
  }

  def assistInjuredPassenger: Receive = { //immediately assist injured passenger by serving them a 'Magic Healing Potion' 
    case Assist(passenger) =>
      pendingDelivery foreach { _.cancel() }
      pendingDelivery = None
      passenger ! Drink("Magic Healing Potion")
  }

  def handleSpecificPerson(person: ActorRef): Receive = {
    case GetDrink(drinkName) if sender == person =>
      pendingDelivery foreach { _.cancel() }
      pendingDelivery = Some(scheduleDelivery(drinkName))
    case DeliverDrink(drink) =>
      person ! drink
      pendingDelivery = None
      context.become(assistInjuredPassenger orElse handleDrinkRequests)
    case m: GetDrink =>
      context.parent forward m
    case Busy =>
      sender ! Yes
  }
  
  def receive = {
    case GetDrink(drinkName) => assistInjuredPassenger orElse handleDrinkRequests
  }
}