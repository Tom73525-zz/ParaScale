package scalaparallel.thread.actor

import org.apache.log4j.Logger

import scala.collection.mutable.ListBuffer
import scala.util.Random

/**
  * This class implements a consumer thread which receives and processes tasks through
  * its mailbox.
  */
class Consumer (id: Int) extends Thread {
  import Constant._

  val LOG =  Logger.getLogger(getClass)
  val ran = new Random

  val mailbox = new Mailbox

  /** Deposits a task in the mailbox. */
  def send(task: Task): Unit = mailbox.add(task)

  /** Retrieves a task from the mailbox.*/
  def receive: Option[Task] = mailbox.remove

  /** Runs the consumer loop. */
  override def run(): Unit = {
    LOG.info("consumer " + id + " started")

    val efforts = ListBuffer[Effort]()

    while (true) {
      // Wait to receive a task, if there is one
      receive match {
        case Some(task) =>
          // If not done, record the effort for analysis later
          if(task != DONE)
            efforts.append(consume(task))
          else {
            // If done, log the effort and stop the thread
            efforts.foreach { effort =>
              LOG.info("duration " + effort.duration)
            }
            return
          }

        case None =>
          LOG.debug("mailbox empty")
      }
    }
  }

  /**
    * Consumes a task.
    * @param task Task
    */
  def consume(task: Task): Effort = {
    if(task != DONE) {
      LOG.debug("consuming task " + task.num)
      val working = ran.nextInt(MAX_WORKING)

      Thread.sleep(working)
      Effort(working)
    }
    else
      Effort(-1)
  }
}
