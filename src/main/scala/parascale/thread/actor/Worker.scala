/*
 Copyright (c) Ron Coleman

 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files (the
 "Software"), to deal in the Software without restriction, including
 without limitation the rights to use, copy, modify, merge, publish,
 distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to
 the following conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package parascale.thread.actor

import org.apache.log4j.Logger

import scala.collection.mutable.ListBuffer
import scala.util.Random

/**
  * This class consumes tasks through
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
