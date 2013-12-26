package akka.avionics
import akka.actor.{ Actor, ActorRef, IO, IOManager, ActorLogging, Props }
import akka.util.ByteString
import akka.pattern.ask
import akka.util.Timeout
import scala.collection.mutable.Map
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{ Success, Failure }

object Telnet {
  import Altimeter.{ GetCurrentAltitude, CurrentAltitude }
  import HeadingIndicator.{ GetCurrentHeading, CurrentHeading }
  implicit val askTimeout = Timeout(1 second)
  val welcome = """| Welcome to the Airplane!
                   --------------------------
                   |
                   | valid commands are: 'heading' and 'altitude'
                   |
                   |>""".stripMargin
  def ascii(bytes: ByteString): String = bytes.decodeString("UTF-8").trim()
  case class NewMessage(msg: String)
  class SubServer(socket: IO.SocketHandle, plane: ActorRef) extends Actor {
    import HeadingIndicator._
    import Altimeter._
    def headingString(heading: Float): ByteString = ByteString(f"current heading is: $heading%3.2f degrees\n\n> ")
    def altitudeString(altitude: Double): ByteString = ByteString(f"current altitude is: $altitude%4.2f feet\n\n> ")
    def unknown(string: String): ByteString = ByteString(f"current $string is unknown\n\n> ")
    def handleHeading() = {
      (plane ? GetCurrentHeading).mapTo[CurrentHeading].onComplete {
        case Success(CurrentHeading(heading)) => socket.write(headingString(heading))
        case Failure(_) => socket.write(unknown("heading"))
      }
    }
    def handleAltitude() = {
      (plane ? GetCurrentAltitude).mapTo[CurrentAltitude].onComplete {
        case Success(CurrentAltitude(altitude)) => socket.write(altitudeString(altitude))
        case Failure(_) => socket.write(unknown("altitude"))
      }
    }
    def receive = {
      case NewMessage(msg) => msg match {
        case "heading" => handleHeading()
        case "altitude" => handleAltitude()
        case m => socket.write(ByteString("What?\n\n"))
      }
    }
  }
}

class Telnet(plane: ActorRef) extends Actor with ActorLogging {
  import Telnet._
  val subservers = Map.empty[IO.Handle, ActorRef]
  val serverSocket = IOManager(context.system).listen("0.0.0.0", 31733)
  def receive = {
    case IO.Listening(server, address) =>
      log.info("Telnet Server listening on port {}", address)
    case IO.NewClient(server) =>
      log.info("New incoming client connection on server")
      val socket = server.accept()
      socket.write(ByteString(welcome))
      subservers += (socket -> context.actorOf(Props(new SubServer(socket, plane))))
    case IO.Read(socket, bytes) =>
      val cmd = ascii(bytes)
      subservers(socket) ! NewMessage(cmd)
    case IO.Closed(socket, cause) =>
      context.stop(subservers(socket))
      subservers -= socket
  }
}