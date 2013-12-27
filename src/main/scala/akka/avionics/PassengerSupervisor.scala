package akka.avionics
import akka.actor.{ Actor, ActorRef, ActorLogging, ActorKilledException, ActorInitializationException, OneForOneStrategy, Props, SupervisorStrategy }
import akka.pattern.{ ask, pipe }
import akka.routing.BroadcastRouter
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps

object PassengerSupervisor {
  case object GetPassengerBroadcaster
  case class PassengerBroadcaster(broadcaster: ActorRef)
  def apply(callButton: ActorRef, bathrooms: ActorRef) = new PassengerSupervisor(callButton, bathrooms) with PassengerProvider
}

class PassengerSupervisor(callButton: ActorRef, bathrooms: ActorRef) extends Actor with ActorLogging { this: PassengerProvider =>
  import PassengerSupervisor._

  override val supervisorStrategy = OneForOneStrategy() {
    case _: ActorKilledException => SupervisorStrategy.Escalate
    case _: ActorInitializationException => SupervisorStrategy.Resume
    case _ => SupervisorStrategy.Resume
  }
  case class GetChildren(forSomeone: ActorRef)
  case class Children(children: Iterable[ActorRef], childrenFor: ActorRef)

  override def preStart() {
    context.actorOf(Props(new Actor {
      val config = context.system.settings.config
      override val supervisorStrategy = OneForOneStrategy() {
        case _: ActorKilledException => SupervisorStrategy.Escalate
        case _: ActorInitializationException => SupervisorStrategy.Escalate
        case _ => SupervisorStrategy.Stop
      }
      override def preStart() {
        import scala.collection.JavaConverters._
        import com.typesafe.config.ConfigList
        val passengers = config.getList("akka.avionics.passengers")
        passengers.asScala.foreach { nameWithSeat =>
          val id = nameWithSeat.asInstanceOf[ConfigList].unwrapped().asScala.mkString("-").replaceAllLiterally(" ", "_")
          context.actorOf(Props(newPassenger(callButton)), id)
        }
      }
      def receive = {
        case GetChildren(forSomeone: ActorRef) => //sender ! context.children.toSeq
          sender ! Children(context.children, forSomeone)
      }
    }), "PassengersSupervisor")
  }

  import context.dispatcher
  implicit val timeout = Timeout(5 seconds)
  def noRouter: Receive = {
    case GetPassengerBroadcaster =>
      val passengers = context.actorFor("PassengersSupervisor")
      passengers ! GetChildren(sender)
    case Children(passengers, destinedFor) =>
      val passengerPaths: Iterable[String] = passengers.map(passenger => passenger.path.toString)
      val bRouter = BroadcastRouter(routees = passengerPaths.toIndexedSeq)
      val router = context.actorOf(Props(PassengerSupervisor(callButton, bathrooms)).withRouter(bRouter), "Passengers")
      destinedFor ! PassengerBroadcaster(router)
      context.become(withRouter(router))
  }

  def withRouter(router: ActorRef): Receive = {
    case GetPassengerBroadcaster => sender ! PassengerBroadcaster(router)
    case Children(_, destinedFor) => destinedFor ! PassengerBroadcaster(router)
  }

  def receive = noRouter
}
