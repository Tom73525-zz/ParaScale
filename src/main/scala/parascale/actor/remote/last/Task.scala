package parascale.actor.remote.last

import java.io.ObjectOutputStream
import java.net.{InetAddress, Socket}

/** Experimental task enumeration */
object TaskType extends Enumeration {
  val TASK, REPLY = Value
}

/** Task constants */
object Task {
  val TASK = 0
  val REPLY = 1
  val LOCAT_HOST = InetAddress.getLocalHost.getHostAddress + ":0"
}

/**
  * Wrapper class for all actor messages.
  * @param src Source socket has address+":"+port
  * @param payload Payload
  * @param kind Kind of task
  */
case class Task(src: String, payload: Any, kind: Int = Task.TASK) extends Serializable {
  import Task._
  def reply(that: Any): Unit = {
    val params = src.split(":")
    val addr = params(0)
    val port = params(1).toInt

    // TODO: do something better here.
    if(port == 0)
      return

    // Wrap that if necessary as a reply
    val task = that match {
      case task: Task =>
        task

      case _ =>
        Task(LOCAT_HOST, that, REPLY)
    }

    // Connect to remote reply host
    val socket = new Socket(addr, port)

    // If we get here, there's somebody listening on the other side
    val os = socket.getOutputStream

    // Serialize the task and send it
    val oos = new ObjectOutputStream(os)

    oos.writeObject(task)

    oos.flush
    oos.close

    os.close
  }

  /**
    * Sends a message.
    * @param that Message
    */
  def !(that: Any) = reply(that)
}