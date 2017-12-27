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
package scalaparallel.actor.nonblocking.perfect

/**
  * Finds the perfect numbers from a candidate list.
  */
object PerfectNumberFinder extends App {
  (0 until candidates.length).foreach { index =>
    val num = candidates(index)
    println(num + " is perfect? "+ ask(isPerfect_,num))
  }

  /**
    * Sums the factors of a number using a range which may explode for large numbers.
    * @param number Number
    * @return sum of factors
    */
  def sumOfFactors(number: Long) = {
    (1L to number.toLong).foldLeft(0L) { (sum, i) => if (number % i == 0L) sum + i else sum }
  }

  /**
    * Sums the factors using a loop which is more robust for large numbers.
    * @param number Number
    * @return sum of factors
    */
  def sumOfFactors_(number: Long) = {
    var i = 1L
    var sum = 0L
    while(i <= number.toLong) {
      if(number % i == 0)
        sum += i
      i += 1
    }
    sum
  }

  /**
    * Retruns true if the candidate is perfect.
    * @param candidate Candidate number
    * @return True if candidate is perfect, false otherwise
    */
  def isPerfect(candidate: Long) = 2 * candidate == sumOfFactors(candidate)

  /**
    * Retruns true if the candidate is perfect.
    * @param candidate Candidate number
    * @return True if candidate is perfect, false otherwise
    */
  def isPerfect_(candidate: Long) = 2 * candidate == sumOfFactors_(candidate)
}
