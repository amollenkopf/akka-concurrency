package akka.avionics

import akka.actor.{ Actor, ActorRef }

object EventSource {
  case class RegisterListener(listener: ActorRef)
  case class UnregisterListener(listener: ActorRef)
}

trait EventSource { 
  def sendEvent[T](event: T): Unit
  def eventSourceReceive: Actor.Receive
}

trait EventSourceProducer extends EventSource { this: Actor => //self-typing
  import EventSource._

  var listeners = Vector.empty[ActorRef]

  def sendEvent[T](event: T): Unit = listeners foreach { _ ! event } //sends event to all listeners

  def eventSourceReceive: Receive = {
    case RegisterListener(listener) => listeners = listeners :+ listener
    case UnregisterListener(listener) => listeners = listeners filter { _ != listener }
  }
}