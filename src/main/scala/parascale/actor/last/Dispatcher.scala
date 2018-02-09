package parascale.actor.last

abstract class Dispatcher(workers: List[String]) extends Actor {
  val relays = for(worker <- workers) yield new RemoteRelay(worker, this)
  Thread.sleep(250)
}
