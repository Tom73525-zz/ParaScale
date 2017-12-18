// https://www.youtube.com/watch?v=v3qiXW3HyGI
import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object SimpleActorExample extends App {
  class SimpleActor extends Actor {
    def receive = {
      case s: String => println("String: "+s)
      case i: Int => println("Number: "+i)

    }

    def foo = println("normal method")
  }

  val system = ActorSystem("SimpleSystem")
  val actor: ActorRef = system.actorOf(Props[SimpleActor],"SimpleActor")

  actor ! "hello"
  actor ! 5
}


