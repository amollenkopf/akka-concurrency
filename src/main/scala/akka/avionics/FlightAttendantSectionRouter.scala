package akka.avionics
import akka.actor.{ Props, SupervisorStrategy }
import akka.routing.{ RouterConfig, RouteeProvider, Route, Destination }
import akka.dispatch.Dispatchers

class FlightAttendantSectionRouter extends RouterConfig { this: FlightAttendantProvider =>
  def routerDispatcher: String = Dispatchers.DefaultDispatcherId
  def supervisorStrategy: SupervisorStrategy = SupervisorStrategy.defaultStrategy
  
  def createRoute(routeeProvider: RouteeProvider): Route = {
    val attendants = (1 to 5) map { n =>
      routeeProvider.context.actorOf(Props(newFlightAttendant), "Attendant-" + n)
    }
    routeeProvider.registerRoutees(attendants)
    
    {
      case (sender, message) =>
        import Passenger.SeatAssignment
        val SeatAssignment(_, row, _) = sender.path.name
        List(Destination(sender, attendants(math.floor(row.toInt / 11).toInt)))
    }
  }
}