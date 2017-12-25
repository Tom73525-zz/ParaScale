package scalaparallel.thread.actor

import scalaparallel.thread.actor.Constant._
import org.apache.log4j.Logger
import scala.util.Random

object Dispatcher extends App {
  val LOG =  Logger.getLogger(getClass)

  val config = Config()

    val consumers = (0 until config.numConsumers).foldLeft(List[Worker]()) { (list, n) =>
      val consumer = new Worker(n)

      consumer.start

      consumer :: list
    }

    val ran = new Random

    for(taskno <- 0 until Constant.NUM_TASKS) {
      val task = produce(taskno)

      val index = ran.nextInt(consumers.size)

      consumers(index).send(task)
    }

    consumers.foreach { consumer =>
      consumer.send(DONE)
      consumer.join
  }

  /**
    * Produces a task.
    * @param num Task number
    * @return Task
    */
  def produce(num: Int): Task = {
    Thread.sleep(MAX_PRODUCING)

    LOG.debug("producing task "+num)
    Task(num, MAX_PRODUCING)
  }
}


