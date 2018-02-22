package actor.remote

import parascale.actor.last.{Dispatcher, Task}

object MyDispatcher extends App {
  val workers = List("localhost:8000", "localhost:9000")

  new MyDispatcher(workers)
}

class MyDispatcher(sockets: List[String]) extends Dispatcher(sockets) {
  println(sockets)
  def act: Unit = {

    println("worker len = "+sockets.length)
    (0 until sockets.length).foreach { k =>
      println("sending message to worker " + k)
        workers(k) ! "to worker(" + k + ") hello from dispatcher"
    }

    while (true)
      receive match {
        case task: Task if task.kind == Task.REPLY =>
          println("received reply " + task)
      }
  }
}
