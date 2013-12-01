package akka.avionics

import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global //futures created by ask need an execution context to run

object Avionics {
  implicit val timeout = Timeout(5.seconds)
  val system = ActorSystem("PlaneSimulation")
  val plane = system.actorOf(Props(Plane()), "Plane")

  def main(args: Array[String]) {
    val controls = Await.result((plane ? Plane.GiveMeControl).mapTo[ActorRef], 5.seconds) //get the controls
    system.scheduler.scheduleOnce(200.millis) { controls ! ControlSurface.StickBack(1f) } //take off
    system.scheduler.scheduleOnce(1.seconds) { controls ! ControlSurface.StickBack(0.1f) } //leveling out slightly climbing
    system.scheduler.scheduleOnce(3.seconds) { controls ! ControlSurface.StickBack(0.5f) } //climb
    //system.scheduler.scheduleOnce(3.seconds + 800.millis) { controls ! ControlSurface.StickBack(0.0f) } //leveled out
    system.scheduler.scheduleOnce(4.seconds) { controls ! ControlSurface.StickBack(-0.1f) } //leveling out slightly descending
    system.scheduler.scheduleOnce(5.seconds) { system.shutdown() } //shut down
  }
}