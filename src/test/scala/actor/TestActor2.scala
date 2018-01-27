//package actor
//
//import akka.actor.{Actor, ActorRef, ActorSystem, Props}
//import parascale.actor.remote.ukko.{Remote, Reply, Task}
//
//object TestActor2 {
//  def main(args: Array[String]): Unit = {
//    val system = ActorSystem("ParaScala")
//    val actor: ActorRef = system.actorOf(Props[TestActor2],"TestActor2")
//    println("i am "+actor)
//
//    new parascale.actor.remote.ukko.Remote(actor,9000)
//
//    Thread.sleep(1000)
//
//    actor ! "hello world"
//  }
//}
//
//class TestActor2 extends Actor {
//  override def receive = {
//    case a: A =>
//      println("T2: got an A!")
//
//    case s: String =>
//      println("T2: i am "+this)
//      println("T2: "+s)
//
//    case task: Task =>
//      println("T2: i am "+this)
//      println("T2: got a task replies to "+task.srcHost+":"+task.srcPort)
//      task.payload match {
//        case a: A =>
//          println("T2: payload is A: "+a.s)
//
//          task.reply("received your A")
//
//      }
//
//    case reply: Reply =>
//      println("T2: got the reply "+reply)
//  }
//}
