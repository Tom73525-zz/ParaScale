package parascale.actor.remote.ukko

trait Actor extends Runnable {


  val mailbox = new Mailbox[Task]

  new Thread(this).start

  /** Deposits a task in the mailbox. */
  def send(msg: Any): Unit = {
    msg match {
      case task: Task =>
        mailbox.add(task)

      case _ =>
        mailbox.add(Task(Task.UNKNOWN_HOST,Task.UNKNOWN_PORT,msg))
    }

  }

  /** Retrieves a task from the mailbox.*/
  def receive: Task = mailbox.remove
}
