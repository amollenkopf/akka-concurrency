package akka.avionics

import akka.actor.{ ActorSystem, ActorRef, Props }
import akka.testkit.{ TestKit, ImplicitSender, TestFSMRef, TestProbe }
import org.scalatest.WordSpec
import org.scalatest.matchers.Matchers

class FlightBehaviorSpec extends TestKit(ActorSystem("FlightBehaviorSpec"))
  with ImplicitSender with WordSpec with Matchers {
  import Altimeter._
  import ControlSurface._
  import HeadingIndicator._
  import FlyingBehavior._
  import Plane._
  def nilActor: ActorRef = TestProbe().ref
  val target: CourseTarget = CourseTarget(30000, 10, 100)
  
  def fsm(plane: ActorRef = nilActor, heading: ActorRef = nilActor, altimeter: ActorRef = nilActor) = {
  	TestFSMRef(new FlyingBehavior(plane, heading, altimeter))
  }
  
  "FlyingBehavior" should {
    "start in the idle state and with Uninitialized data" in {
      val a = fsm()
      assert(a.stateName == Idle, "Actor state name is Idle")
      assert(a.stateData == Uninitialized, "Actor state data is Uninitialized")
    }
  }
  
  "PreparingToFly state" should {
    "stay in PreparingToFly state when only a HeadingUpdate is received" in {
      val a = fsm()
      a ! Fly(target)
      a ! HeadingUpdate(20)
      assert(a.stateName == PreparingToFly, "Actor state name is PreparingToFly")
      val sd = a.stateData.asInstanceOf[FlightData]
      assert(sd.status.altitude == -1, "Actor state data altitude is -1")
      assert(sd.status.heading == 20, "Actor state data heading is 20")
    }
    "move to Flying state when all parts are received" in {
      val a = fsm()
      a ! Fly(target)
      a ! HeadingUpdate(20)
      a ! AltitudeUpdate(50)
      a ! Controls(testActor)
      assert(a.stateName == Flying, "Actor state name is Flying")
      val sd = a.stateData.asInstanceOf[FlightData]
      assert(sd.controls == testActor, "Actor state data controls is the testActor")
      assert(sd.status.heading == 20, "Actor state data heading is 20")
      assert(sd.status.altitude == 50, "Actor state data altitude is 50")
    } 
  }

  "Transitioning to the Flying state" should {
    "create the Adjustment time" in {
      val a = fsm()
      a.setState(PreparingToFly)
      a.setState(Flying)
      assert(a.timerActive_?("Adjustment") == true, "Adjustment timer is active")
    }
  }
}