package parascale.actor.remote.ukko

import java.io.{EOFException, ObjectInputStream}
import java.net.ServerSocket

object Remote {
  def apply(actor: Actor, localPort: Int) = new Remote(actor,localPort)
}

/**
  * This actor runs on the client side and relays message to the local actor
  * @param actor Local actor
  * @param localPort Port to listen for inbound remote messages.
  */
class Remote(actor: Actor, localPort: Int) extends Thread {
  // Start the thread to receive inbound messages
  new Thread(this).start

  /** Relays inbound message to the (local) actor */
  override def run(): Unit = {
    println("Remote: receiver for actor started")
    val socket = new ServerSocket(localPort)

    try {
      while(true) {
        println("Remote: waiting to accept connection on port "+localPort)
        val clientSocket = socket.accept()

        // Deserialize the message
        println("Remote: got connection")
        val ois = new ObjectInputStream(clientSocket.getInputStream)

        val msg = ois.readObject

        // Relay message to the (local) actor
        actor.send(msg)

        ois.close
        clientSocket.close
      }
    }
    catch {
      case _: EOFException =>
    }
  }
}
