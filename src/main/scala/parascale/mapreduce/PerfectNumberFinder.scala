package parascale.mapreduce

class PerfectNumberFinder {
  def isPerfectConcurrent(candidate: Long): Boolean = {
    val RANGE = 1000000L

    val numberOfPartitions = (candidate.toDouble / RANGE).ceil.toInt

    val ranges = for (i <- 0L until numberOfPartitions) yield {
      val lower: Long = i * RANGE + 1

      val upper: Long = candidate min (i + 1) * RANGE

      (lower, upper)
    }

    val sums = ranges.map { lowerUpper =>
      val (lower, upper) = lowerUpper
      sumOfFactorsInRange_(lower, upper, candidate)
    }

    val total = sums.reduce { (a, b) =>
      a + b
    }

    2 * candidate == total
  }

  /**
    * Computes the sum of factors in a range using a loop which is robust for large numbers.
    *
    * @param lower  Lower part of range
    * @param upper  Upper part of range
    * @param number Number
    * @return Sum of factors
    */
  def sumOfFactorsInRange_(lower: Long, upper: Long, number: Long): Long = {
    var index: Long = lower

    var sum = 0L

    while (index <= upper) {
      if (number % index == 0L)
        sum += index

      index += 1L
    }

    sum
  }
}
