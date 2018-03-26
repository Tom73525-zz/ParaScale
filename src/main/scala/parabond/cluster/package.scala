/*
 * Copyright (c) Ron Coleman
 * See CONTRIBUTORS.TXT for a full list of copyright holders.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Scaly Project nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE DEVELOPERS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package parabond

import org.apache.log4j.Logger
import parascale.parabond.util.Constant.{PORTF_NUM}
import parascale.parabond.util.{Job, Helper}
import parascale.util.getPropertyOrElse

import scala.collection.GenSeq
import scala.collection.parallel.ParSeq

package object cluster {
  /**
    * Converts nano-seconds to seconds implicitly.
    * See https://alvinalexander.com/scala/scala-how-to-add-new-methods-to-existing-classes
    * @param dt Time
    */
  class NanoToSecondsCoverter(dt: Double) {
    def this(t: Long) = this(t.toDouble)
    def seconds = dt / 1000000000.0
  }

  // Compiler may complain without this import
  import scala.language.implicitConversions

  // Actual implicit conversions
  implicit def nanoSecondsToSeconds(dt: Long) = new NanoToSecondsCoverter(dt)
  implicit def nanoSecondsToSeconds(dt: Double) = new NanoToSecondsCoverter(dt)

  /**
    * Writes a report to the diagnostic log
    * @param log Logger to use
    * @param analysis Results to report
    */
  def report(log: Logger, analysis: Analysis): Unit = {
    // Generate the detailed output report, if needed
    val details = getPropertyOrElse("details",false)

    val results = analysis.results

    if(details) {
      log.info("%6s %10.10s %-5s %-2s".format("PortId","Price","Bonds","dt"))

      val t0 = analysis.t0

      results.foreach { output =>
        val id = output.portfId

        val dt = (output.result.t1 - output.result.t0) seconds

        val bondCount = output.result.bondCount

        val price = output.result.value

        log.info("%6d %10.2f %5d %6.4f %12d %12d".format(id, price, bondCount, dt, output.result.t1 - t0, output.result.t0 - t0))
      }
    }

    val dt1 = results.foldLeft(0.0) { (sum, output) =>
      sum + (output.result.t1 - output.result.t0)
    } seconds


    val dtN = (analysis.t1 - analysis.t0) seconds

    val speedup = dt1 / dtN

    val numCores = Runtime.getRuntime().availableProcessors()

    val e = speedup / numCores

    log.info("dt(1): %7.4f  dt(N): %7.4f  cores: %d  R: %5.2f  e: %5.2f ".format(dt1,dtN,numCores,speedup,e))

    val n = getPropertyOrElse("n", PORTF_NUM)
    log.info("DONE! %d %7.4f %7.4f".format(n, dt1, dtN))
  }
}
