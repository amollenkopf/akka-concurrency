package akka.avionics

import akka.actor.{ Actor, ActorRef, ActorLogging, FSM }

trait PilotProvider {
  def newPilot(plane: ActorRef, autopilot: ActorRef, controls: ActorRef, altimeter: ActorRef): Actor =
    new Pilot(plane, autopilot, controls, altimeter) with DrinkingProvider with FlyingProvider

  def newCopilot(plane: ActorRef, altimeter: ActorRef): Actor = new Copilot(plane, altimeter)
  def newAutopilot(): Actor = new Autopilot()
}

object Pilot {
  import FlyingBehavior._
  import ControlSurface._
  case object ReadyToGo
  case object RelinquishControl

  val calculateElevationTipsy: Calculator = { (target, status) =>
    val message = calculateElevation(target, status)
    message match {
      case StickForward(amount) => StickForward(amount * 1.03f)
      case StickBack(amount) => StickBack(amount * 1.03f)
      case m => m
    }
  }

  val calculateElevationZaphod: Calculator = { (target, status) =>
    val message = calculateElevation(target, status)
    message match {
      case StickForward(amount) => StickBack(1f)
      case StickBack(amount) => StickForward(1f)
      case m => m
    }
  }

  val calculateAileronsTipsy: Calculator = { (target, status) =>
    val message = calculateAilerons(target, status)
    message match {
      case StickLeft(amount) => StickLeft(amount * 1.03f)
      case StickRight(amount) => StickLeft(amount * 1.03f)
      case m => m
    }
  }

  val calculateAileronsZaphod: Calculator = { (target, status) =>
    val message = calculateAilerons(target, status)
    message match {
      case StickLeft(amount) => StickRight(1f)
      case StickRight(amount) => StickLeft(1f)
      case m => m
    }
  }
}

class Pilot(plane: ActorRef, autopilot: ActorRef, heading: ActorRef, altimeter: ActorRef)
  extends Actor with ActorLogging { this: DrinkingProvider with FlyingProvider =>
  import DrinkingBehavior._
  import FlyingBehavior._
  import FSM._
  import Pilot._
  import Plane._
  var copilotName = context.system.settings.config.getString("akka.avionics.flightcrew.copilotName")

  override def preStart() {
    context.actorOf(newDrinkingBehavior(self), "DrinkingBehavior")
    context.actorOf(newFlyingBehavior(plane, heading, altimeter), "FlyingBehavior")
  }

  def receive = bootstrap

  def bootstrap: Receive = {
    case ReadyToGo =>
      val copilot = context.actorFor("../" + copilotName)
      val flyer = context.actorFor("FlyingBehavior")
      flyer ! SubscribeTransitionCallBack(self)
      setCourse(flyer)
      context.become(sober(copilot, flyer))
  }

  def setCourse(flyer: ActorRef) { flyer ! Fly(CourseTarget(20000, 250, System.currentTimeMillis + 30000)) }

  def becomeSober(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevationCalculator(calculateElevation)
    flyer ! NewBankingCalculator(calculateAilerons)
    context.become(sober(copilot, flyer))
  }

  def sober(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober => //already sober
    case FeelingTipsy => becomeTipsy(copilot, flyer)
    case FeelingLikeZaphod => becomeZaphod(copilot, flyer)
  }

  def becomeTipsy(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevationCalculator(calculateElevationTipsy)
    flyer ! NewBankingCalculator(calculateAileronsTipsy)
    context.become(tipsy(copilot, flyer))
  }

  def tipsy(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober => becomeSober(copilot, flyer)
    case FeelingTipsy => //already tipsy
    case FeelingLikeZaphod => becomeZaphod(copilot, flyer)
  }

  def becomeZaphod(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevationCalculator(calculateElevationZaphod)
    flyer ! NewBankingCalculator(calculateAileronsZaphod)
    context.become(zaphod(copilot, flyer))
  }

  def zaphod(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober => becomeSober(copilot, flyer)
    case FeelingTipsy => becomeTipsy(copilot, flyer)
    case FeelingLikeZaphod => //already zaphod
  }

  override def unhandled(message: Any): Unit = {
    message match {
      case Transition(_, _, Flying) => setCourse(sender)
      case Transition(_, _, Idle) => context.become(idle)
      case Transition(_, _, _) => //goes to log
      case CurrentState(_, _) => //goes to log
      case m => super.unhandled(m)
    }
  }

  def idle: Receive = { case _ => }
}