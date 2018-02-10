package parascale.actor.last

import java.net.ServerSocket

/**
  * Creates a remote object
  */
object Remote {
  def apply(localPort: Int, actor: Actor) = new Remote(localPort, actor)
}

/**
  * This actor runs on the client side and relays message to the local actor
  * @param callforward Local actor
  * @param port Port to listen for inbound remote messages.
  */
class Remote(port: Int, callforward: Actor) extends Runnable {
  import org.apache.log4j.Logger
  val LOG =  Logger.getLogger(getClass)

  // Start the thread to receive inbound messages
  val me = new Thread(this)
  me.start

  /** Relays inbound message to the (local) actor */
  def run = {
    val id = Thread.currentThread.getId
    LOG.info("remote daemon id = " + id + " for call-forward actor " + callforward + " port = " + port)
    val socket = new ServerSocket(port)

    while (true) {
      println("Remote: waiting to accept connection on port " + port)
      val clientSocket = socket.accept()

      println("Remote: got connection from " + clientSocket.getInetAddress.getHostAddress)

      new Thread(new Ice(clientSocket, callforward)).start
    }
  }
}
