package parascale.thread.actor

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
