package akka.avionics

import akka.actor.{ Actor, ActorSystem, Props }
import akka.testkit.{ TestActorRef, TestKit, TestLatch }
import scala.concurrent.duration._
import scala.concurrent.Await
import org.scalatest.{ BeforeAndAfterAll, WordSpec }
import org.scalatest.matchers.Matchers

class AltimeterSpec extends TestKit(ActorSystem("AltimeterSpec")) with WordSpec with Matchers with BeforeAndAfterAll {
  import Altimeter._

  override def afterAll() { system.shutdown() }

  class Helper {
    object EventSourceSpy {
      val latch = TestLatch() //latch gives feedback when something happens
    }

    trait EventSourceSpy extends EventSource {
      def sendEvent[T](event: T): Unit = EventSourceSpy.latch.countDown
      def eventSourceReceive = Actor.emptyBehavior //don't care about actual messages
    }
    
    def slicedAltimeter = new Altimeter with EventSourceSpy
    def actor() = {
      val a = TestActorRef[Altimeter](Props(slicedAltimeter))
      (a, a.underlyingActor)
    }
  }

  "Altimeter" should {

    "record rate of climb changes" in new Helper {
      val (_, real) = actor()
      real.receive(RateChange(1f))
      assert(real.rateOfClimb == real.maxRateOfClimb, "After first rate change, maxRateOfClimb is the same as the rate change.")
    }

    "keep rate of climb changes within bounds" in new Helper {
      val (_, real) = actor()
      real.receive(RateChange(2f))
      println(real.rateOfClimb)
      assert(real.rateOfClimb == real.maxRateOfClimb, "A rate change of 2f is received and maxRate is adjusted")
    }
    
    "calculate altitude changes" in new Helper {
      val ref = system.actorOf(Props(Altimeter()))
      ref ! EventSource.RegisterListener(testActor)
      ref ! RateChange(1f)
      fishForMessage() {
        case AltitudeUpdate(altitude) if altitude == 0f => false
        case AltitudeUpdate(altitude) => true
      }
    }
    
    "send events" in new Helper {
      val (ref, _) = actor()
      Await.ready(EventSourceSpy.latch, 1.second)
      assert(EventSourceSpy.latch.isOpen, "Latch is open")
    }
  }
}