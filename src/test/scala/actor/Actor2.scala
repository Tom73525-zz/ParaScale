package actor

import parascale.actor.remote.ukko.{Actor, Remote, Task}

object Actor2 extends App {
  val actor2 = new Actor2

  new Remote(actor2,9000)
}

class Actor2 extends Actor {
  override def run = {
    println("A2: started")
    while (true) {
      receive match {
        case task: Task =>
          println("A2: got a task "+task)
          task.payload match {
            case a: Payload =>
              println("A2: payload is A: " + a.s)

              task.reply("received your A")

          }
      }
    }
  }
}
