package sp.thread

import scala.util.Random

/**
  * Tasks are both produced and consumed.
  * @param num Task number
  */
case class Task(num: Int)

/** Some constants */
object Constant {
  val MAX_WORKING = 5 * 1000
  val NUM_TASKS = 5
  val DONE = Task(-1)
}

/** This object produces tasks and sends them to the consumer. */
object Producer {
  import Constant._

  val ran = new Random(1)

  /**
    * Main processing loop for creating the consumer, sending tasks to it, and
    * waiting for it to finish.
    * @param args
    */
  def main(args: Array[String]): Unit = {
    // Create the consumer and start it running
    val consumer = new Consumer

    consumer.start

    // Produce tasks and send each one in turn to the consumer
    for (num <- 0 until NUM_TASKS) {
      val task = produce(num)

      consumer.send(task)
    }

    // Send last task for the day
    consumer.send(DONE)

    // Wait for the consumer to finish
    consumer.join
  }

  /**
    * Produces a task.
    * @param num Task number
    * @return Task
    */
  def produce(num: Int): Task = {
    val working = ran.nextInt(MAX_WORKING)
    Thread.sleep(working)

    println("producing task "+num)
    Task(num)
  }
}

/**
  * This class implements the consumer's mailbox
  */
class Mailbox {
  val NEXT = 0

  // Implement the mail queue as a mutable list buffer.
  import scala.collection.mutable.ListBuffer
  val queue = ListBuffer[Task]()

  /**
    * Adds a task to the mailbox.
    * @param task Task
    */
  def add(task: Task): Unit = synchronized {
    // Adds a task then wakes up any waiting threads
    queue.append(task)

    // Note: notifications are NOT buffered, although tasks are!
    this.notify
  }

  /**
    * Removes a task from the queue.
    * @return Some task
    */
  def remove: Option[Task] = synchronized {
    // If the queue is emtpy then we'll wait otherwise get the next task immediately
    if(queue.isEmpty)
      this.wait

    val task = queue.remove(NEXT);

    Some(task)
  }
}

/**
  * This class implements a consumer thread which receives and processes tasks through
  * its mailbox.
  */
class Consumer extends Thread {
  import Constant._

  val ran = new Random

  val mailbox = new Mailbox

  /** Deposits a task in the mailbox. */
  def send(task: Task): Unit = mailbox.add(task)

  /** Retrieves a task from the mailbox.*/
  def receive: Option[Task] = mailbox.remove

  /** Runs the consumer loop. */
  override def run(): Unit = {
    while (true) {
      // Wait to receive a task, if there is one
      receive match {
        case Some(task) =>
          consume(task)

          if(task == DONE)
            return

        case None =>
          println("mailbox empty")
      }
    }
  }

  /**
    * Consumes a task.
    * @param task Task
    */
  def consume(task: Task): Unit = {
    val working = ran.nextInt(MAX_WORKING)
    println("consuming task "+task.num)
    Thread.sleep(working)
  }
}