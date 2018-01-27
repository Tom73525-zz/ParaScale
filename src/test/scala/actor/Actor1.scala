package actor

import java.net.InetAddress

import parascale.actor.remote.ukko.{Actor, Relay, Task}

object Actor1 extends App {

  val actor1 = new Actor1

  val destHost = InetAddress.getLocalHost.getHostAddress
  val destPort = 9000
  val relay = new Relay(destHost, destPort, actor1)

  relay.send(Payload("got it!"))
}

class Actor1 extends Actor  {
  override def run = {
    println("A1: started")
    while(true) {
      receive match {
        case reply: Task if reply.kind == Task.REPLY =>
          println("A1: got reply "+reply)
        case msg =>
          println("A1: to some other message: "+msg)
      }
    }
  }
}
