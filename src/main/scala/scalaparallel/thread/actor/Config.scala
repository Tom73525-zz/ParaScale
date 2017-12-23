package scalaparallel.thread.actor

case class Config(numConsumers: Int, numTasks: Int)

object Config {
  def apply(): Config = {
    val consumersProp = System.getProperty("scalaparallel.consumers")

    val numConsumers = consumersProp match {
      case prop:String =>
        prop.toInt

      case null =>
        Runtime.getRuntime.availableProcessors
    }

    val tasksProp = System.getProperty("scalaparallel.tasks")
    val numTasks = consumersProp match {
      case prop:String =>
        prop.toInt

      case null =>
        Constant.NUM_TASKS
    }

    Config(numConsumers, numTasks)
  }
}
