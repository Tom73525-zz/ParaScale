package parascale.actor.remote.ukko

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.{InetAddress, ServerSocket, Socket}

object Relay {
  def apply(destHost: String, destPort: Int, callback: Actor) = new Relay(destHost,destPort,callback)
}

/**
  * This class is an actor which relays message from the local (or srouce host) to a remote (or destination) host.
  * @param destHost Destination host
  * @param destPort Port on destination host
  * @param callback Actor to receive replies.
  */
class Relay(destHost: String, destPort: Int, callback: Actor) extends Actor {
  import org.apache.log4j.Logger
  val LOG =  Logger.getLogger(getClass)

  // For sending reply messages
  val replyHost =  InetAddress.getLocalHost.getHostAddress
  LOG.info("Relay: listening for replies as host "+replyHost)

  val replyPort = destPort + Thread.activeCount
  LOG.info("Relay: listening for replies on port "+replyPort)

//  // Start the listener thread to receive replies
//  new Thread(new ReplyHandler(replyHost,replyPort,callback)).start
//  new Thread(this).start

  /** Runs the worker thread to receive replies. */
  override def run(): Unit = {
    Thread.sleep(250)

    LOG.info("run: thread started to receive replies")
    val socket = new ServerSocket(replyPort)

    while(true) {
      LOG.info("run: waiting to accept connection")
      val clientSocket = socket.accept()

      LOG.info("run: connection accepted")
      val ois = new ObjectInputStream(clientSocket.getInputStream)

      val reply = ois.readObject

      LOG.info("run: received message as reply: "+reply)
      LOG.info("run: callback actor is "+callback)
      LOG.info("run: i am "+this)

      callback.send(reply)
      LOG.info("run: successfully relayed reply "+reply)

      ois.close
      clientSocket.close
    }
  }


  /**
    * Sends a message using sockets.
    * @param msg A message
    */
  override def send(msg: Any): Unit = {
    LOG.info("send: relaying msg " +msg)
    val task = msg match {
      case t: Task =>
        t
      case _ =>
         Task(replyHost, replyPort, msg)
    }

    val socket = new Socket(destHost, destPort)

    val os = socket.getOutputStream
    val oos = new ObjectOutputStream(os)

    oos.writeObject(task)
    LOG.info("send: sent msg as = "+task+" to "+destHost+":"+destPort)

    oos.flush
    oos.close

    os.close
  }
}

