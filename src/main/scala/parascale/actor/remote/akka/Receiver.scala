package parascale.actor.remote.akka

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object Receiver {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Sys", ConfigFactory.load("remotelookup"))
    system.actorOf(Props[Receiver], "rcv")
  }
}

class Receiver extends Actor {
  import Sender._

  def receive = {
    case m: Echo  => sender() ! m
    case Shutdown => context.system.terminate()
    case _        =>
  }
}
