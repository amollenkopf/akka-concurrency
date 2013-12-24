package akka.avionics
import akka.actor.{ Actor, ActorRef, ActorLogging }
import scala.concurrent.duration._
import scala.language.postfixOps

trait DrinkRequestProbability {
  val askThreshold = 0.9f //possibility on if a passenger asks for a drink
  val requestMin = 20 minutes //minimum time between drink requests
  val requestUpper = 30 minutes //maximum time between drink requests
  def randomishTime(): FiniteDuration = requestMin + scala.util.Random.nextInt(requestUpper.toMillis.toInt).millis
}

trait PassengerProvider {
  def newPassenger(callButton: ActorRef): Actor =
    new Passenger(callButton) with DrinkRequestProbability
}

object Passenger {
  case object FastenSeatbelts
  case object UnfastenSeatbelts
  val SeatAssignment = """([\w\s_]+)-(\d+)-([A-Z])""".r
}

class Passenger(callButton: ActorRef) extends Actor with ActorLogging { this: DrinkRequestProbability =>
  import Passenger._
  import FlightAttendant.{ GetDrink, Drink }
  import scala.collection.JavaConverters._

  case object CallForDrink

  val SeatAssignment(myname, _, _) = self.path.name.replaceAllLiterally("_", " ")
  val drinks = context.system.settings.config.getStringList("akka.avionics.drinks").asScala.toIndexedSeq
  implicit val ec = context.dispatcher
  val scheduler = context.system.scheduler
  val random = scala.util.Random

  override def preStart() { self ! CallForDrink }

  def maybeSendDrinkRequest(): Unit = {
    if (random.nextFloat() > askThreshold) callButton ! GetDrink(drinks(random.nextInt(drinks.length)))
    scheduler.scheduleOnce(randomishTime(), self, CallForDrink)
  }

  def receive = {
    case CallForDrink => maybeSendDrinkRequest()
    case Drink(drinkName) => log.info(s"$myname received a $drinkName - Yum")
    case FastenSeatbelts => log.info(s"$myname fastening seatbelt.")
    case UnfastenSeatbelts => log.info(s"$myname unfastening seatbelt.")
  }
}
