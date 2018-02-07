package actor.remote

import java.net.InetAddress

import org.apache.log4j.Logger
import parascale.actor.last.{Actor, RemoteRelay, Task}
import parascale.actor.remote.last.{Actor, RemoteRelay}

object Actor1 extends App {
  val LOG =  Logger.getLogger(getClass)

  val actor1 = new Actor1

  actor1 ! "test message sent to self"

  val destAddr = InetAddress.getLocalHost.getHostAddress
  val destPort = 9000
  val socket = destAddr + ":" + destPort

  val relay = new RemoteRelay(socket, actor1)

  relay ! A("got it!")
}

class Actor1 extends Actor  {
  import Actor1._

  def act = {
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
