package parascale.thread.actor

/**
  * Tasks are both produced and consumed.
  * @param num Task number
  */
case class Task(num: Int, duration: Long)