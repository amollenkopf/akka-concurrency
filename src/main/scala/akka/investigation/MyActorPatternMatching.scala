package akka.investigation

import akka.actor.{Actor, ActorSystem, Props}

case class Gamma(g: String)
case class Beta(b: String, g: Gamma)
case class Alpha(b1: Beta, b2: Beta)

class MyActorPatternMatching extends Actor {

  def receive = {
    case "Hello" => println("Hi")
    case 42 => println("I don't know the question.  Go ask the Earth Mark II.")
    case s: String => println(s"You sent me a string: $s")
    case Alpha(Beta(b1, Gamma(g1)), Beta(b2, Gamma(g2))) => println(s"Alpha(Beta($b1, Gamma($g1)), Beta($b2, Gamma($g2)))")
    case _ => println("Huh?")
  }
}

object MyActorPatternMatchingMain {
  val system = ActorSystem("MyActorSystem")
  val actor = system.actorOf(Props[MyActorPatternMatching], "mapm")
  
  def main(args: Array[String]) {
    actor ! "Hello"
    Thread.sleep(100)

    actor ! 42
    Thread.sleep(100)

    actor ! "Yo, Gabba Gabba"
    Thread.sleep(100)

    actor ! 777
    Thread.sleep(100)

    actor ! Alpha(Beta("b1Str", Gamma("g1Str")), Beta("b2Str", Gamma("g2Str")))
    Thread.sleep(100)
    
    system.shutdown()
  }
}