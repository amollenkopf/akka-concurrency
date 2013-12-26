package akka.avionics
import akka.actor.{ ActorSystem, Actor, ActorRef, Props, IO, IOManager }
import akka.testkit.{ TestKit, ImplicitSender }
import akka.util.ByteString
import org.scalatest.{ WordSpec, BeforeAndAfterAll }
import org.scalatest.matchers.Matchers

class PlaneForTelnet extends Actor {
  import HeadingIndicator._
  import Altimeter._
  def receive = {
    case GetCurrentAltitude => sender ! CurrentAltitude(52500f)
    case GetCurrentHeading => sender ! CurrentHeading(233.4f)
  }
}

class TelnetSpec extends TestKit(ActorSystem("TelnetSpec"))
  with ImplicitSender with WordSpec with BeforeAndAfterAll with Matchers {
  import Telnet._

  override def afterAll() {
    system.shutdown()
  }

  "Telnet" should {
    "work" in {
      val plane = system.actorOf(Props[PlaneForTelnet])
      val telnet = system.actorOf(Props(new Telnet(plane)))
      val socket = IOManager(system).connect("localhost", 31733)
      expectMsgType[IO.Connected]
      expectMsgType[IO.Read]
      socket.write(ByteString("heading"))
      expectMsgPF() {
        case IO.Read(_, bytes) =>
          val result = Telnet.ascii(bytes)
          assert(result.contains("233.40 degrees"), "heading did not come back")
      }
      socket.write(ByteString("altitude"))
      expectMsgPF() {
        case IO.Read(_, bytes) =>
          val result = Telnet.ascii(bytes)
          assert(result.contains("52500.00 feet"), "altitude did not come back")
      }
      socket.close()
    }
  }
}
