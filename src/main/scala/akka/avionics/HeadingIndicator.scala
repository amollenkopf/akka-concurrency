package akka.avionics

import akka.actor.{ Actor, ActorLogging }
import scala.concurrent.duration._
import scala.language.postfixOps

trait HeadingIndicatorProvider {
  def newHeadingIndicator: Actor = HeadingIndicator()
}

object HeadingIndicator {
  case class BankChange(amount: Float) //how fast plane is changing direction
  case class HeadingUpdate(heading: Float) //event published to listeners that want to know where the plane is headed
  def apply() = new HeadingIndicator with EventSourceProducer
}

trait HeadingIndicator extends Actor with ActorLogging { this: EventSource =>
  import HeadingIndicator._
  import context._
  
  val maxDegreesPerSecond = 5
  val ticker = system.scheduler.schedule(100 millis, 100 millis, self, Tick)
  var lastTick: Long = System.currentTimeMillis()
  var rateOfBank = 0f //current rate of bank
  var heading = 0f //current direction
  case object Tick
  
  def headingIndicatorReceive : Receive = {
    case BankChange(amount) => rateOfBank = amount.min(1.0f).max(1.0f)
    case Tick =>
      val tick = System.currentTimeMillis()
      val timeDeltaSeconds = (tick - lastTick) / 1000f
      val degrees = rateOfBank * maxDegreesPerSecond
      heading = (heading + (360 + (timeDeltaSeconds * degrees))) % 360
      lastTick = tick
      sendEvent(HeadingUpdate(heading))
  }

  def receive = eventSourceReceive orElse headingIndicatorReceive
  
  override def postStop(): Unit = ticker.cancel()
}