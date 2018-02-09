package actor.remote

import parascale.actor.last.Dispatcher

object MyDispatcher1 extends App {
  val workers = List("localhost:8000", "localhost:9000")

  new MyDispatcher1(workers)
}

class MyDispatcher1(workers: List[String]) extends Dispatcher(workers) {
  println(relays)
  println(workers)
  def act: Unit = {
//    Thread.sleep(250)
    (0 until workers.length).foreach { k =>
      relays(k) ! "to worker(" + k + ")hello from Fred"
    }
  }
}
