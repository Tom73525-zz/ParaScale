package scalaparallel.actor.remote.util

import java.io.{EOFException, FileInputStream, ObjectInputStream}
import java.net.ServerSocket

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object Remote {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Transceiver")
    val actor: ActorRef = system.actorOf(Props[TestActor],"TestActor")

    new Remote(actor,9000)

    Thread.sleep(1000)

    actor ! "hello world"
  }
}

class Remote(actor: ActorRef, port: Int) extends Thread {


//  val fi = new FileInputStream("c:/tmp/trans.out")
//  val ois = new ObjectInputStream(fi)

  new Thread(this).start

  override def run(): Unit = {
    println("receiver for actor started")
    val socket = new ServerSocket(port)

    try {
      while (true) {
        println("waiting to accept connection")
        val clientSocket = socket.accept()

        println("got connection")
        val ois = new ObjectInputStream(clientSocket.getInputStream)
        val obj = ois.readObject()
        actor ! obj

        clientSocket.close
      }
    }
    catch {
      case _: EOFException =>

    }
  }
}

object RemoteActor2 {

}

class RemoteActor2 extends Actor {
  println("RemoteActor2 constructor invoked")
  val system = ActorSystem("Transceiver")
  val actor: ActorRef = system.actorOf(Props[RemoteActor2],"TestActor")
  val receiver = new Remote(actor,0)

  new Thread(receiver).start


  def receive = {
    case s: String =>
  }
}

class TestActor extends Actor {

  override def receive = {
    case a: A =>
      println("got an A!")
    case s: String =>
      println(s)
  }
}
