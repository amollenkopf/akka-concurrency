package akka.patterns.behavioralcomposition
import akka.actor.{ Actor }

trait CompositeBehaviorActor extends Actor {
  import scala.collection.mutable.Map
  lazy val receivePartials = Map.empty[Int, Receive]
  val startOfReceiveChain = 0
  val endOfReceiveChain = 10000

  def becomeBehavior(key: Int, behavior: Receive) {
    receivePartials += (key -> behavior)
    context.become(composeReceive)
  }

  def composeReceive: Receive = {
    receivePartials.toSeq.sortBy {
      case (key, _) => key
    }.map {
      case (_, value) => value
    }.reduceLeft {
      (a, b) => a orElse b
    }
  }

  override def preStart() {
    super.preStart()
    context.become(composeReceive)
  }
  
  final def receive: Receive = Actor.emptyBehavior
}



