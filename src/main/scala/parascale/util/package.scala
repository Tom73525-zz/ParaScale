package parascale

package object util {
  /**
    * Gets an integer value from system properties, if it's not found use a default.
    * @param key Property
    * @param default Default integer
    * @return Default integer value
    */
  def getPropertyOrDefault(key: String, default: Int): Int = getPropertyOrDefault(key,Integer.parseInt,default)

  /**
    * Gets a generic property from the system properyies, if it's not found use a default.
    * @param key Property
    * @param parse Parser
    * @param default Default
    * @tparam T Parameterize type of value
    * @return Default value
    */
  def getPropertyOrDefault[T](key: String, parse: (String) => T, default: T): T = {
    val value = System.getProperty(key)

    if(value == null)
      default
    else
      parse(value)
  }

  /**
    * Convenience method for sleeping.
    * @param millis Time in milliseconds to sleep
    */
  def sleep(millis: Long): Unit = Thread.sleep(millis)

  /**
    * Convenience method for sleeping.
    * @param seconds Time in seconds.
    */
  def sleep(seconds: Double): Unit = sleep((seconds * 1000 + 0.5).toLong)
}
