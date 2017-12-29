package parascale.mapreduce

class FasterPerfectNumberFinder {
  def isPerfectConcurrent(candidate: Long): Boolean = {
//    val RANGE = 1000000L
//
//    val numberOfPartitions = (candidate.toDouble / RANGE).ceil.toInt
//
//    val ranges = for (i <- 0L until numberOfPartitions) yield {
//      val lower: Long = i * RANGE + 1
//
//      val upper: Long = candidate min (i + 1) * RANGE
//
//      (lower, upper)
//    }
//
//    import scala.concurrent.{Await, Future}
//    import scala.concurrent.ExecutionContext.Implicits.global
//    val futuresums = ranges.map { lowerUpper =>
//      Future {
//        val (lower, upper) = lowerUpper
//        sumOfFactorsInRange_(lower, upper, candidate)
//      }
//    }
//
//    import scala.concurrent.duration._
//    val total = futuresums.reduce { (futurea, futureb) =>
//      val a = Await.result(futurea, 100 seconds)
//      val b = Await.result(futureb, 100 seconds)
//
//      a + b
//    }
//
//    2 * candidate == total

    false
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
