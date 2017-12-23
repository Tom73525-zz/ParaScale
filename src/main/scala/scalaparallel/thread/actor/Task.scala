package scalaparallel.thread.actor

/**
  * Tasks are both produced and consumed.
  * @param num Task number
  */
case class Task(num: Int)
case class Effort(duration: Long)
