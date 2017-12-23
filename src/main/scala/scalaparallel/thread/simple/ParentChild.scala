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
package scalaparallel.thread.simple

/**
  * This object is responsible for spawning the child thread and waiting for it
  * to complete.
  */
object Parent {
  def main(args: Array[String]): Unit = {
    val numCores = Runtime.getRuntime.availableProcessors

    val child = new Child(numCores)

    child.start

    val numThreads = Thread.activeCount

    println("parent: threads = "+numThreads)

    child.join
  }
}

/**
  * This class runs the child thread.
  * @param n Number of cores passed from the parent.
  */
class Child(n: Int) extends Thread {
  /**
    * This method gets invoke when the parent does start.
    */
  override def run(): Unit = {
    println("child: cores = "+n)
  }
}
