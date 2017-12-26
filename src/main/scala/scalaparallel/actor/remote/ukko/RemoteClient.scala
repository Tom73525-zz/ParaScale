package scalaparallel.actor.remote.ukko

import java.io.{EOFException, FileInputStream, ObjectInputStream}
import java.net.ServerSocket

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object RemoteClient {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Transceiver")
    val actor: ActorRef = system.actorOf(Props[TestActor],"TestActor")

    new RemoteClient(actor,9000)

    Thread.sleep(1000)

    actor ! "hello world"
  }
}

/**
  * This actor runs on the client side and relays message to the local actor
  * @param actor Local actor
  * @param port Port to listen for remote messages.
  */
class RemoteClient(actor: ActorRef, port: Int) extends Thread {
  // Start the thread to receive inbound messages
  new Thread(this).start

  /** Relays inbound message to the (local) actor */
  override def run(): Unit = {
    println("receiver for actor started")
    val socket = new ServerSocket(port)

    try {
      while (true) {
        println("waiting to accept connection")
        val clientSocket = socket.accept()

        // Deserialize the message
        println("got connection")
        val ois = new ObjectInputStream(clientSocket.getInputStream)
        val msg = ois.readObject()

        // Relay message to the (local) actor
        actor ! msg

        ois.close
        clientSocket.close
      }
    }
    catch {
      case _: EOFException =>

    }
  }
}

class TestActor extends Actor {

  override def receive = {
    case a: A =>
      println("got an A!")
    case s: String =>
      println(s)
    case pkt: Packet =>
      println("got a packet from "+pkt.host)
      pkt.payload match {
        case a: A =>
          println("got an A via the packet")
      }
  }
}
