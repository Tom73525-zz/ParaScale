package actor

import java.net.InetAddress

import org.apache.log4j.Logger
import parascale.actor.remote.last.{Actor, Relay, Task}

object Actor1 extends App {
  val LOG =  Logger.getLogger(getClass)

  val actor1 = new Actor1

  actor1.send("test message sent actor1")

  val destHost = InetAddress.getLocalHost.getHostAddress
  val destPort = 9000
  val socket = destHost + ":" + destPort

  val relay = new Relay(socket, actor1)

  relay.send(A("got it!"))
}

class Actor1 extends Actor  {
  import Actor1._

  override def run = {
    LOG.info("running")
    while(true) {
      receive match {
        case reply: Task if reply.kind == Task.REPLY =>
          LOG.info("got reply = "+reply)

        case msg =>
          LOG.info("got some message =  "+msg)
      }
    }
  }
}
