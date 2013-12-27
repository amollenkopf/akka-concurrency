package akka.patterns.behavioralcomposition

trait HelloBehavior { this: CompositeBehaviorActor =>
  val helloIx = 10
  def hello: Receive = {
    case "Hello" => sender ! ":-| Hi there."
  }
  receivePartials += (helloIx -> hello)
}

trait SmalltalkBehavior{ this: CompositeBehaviorActor =>
  val weatherIx = 15
  val kidsIx = 16
  val workIx = 17
  def weather: Receive = {
    case "How is the Weather?" => sender ! ":-( Rainy and lousy."
  }
  def kids: Receive = {
    case "How are the kids?" => sender ! ":-( They are doing alright."
  }
  def work: Receive = {
    case "How is work?" => sender ! ":-( I am so overloaded."
  }
  receivePartials += (weatherIx -> weather)
  receivePartials += (kidsIx -> kids)
  receivePartials += (workIx -> work)  
}

trait GoodbyeBehavior{ this: CompositeBehaviorActor =>
  val goodbyeIx = 20
  def goodbye: Receive = {
    case "Goodbye" => sender ! ";-} So long."
  }
  receivePartials += (goodbyeIx -> goodbye)
}

class TalkingActor extends CompositeBehaviorActor
  with HelloBehavior with SmalltalkBehavior with GoodbyeBehavior {
  val moodIx = 50
  def happyWeather: Receive = {
    case "How is the Weather?" => sender ! ":-) Sunny and Great!"
  }
  def happyKids: Receive = {
    case "How are the kids?" => sender ! ":-) They are Funny, Smart, and Awesome!"
  }
  def happyWork: Receive = {
    case "How is work?" => sender ! ":-) It is Exciting, Motivating, and Fun!"
  }
  def mood: Receive = {
    case "Happy" =>
      becomeBehavior(weatherIx, happyWeather)
      becomeBehavior(kidsIx, happyKids)
      becomeBehavior(workIx, happyWork)
    case "Grumpy" =>
      becomeBehavior(weatherIx, weather)
      becomeBehavior(kidsIx, kids)
      becomeBehavior(workIx, work)
  }
  receivePartials += (moodIx -> mood)
}