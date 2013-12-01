package akka.avionics

import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ TestKit, TestActorRef, ImplicitSender }
import org.scalatest.WordSpec
import org.scalatest.matchers.Matchers
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

object TestFlightAttendant {
  def apply() = new FlightAttendant with AttendantResponsiveness
}

class FlightAttendantSpec extends TestKit(ActorSystem("FlightAttendantSpec"))
  with ImplicitSender with WordSpec with Matchers {
  import FlightAttendant._
  
  "FlightAttendant" should {
    "get a drink when asked" in {
      val a = TestActorRef(Props(TestFlightAttendant()))
      a ! GetDrink("Water")
      expectMsg(2.seconds, Drink("Water"))
    }
  }
}