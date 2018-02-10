package parascale.actor.last

abstract class Dispatcher(sockets: List[String]) extends Actor {
  val workers = for(socket <- sockets) yield new Relay(socket, this)
  Thread.sleep(250)
}
