package scalaparallel.actor.remote.util

import java.io.{FileOutputStream, ObjectOutputStream}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

class Transmitter(host: String, port: Int) extends Actor {

  val fo = new FileOutputStream(host);
  val oos = new ObjectOutputStream(fo);


  def receive = {
    case obj =>
      send(obj)
  }

  def send(obj: Any): Unit = {

    oos.writeObject(obj)

    oos.flush
  }
}

object Transmitter {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Transceiver")
    val actor: ActorRef = system.actorOf(Props(new Transmitter("c:/tmp/trans.out",0)),name="TransSender")

    actor ! A("got it!")
  }
}

case class A(s: String) extends java.io.Serializable

