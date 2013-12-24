package akka.avionics
import akka.actor.{ Props, ActorSystem, Actor, ActorRef }
import akka.testkit.{ TestKit, ImplicitSender, ExtractRoute }
import akka.routing.{ RouterConfig, Destination }
import org.scalatest.{ WordSpec, BeforeAndAfterAll }
import org.scalatest.matchers.Matchers

class TestRoutee extends Actor {
  def receive = Actor.emptyBehavior
}

class TestPassenger extends Actor {
  def receive = Actor.emptyBehavior
}

class FlightAttendantSectionRouterSpec extends TestKit(ActorSystem("FlightAttendantSectionRouterSpec"))
  with ImplicitSender with WordSpec with BeforeAndAfterAll with Matchers {

  override def afterAll() {
    system.shutdown()
  }
  
  def newRouter(): RouterConfig = {
    new FlightAttendantSectionRouter with FlightAttendantProvider {
      override def newFlightAttendant() = new TestRoutee
    }
  }
  
  def passengerWithRow(row: Int) = system.actorOf(Props[TestPassenger], s"Passenger-$row-C")
  val passengers = (1 to 25).map(passengerWithRow)

  "FlightAttendantSectionRoute" should {
    "route consistently" in {
      val router = system.actorOf(Props[TestRoutee].withRouter(newRouter()))
      val route = ExtractRoute(router)

      val routeA: Iterable[Destination] = passengers.slice(0, 10).flatMap { p => route(p, "Hi") }
      val routeASame = routeA.tail.forall { destination => destination.recipient == routeA.head.recipient }
      assert(routeASame, "Route A should be in the same section.")
      
      val routeAB: Iterable[Destination] = passengers.slice(9, 11).flatMap { p => route(p, "Hi") }
      assert(routeAB.head != routeAB.tail.head, "Route AB shoudl be in different sections.")
      
      val routeB: Iterable[Destination] = passengers.slice(10, 20).flatMap { p => route(p, "Hi") }
      val routeBSame = routeB.tail.forall { destination => destination.recipient == routeB.head.recipient }
      assert(routeBSame, "Route B should be in the same section.")
    }
  }
}

