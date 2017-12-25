package scalaparallel.thread.actor

import org.apache.log4j.Logger

import scala.collection.mutable.ListBuffer
import scala.util.Random

/**
  * This class implements a consumer thread which receives and processes tasks through
  * its mailbox.
  */
class Worker(id: Int) extends Thread {
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

    val tasks = ListBuffer[Task]()

    while (true) {
      // Wait to receive a task, if there is one
      receive match {
        case Some(task) =>
          // If not done, record the effort for analysis later
          if(task != DONE)
            tasks.append(process(task))
          else {
            // If done, log the effort and stop the thread
            tasks.foreach { task =>
              LOG.info("duration " + task.duration)
            }
            return
          }

        case None =>
          LOG.debug("mailbox empty")
      }
    }
  }

  /**
    * Processes a task.
    * @param task Task
    */
  def process(task: Task): Task = {
    if(task != DONE) {
      LOG.debug("consuming task " + task.num)
      val working = ran.nextInt(MAX_WORKING)
      Thread.sleep(working)

      Task(task.num,working)
    }
    else
      DONE
  }
}
