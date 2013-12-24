package akka.avionics
import akka.actor.{ Actor, ActorRef, ActorLogging, ActorKilledException, ActorInitializationException, OneForOneStrategy, Props, SupervisorStrategy }
import akka.routing.BroadcastRouter

object PassengerSupervisor {
  case object GetPassengerBroadcaster
  case class PassengerBroadcaster(broadcaster: ActorRef)
  def apply(callButton: ActorRef) = new PassengerSupervisor(callButton) with PassengerProvider
}

class PassengerSupervisor(callButton: ActorRef) extends Actor with ActorLogging { this: PassengerProvider =>
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
        case GetChildren(forSomeone: ActorRef) => sender ! Children(context.children, forSomeone)
      }
    }), "PassengersSupervisor")
  }

  def noRouter: Receive = {
    case GetPassengerBroadcaster =>
      val passengers = context.actorFor("PassengersSupervisor")
      passengers ! GetChildren(sender)
    case Children(passengers, destinedFor) =>
      val passengerPaths: Iterable[String] = passengers.map(passenger => passenger.path.toString)
      val bRouter = BroadcastRouter(routees = passengerPaths.toIndexedSeq)
      val router = context.actorOf(Props(PassengerSupervisor(callButton)).withRouter(bRouter), "Passengers")
      destinedFor ! PassengerBroadcaster(router)
      context.become(withRouter(router))
  }

  def withRouter(router: ActorRef): Receive = {
    case GetPassengerBroadcaster => sender ! PassengerBroadcaster(router)
    case Children(_, destinedFor) => destinedFor ! PassengerBroadcaster(router)
  }

  def receive = noRouter
}
