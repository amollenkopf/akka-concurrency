package akka.avionics

import akka.actor.{ Actor, ActorSystem, Props }
import akka.testkit.{ TestKit, TestActorRef }
import org.scalatest.{ BeforeAndAfterAll, WordSpec }
import org.scalatest.matchers.Matchers

class EventSourceTest extends Actor with EventSourceProducer {
  def receive = eventSourceReceive
}

class EventSourceSpec extends TestKit(ActorSystem("EventSourceSpec")) with WordSpec with Matchers with BeforeAndAfterAll {
  import EventSource._
  
  override def afterAll() { system.shutdown() }
  
  "EventSource" should {
    "allow us to register a listener" in {
      val real = TestActorRef[EventSourceTest].underlyingActor
      real.receive(RegisterListener(testActor))
      assert(real.listeners contains (testActor), "After registering listeners contains listener")
    }
    
    "allow us to unregister a listener" in {
      val real = TestActorRef[EventSourceTest].underlyingActor
      real.receive(RegisterListener(testActor))
      assert(real.listeners.size == 1, "After registering there is one listener")
      real.receive(UnregisterListener(testActor))
      assert(real.listeners.size == 0, "After unregistering there are zero listeners")
    }

    "send the event to our test actor" in {
      val testA = TestActorRef[EventSourceTest]
      testA ! RegisterListener(testActor)
      testA.underlyingActor.sendEvent("Fibonacci")
      expectMsg("Fibonacci")
    }
  }
}