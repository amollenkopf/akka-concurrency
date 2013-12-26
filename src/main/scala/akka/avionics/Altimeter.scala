package akka.avionics

import akka.actor.{ Actor, ActorRef, ActorLogging, ActorSystem, Props, OneForOneStrategy, SupervisorStrategy }
import scala.concurrent.duration._
import scala.language.postfixOps

trait AltimeterProvider {
  def newAltimeter: Actor = Altimeter()
}

object Altimeter {
  case class RateChange(amount: Float)
  case class AltitudeUpdate(altitude: Double)
  case object GetCurrentAltitude
  case class CurrentAltitude(altitude: Double)
  def apply() = new Altimeter with EventSourceProducer with StatusReporter //mixes a trait in at construction time
}

class Altimeter extends Actor with ActorLogging with StatusReporter { this: EventSource => //self-typing
  import Altimeter._
  import AltitudeCalculator._
  import StatusReporter._

  override val supervisorStrategy = OneForOneStrategy(-1, Duration.Inf) {
    case _ => SupervisorStrategy.Restart
  }
  val maxRateOfClimb = 5000 //the maximum rate of climb for our plan in 'feet per minute'
  var rateOfClimb = 0.1f //the varying rate of climb depending on the movement of the stick
  var altitude = 16000d //our current altitude
  var lastTick = System.currentTimeMillis //represents how much time has passed, as time passes, we need to change the altitude
  implicit val ec = context.dispatcher
  var ticker = context.system.scheduler.schedule(100 millis, 100 millis, self, Tick) //schedules tick messages

  case object Tick //An internal message we send to ourselves to tell us to update our altitude
  def currentStatus = StatusOk

  var altitudeCalculator: ActorRef = context.system.deadLetters
  override def preStart() = {
    altitudeCalculator = context.actorOf(Props(new AltitudeCalculator), "AltitudeCalculator")
  }

  def altimeterReceive: Receive = {
    case RateChange(amount) => //Rate of climb has changed
      rateOfClimb = amount.min(1.0f).max(-1.0f) * maxRateOfClimb
      log info (s"Altimeter changed rate of climb to $rateOfClimb.")
    case Tick => //time has passed so calculate a new altitude
      val tick = System.currentTimeMillis
      altitudeCalculator ! CalculateAltitude(altitude, lastTick, tick, rateOfClimb)
      lastTick = tick
    case AltitudeCalculated(newTick, newAltitude) =>
      altitude = newAltitude
      sendEvent(AltitudeUpdate(altitude))
    case GetCurrentAltitude =>
      log.info("Altimeter - altitude request")
      sender ! CurrentAltitude(altitude)
  }

  def receive = { statusReceive orElse eventSourceReceive orElse altimeterReceive }

  override def postStop(): Unit = ticker.cancel
}
