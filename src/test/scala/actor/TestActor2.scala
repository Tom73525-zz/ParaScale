package actor

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import parascale.actor.remote.ukko.{Task, Remote}

object TestActor2 {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Transceiver")
    val actor: ActorRef = system.actorOf(Props[TestActor2],"TestActor")

    new Remote(actor,10000)

    Thread.sleep(1000)

    actor ! "hello world"
  }
}

class TestActor2 extends Actor {
  override def receive = {
    case a: A =>
      println("got an A!")
    case s: String =>
      println(s)
    case task: Task =>
      println("got a task replies to "+task.srcHost+":"+task.srcPort)
      task.payload match {
        case a: A =>
          println("got an A task")
          task.reply("received A")
      }
  }
}
