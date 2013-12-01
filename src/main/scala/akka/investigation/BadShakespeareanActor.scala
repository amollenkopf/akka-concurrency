package akka.investigation

import akka.actor.{ Actor, Props, ActorSystem }

class BadShakespeareanActor extends Actor {

  def receive = {
    case "Good Morning" => println("Him: Forsooth 'tis the 'morn, but morneth for thou doest I do!")
    case "You're terrible" => println("Him: yup")
  }
}

object BadShakespeareanActorMain {
  val system = ActorSystem("BadShakespearean")
  val actor = system.actorOf(Props[BadShakespeareanActor], "Shake")
  
  def send(message: String) {
    println(s"Me: $message")
    actor ! message
    Thread.sleep(100)
  }
  
  def main(args: Array[String]) {
    send("Good Morning")
    send("You're terrible")
    system.shutdown()
  }
}