/*
 Copyright (c) Ron Coleman

 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files (the
 "Software"), to deal in the Software without restriction, including
 without limitation the rights to use, copy, modify, merge, publish,
 distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to
 the following conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package parascale.actor.remote.ukko0

import java.io.{FileOutputStream, ObjectInputStream, ObjectOutputStream}
import java.net.{InetAddress, ServerSocket, Socket}

import akka.actor.{Actor, ActorRef}

object Relay {
  def apply(destHost: String, destPort: Int, callback: ActorRef) = new Relay(destHost,destPort,callback)
}

/**
  * This class is an actor which relays message from the local (or srouce host) to a remote (or destination) host.
  * @param destHost Destination host
  * @param destPort Port on destination host
  * @param callback Actor to receive replies.
  */
class Relay(destHost: String, destPort: Int, callback: ActorRef) extends Actor {
  import org.apache.log4j.Logger
  val LOG =  Logger.getLogger(getClass)

  // For sending reply messages
  val replyHost =  InetAddress.getLocalHost.getHostAddress
  LOG.info("Relay: listening for replies as host "+replyHost)

  val replyPort = destPort + Thread.activeCount
  LOG.info("Relay: listening for replies on port "+replyPort)

  // Start the listener thread to receive replies
  new Thread(new ReplyHandler(replyHost,replyPort,callback)).start

  /** Receives actor messages to be relayed outbound to a remote client */
  def receive = {
    case payload =>
      send(payload)
  }

  /**
    * Sends a message using sockets.
    * @param payload A message
    */
  def send(payload: Any): Unit = {
    LOG.info("send: relaying " + payload + " to remote host " + destHost + ":" + destPort)
    val socket = new Socket(destHost, destPort)

    val os = socket.getOutputStream
    val oos = new ObjectOutputStream(os)

    val task = Task(replyHost, replyPort, payload)
    oos.writeObject(task)
    LOG.info("send: sent successfully Task = "+task+" to "+destHost+":"+destPort)

    oos.flush
    oos.close

    os.close
  }


}

class ReplyHandler(replyHost: String, replyPort: Int, callback: ActorRef) extends Thread {
  import org.apache.log4j.Logger
  val LOG = Logger.getLogger(getClass)

  /** Runs the worker thread to receive replies. */
  override def run(): Unit = {
    LOG.info("run: thread started to receive replies")
    val socket = new ServerSocket(replyPort)

    while(true) {
      LOG.info("run: waiting to accept connection")
      val clientSocket = socket.accept()

      LOG.info("run: connection accepted")
      val ois = new ObjectInputStream(clientSocket.getInputStream)

      val reply = ois.readObject match {
        case r: Reply =>
          r

        case _ =>
          LOG.info("run: received unknown object to reply")
          "some unknown object"
      }
      LOG.info("run: received message to reply: "+reply)
      LOG.info("run: callback actor is "+callback)
      LOG.info("run: i am "+this)

      callback ! "here's a pre-reply message from Relay"
      callback ! reply

      ois.close
      clientSocket.close
    }
  }
}



