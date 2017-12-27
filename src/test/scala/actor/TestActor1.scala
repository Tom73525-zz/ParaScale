package actor

import java.net.InetAddress

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scalaparallel.actor.remote.ukko.{Relay}

object TestActor1 {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Transceiver")

    val testActor2 = system.actorOf(Props[TestActor2],"Test actor 2")

    val remoteHost = InetAddress.getLocalHost.getHostAddress
    val remotePort = 9000
    val relay: ActorRef = system.actorOf(Props(new Relay(remoteHost,remotePort,testActor2)),name="Relay")
    //    val relay: ActorRef = system.actorOf(Props[CourierActor],"TransSender")

    relay ! A("got it!")
  }
}

class TestActor1 extends Actor {
  def receive = {
    case obj =>
      println("test actor 2 got "+obj)
  }
}
