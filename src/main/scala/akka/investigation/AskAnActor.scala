package akka.investigation

import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

class AskAnActor extends Actor {

  def receive = {
    case "Hello" => println("Hi")
    case 1000 => {
      println("waiting 1 second.")
      Thread.sleep(1000)
      println("1 second done.")
    }
    case 10000 => {
      println("waiting 10 seconds.")
      Thread.sleep(10000)
      println("10 seconds done.")
    }
    case _ => println("Huh?")
  }
}

object AskAnActorMain {
  val system = ActorSystem("MyActorSystem")
  val actor = system.actorOf(Props[AskAnActor], "mapm")

  def main(args: Array[String]) {
	implicit val askTimeout = Timeout(5.second)
    
    val question1 = actor ? 1000
    Thread.sleep(2000)
    println("question 1 complete? " + question1.isCompleted)
    
    val question2 = actor ? 10000
    Thread.sleep(15000)
    println("question 2 complete? " + question2.isCompleted)
    
    system.shutdown()
  }
}