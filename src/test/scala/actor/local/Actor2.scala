package actor.local

import parascale.actor.last.{Actor, Task}
import parascale.actor.remote.last.{Actor, RemoteRelay}

class Actor2 extends Actor {
  def act: Unit = {
    receive match {
      case task: Task =>
        println("Actor2: got task = "+task)
        sender ! "testing 3-4-5"
    }
  }
}
