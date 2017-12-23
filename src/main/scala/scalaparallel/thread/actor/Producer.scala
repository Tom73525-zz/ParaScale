package scalaparallel.thread.actor

import org.apache.log4j.Logger

import scala.util.Random

object Producer {
  val LOG =  Logger.getLogger(getClass)

  def main(args: Array[String]): Unit = {
  val config = Config()

    val consumers = (0 until config.numConsumers).foldLeft(List[Consumer]()) { (list, n) =>
      val consumer = new Consumer(n)

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
      consumer.send(Constant.DONE)
      consumer.join
    }
  }

  /**
    * Produces a task.
    * @param num Task number
    * @return Task
    */
  def produce(num: Int): Task = {
    Thread.sleep(250)

    LOG.debug("producing task "+num)
    Task(num)
  }
}


