package parascale.actor.remote.ukko

import java.io.ObjectOutputStream
import java.net.{InetAddress, Socket}

object TaskType extends Enumeration {
  val TASK, REPLY = Value
}

object Task {
  val TASK = 0
  val REPLY = 1
  val UNKNOWN_HOST = ""
  val UNKNOWN_PORT = -1
}

case class Task(srcHost: String, srcPort: Int, payload: Any, kind: Int = Task.TASK) extends Serializable {
  def reply(payload: Any): Unit = {
    println("Task: replying to "+srcHost+":"+srcPort+" payload = "+payload)
    val socket = new Socket(srcHost,srcPort)

    val os = socket.getOutputStream

    val oos = new ObjectOutputStream(os)

    oos.writeObject(new Task(InetAddress.getLocalHost.getHostAddress, -1, payload, Task.REPLY))

    oos.flush
    oos.close

    os.close
  }
}