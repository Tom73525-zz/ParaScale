package scalaparallel.actor.remote.util

import java.io.{FileOutputStream, ObjectOutputStream}
import java.net.Socket

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

class Courier(host: String, port: Int) extends Actor {

//  val fo = new FileOutputStream(host)
//  val oos = new ObjectOutputStream(fo)


  def receive = {
    case obj =>
      send(obj)
  }

  def send(obj: Any): Unit = {

    val socket = new Socket(host,port)

    val so = socket.getOutputStream
    val oos = new ObjectOutputStream(so)
    oos.writeObject(obj)

    oos.flush
    oos.close
    so.close
  }
}

class MyCourier extends Courier("127.0.0.1",9000) {
  override def receive = {
    case s: String =>
  }
}

object Courier {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Transceiver")
//    val courier: ActorRef = system.actorOf(Props(new Courier("127.0.0.1",9000)),name="TransSender")
    val courier: ActorRef = system.actorOf(Props[MyCourier],"TransSender")

    courier ! A("got it!")
  }
}


