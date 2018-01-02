package actor

import java.net.InetAddress

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import parascale.actor.remote.ukko.{Relay}

object TestActor1 {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("ParaScale")

    val me = system.actorOf(Props[TestActor2],"Test actor 1")

    val destHost = InetAddress.getLocalHost.getHostAddress
    val destPort = 9000
    val relay: ActorRef = system.actorOf(Props(new Relay(destHost,destPort,callback=me)),name="Relay")

    relay ! A("got it!")
  }
}

class TestActor1 extends Actor {
  def receive = {
    case s: String =>
      println("got message '"+s+"'")

    case obj =>
      println("test actor 1 got "+obj)
  }
}
