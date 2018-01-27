//package actor
//
//import java.net.InetAddress
//
//import akka.actor.{Actor, ActorRef, ActorSystem, Props}
//import parascale.actor.remote.ukko.{Relay, Reply}
//
//object TestActor1 {
//  def main(args: Array[String]): Unit = {
//    val system = ActorSystem("ParaScale")
//
//    val me = system.actorOf(Props[TestActor2],"TestActor1")
//    println("i am "+me)
//
//    val destHost = InetAddress.getLocalHost.getHostAddress
//    val destPort = 9000
//    val relay: ActorRef = system.actorOf(Props(parascale.actor.remote.ukko.Relay(destHost,destPort,callback=me)),name="Relay")
//
//    relay ! A("got it!")
//  }
//}
//
//class TestActor1 extends Actor {
//  def receive = {
//    case s: String =>
//      println("T1: got message '"+s+"'")
//
//    case reply: Task =>
//      println("T1: got reply with payload "+reply.payload)
//
//    case obj =>
//      println("T1: test actor 1 got "+obj)
//  }
//}
