package akka.patterns.behavioralcomposition
import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ TestKit, TestActorRef, ImplicitSender }
import org.scalatest.WordSpec
import org.scalatest.matchers.Matchers
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import akka.patterns.behavioralcomposition._

class TalkingActorSpec extends TestKit(ActorSystem("TalkingActorSpec"))
  with ImplicitSender with WordSpec with Matchers {

  "TalkingActor" should {
    "say hello and goodbye" in {
      val actor = TestActorRef(Props(new TalkingActor()))
      actor ! "Hello"
      expectMsg(1 second, ":-| Hi there.")
      actor ! "Goodbye"
      expectMsg(1 second, ";-} So long.")
    }
  }

  "TalkingActor" should {
    "behave grumpy" in {
      val actor = TestActorRef(Props(new TalkingActor()))
      actor ! "How is the Weather?"
      expectMsg(1 second, ":-( Rainy and lousy.")
      actor ! "How are the kids?"
      expectMsg(1 second, ":-( They are doing alright.")
      actor ! "How is work?"
      expectMsg(1 second, ":-( I am so overloaded.")
    }
  }

  "TalkingActor" should {
    "become happy and behave happy" in {
      val actor = TestActorRef(Props(new TalkingActor()))
      actor ! "Happy"
      actor ! "How is the Weather?"
      expectMsg(1 second, ":-) Sunny and Great!")
      actor ! "How are the kids?"
      expectMsg(1 second, ":-) They are Funny, Smart, and Awesome!")
      actor ! "How is work?"
      expectMsg(1 second, ":-) It is Exciting, Motivating, and Fun!")
    }
  }

  "TalkingActor" should {
    "behave grumpy, become happy, and become grumpy" in {
      val actor = TestActorRef(Props(new TalkingActor()))
      actor ! "How is the Weather?"
      expectMsg(1 second, ":-( Rainy and lousy.")

      actor ! "Happy"
      actor ! "How is the Weather?"
      expectMsg(1 second, ":-) Sunny and Great!")
      actor ! "How are the kids?"
      expectMsg(1 second, ":-) They are Funny, Smart, and Awesome!")

      actor ! "Grumpy"
      actor ! "How is the Weather?"
      expectMsg(1 second, ":-( Rainy and lousy.")
      actor ! "How are the kids?"
      expectMsg(1 second, ":-( They are doing alright.")
    }
  }
}