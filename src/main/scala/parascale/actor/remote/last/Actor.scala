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
package parascale.actor.remote.last

/**
  * This construct is the base level definition of an actor.
  * Subclasses are started automatically when the actor gets instantiated.
  */
trait Actor extends Runnable {
  // Mailbox used by the actor
  val mailbox = new Mailbox[Task]

  // Automatically starts the actor by invoking its run method
  new Thread(this).start

  /**
    * Deposits a task in the mailbox.
    * @param that Message
    */
  def send(that: Any): Unit = {
    that match {
      case task: Task =>
        mailbox.add(task)

      case _ =>
        mailbox.add(Task(Task.LOCAT_HOST, that))
    }
  }

  /** Retrieves a task from the mailbox.*/
  def receive: Task = mailbox.remove

  /**
    * Sends a message to this actor.
    * @param that Message to send
    */
  def !(that: Any) = send(that)
}
