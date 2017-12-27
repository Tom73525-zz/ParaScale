package actor

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scalaparallel.actor.remote.ukko.{Packet, Remote}

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
    case pkt: Packet =>
      println("got a packet from "+pkt.host)
      pkt.payload match {
        case a: A =>
          println("got an A via the packet")
      }
  }
}
