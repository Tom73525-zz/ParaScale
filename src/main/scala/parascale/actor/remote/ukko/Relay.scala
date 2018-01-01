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
package parascale.actor.remote.ukko

import java.io.{FileOutputStream, ObjectInputStream, ObjectOutputStream}
import java.net.{InetAddress, ServerSocket, Socket}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

/**
  * This class is an actor which relays message from the local (or srouce host) to a remote (or destination) host.
  * @param destHost Destination host
  * @param destPort Port on destination host
  * @param callback Actor to receive replies.
  */
class Relay(destHost: String, destPort: Int, callback: ActorRef) extends Actor with Runnable {
  import org.apache.log4j.Logger
  val LOG =  Logger.getLogger(getClass)

  // For sending reply messages
  val srcHost =  InetAddress.getLocalHost.getHostAddress
  LOG.info("listening for replies as host "+srcHost)

  val srcPort = destPort + Thread.activeCount
  LOG.info("listening for replies to port "+srcPort)

  // Start the listener thread to receive replies
  new Thread(this).start

  /** Receives actor messages to be relayed outbound to a remote client */
  def receive = {
    case obj =>
      send(Task(srcHost, srcPort, obj))
  }

  /**
    * Sends a message using sockets.
    * @param msg A message
    */
  def send(msg: Any): Unit = {
    LOG.info("relaying " + msg + " to remote host " + destHost + ":" + destPort)
    val socket = new Socket(destHost, destPort)

    val os = socket.getOutputStream
    val oos = new ObjectOutputStream(os)

    oos.writeObject(msg)
    LOG.info("message relayed successfully")

    oos.flush
    oos.close

    os.close
  }

  /** Runs the worker thread to receive replies. */
  def run(): Unit = {
    LOG.info("thread started to receive replies")
    val socket = new ServerSocket(srcPort)

    while(true) {
      LOG.info("waiting to accept connection")
      val clientSocket = socket.accept()

      LOG.info("connection accepted")
      val ois = new ObjectInputStream(clientSocket.getInputStream)

      val msg = ois.readObject
      LOG.info("received reply message "+msg)

      callback ! msg

      ois.close
      clientSocket.close

    }
  }

}



