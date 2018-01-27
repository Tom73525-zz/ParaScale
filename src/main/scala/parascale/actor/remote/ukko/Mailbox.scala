package parascale.actor.remote.ukko

/**
  * This class implements a synchronized mailbox in the actor style.
  */
class Mailbox[T] {
  // Next or head index in the mail queue
  val NEXT = 0

  // Implement the mail queue as a mutable list buffer.
  import scala.collection.mutable.ListBuffer
  val queue = ListBuffer[T]()

  /**
    * Adds a task to the mailbox.
    * @param item An item
    */
  def add(item: T): Unit = synchronized {
    // Adds a task then wakes up any waiting threads
    queue.append(item)

    // Note: notifications are NOT buffered, although tasks are!
    this.notify
  }

  /**
    * Removes a task from the queue.
    * @return Some task
    */
  def remove: T = synchronized {
    // If the queue is emtpy then we'll wait otherwise get the next task immediately
    if(queue.isEmpty)
      this.wait

    val item = queue.remove(NEXT)

    item
  }
}

