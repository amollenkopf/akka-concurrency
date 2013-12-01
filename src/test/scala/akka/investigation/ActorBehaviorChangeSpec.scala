package akka.investigation

import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ TestKit, TestActorRef, ImplicitSender }
import org.scalatest.WordSpec
import org.scalatest.matchers.Matchers
import scala.concurrent.duration._

object ActorBehaviorChangeTest {
  def apply() = new ActorBehaviorChange
}

class ActorBehaviorChangeSpec extends TestKit(ActorSystem("ActorBehaviorChangeSpec")) with ImplicitSender with WordSpec with Matchers {
  import ActorBehaviorChange._

  "Actor" should {
    "say hello as initial behavior" in {
      val actor = TestActorRef(Props(ActorBehaviorChangeTest()))
      actor ! Hello("Hello")
      expectMsg(2.seconds, Hello("Hello to you too!"))
    }
  }

  "Actor" should {
    "Not say goodbye as initial behavior" in {
      val actor = TestActorRef(Props(ActorBehaviorChangeTest()))
      actor ! Goodbye("Goodbye")
      expectMsg(2.seconds, "Huh? Who are you?")
    }
  }

  "Actor" should {
    "say hello as initial behavior then goodbye as new behavior" in {
      val actor = TestActorRef(Props(ActorBehaviorChangeTest()))
      actor ! Hello("Hello")
      expectMsg(2.seconds, Hello("Hello to you too!"))
      actor ! Goodbye("Goodbye")
      expectMsg(2.seconds, Goodbye("So long, dude!"))
    }
  }

  "Actor" should {
    "say hello as initial behavior and not say hello again" in {
      val actor = TestActorRef(Props(ActorBehaviorChangeTest()))
      actor ! Hello("Hello")
      expectMsg(2.seconds, Hello("Hello to you too!"))
      actor ! Hello("Hello")
      expectMsg(2.seconds, "Hey, we've already done Hello.")
    }
  }
}