package akka.investigation

import akka.actor.{ Actor, Props }
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._

object ActorBehaviorChange {
  case class Hello(greeting: String)
  case class Goodbye(message: String)
}

class ActorBehaviorChange extends Actor {
  import ActorBehaviorChange._

  def helloBehavior: Receive = {
    case Hello(greeting) =>
      sender ! Hello(greeting + " to you too!")
      context.become(goodbyeBehavior)
    case Goodbye(_) =>
      sender ! "Huh? Who are you?"
  }

  def goodbyeBehavior: Receive = {
    case Hello(_) =>
      sender ! "Hey, we've already done Hello."
    case Goodbye(_) =>
      sender ! Goodbye("So long, dude!")
      context.become(helloBehavior)
  }

  def receive = helloBehavior
}
