package scalaparallel.actor.remote.util

import java.io.{EOFException, FileInputStream, ObjectInputStream}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object Receiver {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Transceiver")
    val actor: ActorRef = system.actorOf(Props[TestActor],"TestActor")

    val receiver = new Receiver(actor,9000)

    receiver.receive
  }
}

class Receiver(actor: ActorRef, port: Int) {
  val fi = new FileInputStream("c:/tmp/trans.out")
  val ois = new ObjectInputStream(fi)

  def receive = {
    try {
      while (true) {
        val obj = ois.readObject()
        actor ! obj
      }
    }
    catch {
      case _: EOFException =>

    }
  }
}

class TestActor extends Actor {
  def receive = {
    case a: A =>
      println("got an A!")
  }
}
