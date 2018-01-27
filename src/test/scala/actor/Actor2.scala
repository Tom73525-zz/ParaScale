package actor

import org.apache.log4j.Logger
import parascale.actor.remote.last.{Actor, Remote, Task}

object Actor2 extends App {
  val LOG =  Logger.getLogger(getClass)
  val actor2 = new Actor2

  Thread.sleep(250)
  actor2.send("testing 1-2-3")

  new Remote(actor2,9000)
}

class Actor2 extends Actor {
  override def run = {
    import Actor2._
    LOG.info("running")
    while (true) {
      receive match {
        case task: Task =>
          LOG.info("got task = "+task)

          task.payload match {
            case a: A =>
              LOG.info("payload is A = " + a.s)
              task.reply("received your A")

            case s: String =>
              LOG.info("got "+s)
          }

      }
    }
  }
}
