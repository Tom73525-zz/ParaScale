package scalaparallel.actor.remote.ukko

import java.io.{FileOutputStream, ObjectInputStream, ObjectOutputStream}
import java.net.{InetAddress, ServerSocket, Socket}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}


object CourierActor {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Transceiver")
    val actor: ActorRef = null
    val courier: ActorRef = system.actorOf(Props(new CourierActor("127.0.0.1",9000,actor)),name="TransSender")
    //    val courier: ActorRef = system.actorOf(Props[CourierActor],"TransSender")

    courier ! A("got it!")
  }
}

class CourierActor(remoteHost: String, port: Int, localActor: Option[ActorRef]) extends Actor with Runnable {
  def this(remoteHost: String,port: Int) = this(remoteHost,port,None)
  def this(remoteHost: String, port: Int, actor: ActorRef) = this(remoteHost,port,Some(actor))

//  val fo = new FileOutputStream(host)
//  val oos = new ObjectOutputStream(fo)

  val localHost =  InetAddress.getLocalHost.getHostAddress

  localActor match {
    case Some(_) =>
      new Thread(this).start

    case None =>
  }

  def run(): Unit = {
    val socket = new ServerSocket(port+1)

    while(true) {
      val clientSocket = socket.accept()

      val ois = new ObjectInputStream(clientSocket.getInputStream)

      val obj = ois.readObject

      localActor match {
        case Some(actor) =>
          actor ! obj

        case None =>
      }

      ois.close
      clientSocket.close

    }
  }

  /** Receives messages to be relayed outbound to a remote client */
  def receive = {

    case obj =>
      val msg = localActor match {
        case Some(_) =>
          Packet(localHost,port,obj)
        case None =>
          obj
      }

      send(msg)
  }

  def send(msg: Any): Unit = {
    val socket = new Socket(remoteHost,port)

    val os = socket.getOutputStream

    val oos = new ObjectOutputStream(os)

    oos.writeObject(msg)

    oos.flush
    oos.close

    os.close
  }
}



