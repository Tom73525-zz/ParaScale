package scalaparallel.actor.remote.ukko

import java.io.ObjectOutputStream
import java.net.Socket

case class Packet(host: String, port: Int, payload: Any) extends Serializable {
  def reply(msg: Any): Unit = {
    val socket = new Socket(host,port)

    val os = socket.getOutputStream

    val oos = new ObjectOutputStream(os)

    oos.writeObject(msg)

    oos.flush
    oos.close

    os.close
  }
}
