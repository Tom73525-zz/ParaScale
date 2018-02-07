package actor.local

import parascale.actor.remote.last.{Actor, RemoteRelay, Task}

class Actor2 extends Actor {
  def act: Unit = {
    receive match {
      case task: Task =>
        println("Actor2: got task = "+task)
        sender ! "testing 3-4-5"
    }
  }
}
