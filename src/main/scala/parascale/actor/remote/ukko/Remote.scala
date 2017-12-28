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

import java.io.{EOFException, FileInputStream, ObjectInputStream}
import java.net.ServerSocket
import akka.actor.{Actor, ActorRef, ActorSystem, Props}

/**
  * This actor runs on the client side and relays message to the local actor
  * @param actor Local actor
  * @param localPort Port to listen for inbound remote messages.
  */
class Remote(actor: ActorRef, localPort: Int) extends Thread {
  // Start the thread to receive inbound messages
  new Thread(this).start

  /** Relays inbound message to the (local) actor */
  override def run(): Unit = {
    println("receiver for actor started")
    val socket = new ServerSocket(localPort)

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

