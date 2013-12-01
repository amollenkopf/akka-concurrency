package akka.avionics

import akka.actor.{ ActorSystem, Actor, ActorRef, Props, PoisonPill }
import akka.pattern.ask
import akka.testkit.{ TestKit, ImplicitSender, TestProbe }
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.scalatest.WordSpec
import org.scalatest.matchers.Matchers
import scala.concurrent.Await
import scala.concurrent.duration._

class FakePilot extends Actor {
  override def receive = {
    case _ =>
  }
}

object PilotDeathWatchSpec {
  val pilotName = "Jack"
  val copilotName = "Sud"
  val configStr = s"""
akka.avionics.flightcrew.pilotName = "$pilotName"
akka.avionics.flightcrew.copilotName = "$copilotName""""
}

class PilotDeathWatchSpec
  extends TestKit(ActorSystem("PilotDeathWatchSpec", ConfigFactory.parseString(PilotDeathWatchSpec.configStr)))
  with ImplicitSender with WordSpec with Matchers {
  import PilotDeathWatchSpec._
  import Plane._

  def nilActor: ActorRef = TestProbe().ref
  val root = "akka://PilotDeathWatchSpec"
  val pilotPath = root + s"/user/TestPilots/$pilotName"
  val copilotPath = root + s"/user/TestPilots/$copilotName"
  val autopilotPath = root + s"/user/TestPilots/Autopilot"

  def pilotsReadyToGo(): ActorRef = {
    implicit val askTimeout = Timeout(4.seconds)
    val a = system.actorOf(Props(new IsolatedStopSupervisor with OneForOneStrategyFactory {
      def childStarter() {
        context.actorOf(Props[FakePilot], pilotName)
        context.actorOf(Props(new Copilot(testActor, nilActor)), copilotName)
      }
    }), "TestPilots")
    Await.result(a ? IsolatedLifeCycleSupervisor.WaitForStart, 3.seconds)
    system.actorFor(copilotPath) ! Pilot.ReadyToGo
    a
  }

  "Copilot" should {
    "take control when the Pilot dies" in {
      pilotsReadyToGo()
      system.actorFor(pilotPath) ! PoisonPill  //killing pilot
      expectMsg(GiveMeControl)
      assert(lastSender.path.toString().equals(copilotPath))
    }
  }
}
