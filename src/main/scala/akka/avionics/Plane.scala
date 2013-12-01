package akka.avionics

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._

object Plane {
  case object GiveMeControl //returns the control surface to the actor that asks for it
  case class Controls(controls: ActorRef)
  def apply() = new Plane with AltimeterProvider with PilotProvider with LeadFlightAttendantProvider
}

class Plane extends Actor with ActorLogging { this: AltimeterProvider with PilotProvider with LeadFlightAttendantProvider =>
  import Altimeter._
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
        val alt = context.actorOf(Props(newAltimeter), "Altimeter")
        context.actorOf(Props(newAutopilot()), "Autopilot")
        context.actorOf(Props(new ControlSurface(alt)), "ControlSurface")
      }
    }), "Equipment")
    Await.result(equipment ? IsolatedLifeCycleSupervisor.WaitForStart, 1.second)
  }

  def startPilots() = {
    val plane = self
    val autopilot = actorForControls("Autopilot")
    val controls = actorForControls("ControlSurface")
    val altimeter = actorForControls("Altimeter")
    val pilots = context.actorOf(Props(new IsolatedStopSupervisor with OneForOneStrategyFactory {
      def childStarter() = {
        val copilot = context.actorOf(Props(newCopilot(plane, altimeter)), copilotName)
        val pilot = context.actorOf(Props(newPilot(plane, autopilot, controls, altimeter)), pilotName)
      }
    }), "Pilots")
    context.actorOf(Props(newLeadFlightAttendant), leadAttendantName)
    Await.result(pilots ? IsolatedLifeCycleSupervisor.WaitForStart, 1.second)
  }

  override def preStart() = {
    startEquipment()
    startPilots()

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

//https://github.com/danluu/akka-concurrency-wyatt/