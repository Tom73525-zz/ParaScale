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
package scalaparallel.future

package object perfect {
  /** Candidate perfect numbers */
  val candidates: List[Long] = List(6, 28, 496, 8128, 33550336, 33550336+1, 8589869056L, 137438691328L, 2305843008139952128L)

  /**
    * Convenience method to measure the runtime of a method.
    * @param method Method to invoke
    * @param number Number to query
    * @return True if the method is true, false otherwise
    */
  def ask(method: Long => Boolean, number: Long): String = {
    val t0 = System.nanoTime

    val result = method(number)

    val t1 = System.nanoTime

    val answer = if(result) "YES" else "NO"

    answer + " dt = "+(t1-t0)/1000000000.0 + "s"
  }
}
