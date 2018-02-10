package actor.local

object Actor1 extends App {
  val actor2 = new Actor2

  val actor1 = new Actor1(actor2)
}

import parascale.actor.last.{Actor, Task}
import parascale.actor.last.{Actor, Relay}

class Actor1(actor2: Actor) extends Actor {
  def act: Unit = {
    println("Actor1 id="+id+": sending message")
    actor2.send("testing 1-2-3", this)
    while(true) {
      receive match {
        case task: Task if task.kind == Task.REPLY =>
          println("Actor1: got reply task "+task)
        case msg =>
          println("Actor1: got task = "+msg)
      }
      return
    }
  }
}
