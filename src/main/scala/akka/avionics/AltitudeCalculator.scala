package akka.avionics

import akka.actor.{ Actor, ActorLogging }

object AltitudeCalculator {
  case class CalculateAltitude(lastAltitude: Double, lastTick: Long, tick: Long, rateOfClimb: Double)
  case class AltitudeCalculated(newTick: Long, altitude: Double)
  def apply() = new AltitudeCalculator
}

class AltitudeCalculator extends Actor with ActorLogging {
  import AltitudeCalculator._
  
  def receive = {
    case CalculateAltitude(lastAltitude, lastTick, tick, rateOfClimb) =>
      if (rateOfClimb == 0)
        throw new ArithmeticException("Rate of climb is zero, causing a division by zero.")
      //risky behavior in altitude calculation has been contrived to allow division by zero
      val altitude = lastAltitude + ((tick - lastTick) / 60000.0) * (rateOfClimb*rateOfClimb) / rateOfClimb
      sender ! AltitudeCalculated(tick, altitude)
  }
}