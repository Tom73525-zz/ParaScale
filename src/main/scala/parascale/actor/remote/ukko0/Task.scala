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

import java.io.ObjectOutputStream
import java.net.{InetAddress, Socket}

/**
  * A wrapper class for remote actors to reply
  * @param srcHost Source host
  * @param srcPort Source port
  * @param payload Inbound payload
  */
case class Task(srcHost: String, srcPort: Int, payload: Any) extends Serializable {
  def reply(payload: Any): Unit = {
    println("Task: replying to "+srcHost+":"+srcPort+" payload = "+payload)
    val socket = new Socket(srcHost,srcPort)

    val os = socket.getOutputStream

    val oos = new ObjectOutputStream(os)

    oos.writeObject(new Reply(InetAddress.getLocalHost.getHostAddress, payload))

    oos.flush
    oos.close

    os.close
  }

//  /**
//    * Converts instance to string.
//    * @return String representation
//    */
//  override def toString = "payload " + payload + " replies to " + srcHost + ":" +srcPort
}
