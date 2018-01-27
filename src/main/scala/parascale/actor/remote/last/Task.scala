package parascale.actor.remote.last

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

case class Task(host: String, port: Int, payload: Any, kind: Int = Task.TASK) extends Serializable {
  def reply(msg: Any): Unit = {
    val task = msg match {
      case task: Task =>
        task

      case _ =>
        Task(InetAddress.getLocalHost.getHostAddress, -1, msg, Task.REPLY)
    }
    println("Task: replying to "+host+":"+port+" payload = "+payload)
    val socket = new Socket(host, port)

    val os = socket.getOutputStream

    val oos = new ObjectOutputStream(os)

    oos.writeObject(task)

    oos.flush
    oos.close

    os.close
  }
}