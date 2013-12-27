package akka.avionics
import akka.actor.{ Actor, ActorRef }
import scala.concurrent.duration._
import scala.language.postfixOps

trait BeaconResolution {
  lazy val beaconInterval = 1 second
}

trait BeaconProvider {
  def newBeacon(heading: Float) = Beacon(heading)
}

object GenericPublisher {
  case class RegisterListener(actor: ActorRef)
  case class UnregisterListener(actor: ActorRef)
}

object Beacon {
  case class BeaconHeading(heading: Float)
  def apply(heading: Float) = new Beacon(heading) with BeaconResolution
}

class Beacon(heading: Float) extends Actor {
  this: BeaconResolution =>
  import Beacon._
  import GenericPublisher._

  case object Tick
  implicit val ec = context.dispatcher
  val ticker = context.system.scheduler.schedule(beaconInterval, beaconInterval, self, Tick)
  
  def receive = {
    case RegisterListener(actor) => context.system.eventStream.subscribe(actor, classOf[BeaconHeading])
    case UnregisterListener(actor) => context.system.eventStream.unsubscribe(actor)
    case Tick => context.system.eventStream.publish(BeaconHeading(heading))
  }
}