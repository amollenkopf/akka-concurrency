package akka.avionics

import akka.actor.{ Actor, ActorRef, Props }

trait AttendantCreationPolicy {  //lead creates it own subordinates, policy for creation can vary
  val numberOfAttendants: Int = 10
  def createAttendant: Actor = FlightAttendant()
}

trait LeadFlightAttendantProvider {
  def newLeadFlightAttendant: Actor = LeadFlightAttendant()
}

object LeadFlightAttendant {
  case object GetFlightAttendant
  case class Attendant(a: ActorRef)
  def apply() = new LeadFlightAttendant with AttendantCreationPolicy
}

class LeadFlightAttendant extends Actor { this: AttendantCreationPolicy =>
  import LeadFlightAttendant._
  
  override def preStart() { //create all subordinates
    import scala.collection.JavaConverters._
    val attendantNames = context.system.settings.config.getStringList("akka.avionics.flightcrew.attendantNames").asScala
    attendantNames take numberOfAttendants foreach { name =>
      context.actorOf(Props(createAttendant), name)
    }
  }
  
  def randomAttendant(): ActorRef = {
    context.children.take(scala.util.Random.nextInt(numberOfAttendants)+1).last
  }

  def receive = {
    case GetFlightAttendant => sender ! Attendant(randomAttendant())
    //caes m => randomAttendant() forward m
  }
}

object FlightAttendantPathChecker {
  def main(args: Array[String]) {
    val system = akka.actor.ActorSystem("PlaneSimulation")
    val leadAttendantName = system.settings.config.getString("akka.avionics.flightcrew.leadAttendantName");
    val lead = system.actorOf(Props(new LeadFlightAttendant with AttendantCreationPolicy), leadAttendantName)
    Thread.sleep(2000)
    system shutdown
  }
}