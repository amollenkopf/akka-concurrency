package akka.avionics
import akka.actor.{ ActorSystem, Actor, ActorRef, Props }
import akka.testkit.{ TestKit, ImplicitSender }
import scala.concurrent.duration._
import scala.language.postfixOps
import com.typesafe.config.ConfigFactory
import org.scalatest.{ WordSpec, BeforeAndAfterAll }
import org.scalatest.matchers.Matchers

object PassengerSupervisorSpec {
  val config = ConfigFactory.parseString("""
    akka.avionics.passengers = [
      ["Ben", "01", "A"],
      ["Patrick", "02", "B"], 
      ["Jeff", "03", "C"], 
      ["Doug", "04", "A"], 
      ["Paul", "10", "B"]
    ]""")
}

trait TestPassengerProvider extends PassengerProvider {
  override def newPassenger(callButton: ActorRef): Actor =
    new Actor {
      def receive = {
        case m => callButton ! m
      }
    }
}

class PassengerSupervisorSpec extends TestKit(ActorSystem("PassengerSupervisorSpec", PassengerSupervisorSpec.config))
  with ImplicitSender with WordSpec with BeforeAndAfterAll with Matchers {
  import PassengerSupervisor._

  override def afterAll() {
    system.shutdown()
  }

  "PassengerSupervisor" should {
    "work" in {
      val a = system.actorOf(Props(new PassengerSupervisor(testActor) with TestPassengerProvider))
      a ! GetPassengerBroadcaster
      val broadcaster = expectMsgType[PassengerBroadcaster].broadcaster
      broadcaster ! "Broadcasted messages rock!"
      expectMsg("Broadcasted messages rock!")
      expectMsg("Broadcasted messages rock!")
      expectMsg("Broadcasted messages rock!")
      expectMsg("Broadcasted messages rock!")
      expectMsg("Broadcasted messages rock!")
      expectNoMsg(100 milliseconds)
      a ! GetPassengerBroadcaster
      expectMsg(PassengerBroadcaster(broadcaster))
    }
  }
}