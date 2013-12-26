package akka.avionics
import akka.actor.Actor

trait StatusReporter { this: Actor =>
  import StatusReporter._
  def currentStatus: Status
  def statusReceive: Receive = {
    case ReportStatus => sender ! currentStatus
  }
}

object StatusReporter {
  case object ReportStatus
  sealed trait Status
  case object StatusOk extends Status
  case object StatusNotGreat extends Status
  case object StatusBad extends Status
}