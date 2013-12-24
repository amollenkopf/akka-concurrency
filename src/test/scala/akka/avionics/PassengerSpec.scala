package akka.avionics
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.actor.{ ActorSystem, ActorRef, Props }
import akka.testkit.{ TestKit, ImplicitSender }
import org.scalatest.WordSpec
import org.scalatest.matchers.Matchers
import com.typesafe.config.ConfigFactory
import akka.avionics.Passenger._

trait TestDrinkRequestProbability extends DrinkRequestProbability {
  override val askThreshold = 0f
  override val requestMin = 0 millis
  override val requestUpper = 2 millis
}

object PassengerSpec {
  val config = ConfigFactory.parseString("""
    akka.avionics.drinks = [
      "akkarita",
      "scalatra"
    ]""")
}

class PassengerSpec extends TestKit(ActorSystem("PassengerSpec", PassengerSpec.config))
  with ImplicitSender with WordSpec with Matchers {
  import akka.event.Logging.Info
  import akka.testkit.TestProbe
  var seatNumber = 9
  
  def newPassenger(): ActorRef = {
    seatNumber += 1
    system.actorOf(Props(new Passenger(testActor) with TestDrinkRequestProbability), s"Eric_Mollenkopf-$seatNumber-B")
  }
  
  "Passengers" should {
    "fasten seatbelts when asked" in {
      val a = newPassenger()
      val p = TestProbe()
      system.eventStream.subscribe(p.ref, classOf[Info])
      a ! FastenSeatbelts
      p.expectMsgPF() {
        case Info(_, _, m) => {
          assert(m.toString.contains(" fastening seatbelt"), "Message did not contain fasten seatbelt")
        }
      }
    }
  }
}