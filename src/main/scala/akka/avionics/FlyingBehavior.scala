package akka.avionics

import akka.actor.{ Actor, ActorRef, FSM }
import scala.concurrent.duration._
import scala.language.postfixOps

trait FlyingProvider {
  import akka.actor.Props
  def newFlyingBehavior(plane: ActorRef, heading: ActorRef, altimeter: ActorRef): Props =
    Props(new FlyingBehavior(plane, heading, altimeter))
}

object FlyingBehavior {
  import ControlSurface._
  sealed trait State
  case object Idle extends State
  case object Flying extends State
  case object PreparingToFly extends State

  case class CourseTarget(altitude: Double, heading: Float, byMillis: Long)
  case class CourseStatus(altitude: Double, heading: Float, headingSinceMillis: Long, altitudeSinceMillis: Long)
  case class NewElevationCalculator(c: Calculator)
  case class NewBankingCalculator(c: Calculator)

  type Calculator = (CourseTarget, CourseStatus) => Any

  sealed trait Data
  case object Uninitialized extends Data

  case class FlightData(controls: ActorRef, elevationCalculator: Calculator,
    bankingCalculator: Calculator, target: CourseTarget, status: CourseStatus) extends Data

  case class Fly(target: CourseTarget)

  def currentMillis = System.currentTimeMillis();

  def calculateElevation(target: CourseTarget, status: CourseStatus): Any = {
    val altitude = (target.altitude - status.altitude).toFloat
    val duration = target.byMillis - status.altitudeSinceMillis
    if (altitude < 0) StickForward((altitude / duration) * -1) else StickBack(altitude / duration)
  }

  def calculateAilerons(target: CourseTarget, status: CourseStatus): Any = {
    import scala.math.{ abs, signum }
    val difference = target.heading - status.heading
    val duration = target.byMillis - status.headingSinceMillis
    val amount = if (abs(difference) < 180) difference else signum(difference) * (abs(difference) - 360f)
    if (amount > 0) StickRight(amount / duration) else StickLeft((amount / duration) * -1)
  }
}

class FlyingBehavior(plane: ActorRef, altimeter: ActorRef, headingIndicator: ActorRef) extends Actor
  with FSM[FlyingBehavior.State, FlyingBehavior.Data] {
  import FSM._
  import FlyingBehavior._
  import Pilot._
  import Plane._
  import Altimeter._
  import HeadingIndicator._
  import EventSource._
  case object Adjust

  startWith(Idle, Uninitialized)

  def adjust(flightData: FlightData): FlightData = {
    val FlightData(controls, ec, bc, target, status) = flightData
    controls ! calculateElevation(target, status)
    controls ! calculateAilerons(target, status)
    flightData
  }

  when(Idle) { //when Idle, recognize 'Fly' messages and transition to 'PreparingToFly' using 'FlightData'
    case Event(Fly(target), _) =>
      goto(PreparingToFly) using FlightData(context.system.deadLetters, calculateElevation, calculateAilerons, target, CourseStatus(-1, -1, 0, 0))
  }

  onTransition {
    case Idle -> PreparingToFly => //-> is a state transition operator
      plane ! GiveMeControl
      altimeter ! RegisterListener(self)
      headingIndicator ! RegisterListener(self)
  }

  def prepComplete(data: Data): Boolean = {
    data match {
      case FlightData(c, _, _, _, s) =>
        if (!c.isTerminated && s.heading != -1f && s.altitude != -1f) true else false
      case _ => false
    }
  }

  when(PreparingToFly, stateTimeout = 5 seconds)(transform {
    case Event(AltitudeUpdate(a), d: FlightData) =>
      stay using d.copy(status =
        d.status.copy(altitude = a, altitudeSinceMillis = currentMillis))
    case Event(HeadingUpdate(h), d: FlightData) =>
      stay using d.copy(status =
        d.status.copy(heading = h, headingSinceMillis = currentMillis))
    case Event(Controls(c), d: FlightData) =>
      stay using d.copy(controls = c)
    case Event(StateTimeout, _) =>
      plane ! LostControl
      goto(Idle)
  } using {
    case s if prepComplete(s.stateData) =>
      s.copy(stateName = Flying)
  })

  onTransition {
    case PreparingToFly -> Flying =>
      setTimer("Adjustment", Adjust, 200 milliseconds, repeat = true)
  }

  when(Flying) {
    case Event(AltitudeUpdate(a), d: FlightData) =>
      stay using d.copy(status =
        d.status.copy(altitude = a, altitudeSinceMillis = currentMillis))
    case Event(HeadingUpdate(h), d: FlightData) =>
      stay using d.copy(status =
        d.status.copy(heading = h, headingSinceMillis = currentMillis))
    case Event(Adjust, flightData: FlightData) =>
      stay using adjust(flightData)
    case Event(NewBankingCalculator(c), d: FlightData) =>
      stay using d.copy(bankingCalculator = c)
    case Event(NewElevationCalculator(c), d: FlightData) =>
      stay using d.copy(elevationCalculator = c)
  }

  onTransition {
    case Flying -> _ =>
      cancelTimer("Adjustment")
  }

  onTransition {
    case _ -> Idle =>
      altimeter ! UnregisterListener(self)
      headingIndicator ! UnregisterListener(self)
  }

  whenUnhandled {
    case Event(RelinquishControl, _) =>
      goto(Idle)
  }

  initialize
}