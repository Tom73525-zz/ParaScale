package actor.remote

import parascale.actor.last.{Dispatcher, Task}

object MyDispatcher1 extends App {
  val workers = List("localhost:8000", "localhost:9000")

  new MyDispatcher1(workers)
}

class MyDispatcher1(sockets: List[String]) extends Dispatcher(sockets) {
  println(sockets)
  def act: Unit = {
//    Thread.sleep(250)
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
