package akka.avionics

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.pattern.ask
import akka.routing.FromConfig
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._

object Plane {
  case object GiveMeControl //returns the control surface to the actor that asks for it
  case object LostControl
  case object TakeControl
  case class Controls(controls: ActorRef)
  def apply() = new Plane with AltimeterProvider with HeadingIndicatorProvider with PilotProvider with FlightAttendantProvider
}

class Plane extends Actor with ActorLogging {
  this: AltimeterProvider with HeadingIndicatorProvider with PilotProvider with FlightAttendantProvider =>
  import Altimeter._
  import HeadingIndicator._
  import Plane._

  val configPath = "akka.avionics.flightcrew"
  val config = context.system.settings.config
  val pilotName = config.getString(s"$configPath.pilotName")
  val copilotName = config.getString(s"$configPath.copilotName")
  val leadAttendantName = config.getString(s"$configPath.leadAttendantName")
  implicit val askTimeout = Timeout(1.seconds)

  def actorForControls(name: String) = context.actorFor("Equipment/" + name)
  def actorForPilots(name: String) = context.actorFor("Pilots/" + name)

  def startEquipment() = {
    val plane = self
    val equipment = context.actorOf(Props(new IsolatedResumeSupervisor with OneForOneStrategyFactory {
      def childStarter() = {
        val altimeter = context.actorOf(Props(newAltimeter), "Altimeter")
        val headingIndicator = context.actorOf(Props(newHeadingIndicator), "HeadingIndicator")
        context.actorOf(Props(newAutopilot()), "Autopilot")
        context.actorOf(Props(new ControlSurface(plane, altimeter, headingIndicator)), "ControlSurface")
      }
    }), "Equipment")
    Await.result(equipment ? IsolatedLifeCycleSupervisor.WaitForStart, 1.second)
  }

  def startPeople() = {
    val plane = self
    val autopilot = actorForControls("Autopilot")
    val controls = actorForControls("ControlSurface")
    val altimeter = actorForControls("Altimeter")
    val people = context.actorOf(Props(new IsolatedStopSupervisor with OneForOneStrategyFactory {
      def childStarter() = {
        val copilot = context.actorOf(Props(newCopilot(plane, altimeter)), copilotName)
        val pilot = context.actorOf(Props(newPilot(plane, autopilot, controls, altimeter)), pilotName)
        val leadAttendant = context.actorOf(Props(newFlightAttendant).withRouter(FromConfig()), "FlightAttendantRouter")
        val passengers = context.actorOf(Props(PassengerSupervisor(leadAttendant)), "Passengers")
      }
    }), "People")
    Await.result(people ? IsolatedLifeCycleSupervisor.WaitForStart, 1.second)
  }

  override def preStart() = {
    startEquipment()
    startPeople()

    actorForControls("Altimeter") ! EventSource.RegisterListener(self)
    actorForPilots(pilotName) ! Pilot.ReadyToGo
    actorForPilots(copilotName) ! Pilot.ReadyToGo
  }

  def receive = {
    case GiveMeControl =>
      log.info(s"Plane giving control to ${sender.path.name}, ${sender.toString}")
      sender ! actorForControls("ControlSurface") //Controls(...)
    case AltitudeUpdate(altitude) =>
      log info (s"Altitude is now: $altitude")
    case LostControl =>
      actorForControls("Autopilot") ! TakeControl
  }
}

object PlanePathChecker {
  def main(args: Array[String]) {
    val system = akka.actor.ActorSystem("PlaneSimulation")
    val plane = system.actorOf(Props(Plane()), "Plane")
    Thread.sleep(2000)
    system shutdown
  }
}