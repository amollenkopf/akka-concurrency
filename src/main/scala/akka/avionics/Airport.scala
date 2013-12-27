package akka.avionics
import akka.actor.{ Actor, ActorRef, Props }

trait AirportSpecifics {
  lazy val headingTo: Float = 0.0f
  lazy val altitude: Double = 0
}

object Airport {
  case class DirectFlyerToAirport(flyingBehavior: ActorRef)
  case class StopDirectingFlyer(FlyingBehavior: ActorRef)
  def toronto(): Props = Props(new Airport with BeaconProvider with AirportSpecifics {
    override lazy val headingTo: Float = 314.3f
    override lazy val altitude: Double = 26000
  })
}

class Airport extends Actor {
  this: AirportSpecifics with BeaconProvider =>
  import Airport._
  import Beacon._
  import FlyingBehavior._
  import GenericPublisher._

  val beacon = context.actorOf(Props(newBeacon(headingTo)), "Beacon")

  def receive = {
    case DirectFlyerToAirport(flyingBehavior) =>
      val oneHourFromNow = System.currentTimeMillis() + 60 * 60 * 1000
      val when = oneHourFromNow
      context.actorOf(Props(new MessageTransformer(from = beacon, to = flyingBehavior, {
        case BeaconHeading(heading) => Fly(CourseTarget(altitude, heading, when))
      })))
    case StopDirectingFlyer(_) =>
      context.children.foreach { context.stop }
  }
}

class MessageTransformer(from: ActorRef, to: ActorRef, transformer: PartialFunction[Any, Any]) extends Actor {
  import GenericPublisher._
  override def preStart() { from ! RegisterListener(self) }
  override def postStop() { from ! UnregisterListener(self) }
  def receive = {
    case m => to forward transformer(m)
  }
}