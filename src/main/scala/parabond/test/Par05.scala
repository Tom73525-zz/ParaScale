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
package parascale.parabond.test

import parascale.parabond.casa.{MongoDbObject, MongoHelper}
import parascale.parabond.util.{Helper, Result, Job}
import parascale.parabond.value.SimpleBondValuator

import scala.util.Random
import parascale.parabond.entry.SimpleBond
import parascale.util._
import parascale.parabond.util.Constant.{DIAGS_DIR, PORTF_NUM}


/** Test driver */
object Par05 {
  def main(args: Array[String]): Unit = {
    new Par05 test
  }
}

/**
 * This class uses parallel collections to price n portfolios in the
 * parabond database using the composite fine-grain algorithm. This class
  * differs from Par04 in that it parallel processes the bond valuations.
 * @author Ron Coleman
 */
class Par05 {
  /** Initialize the random number generator */
  val ran = new Random(0)   

  /** Runs the unit test */
  def test {
    // Set the number of portfolios to analyze
    val n = getPropertyOrElse("n",PORTF_NUM)

    val me =  this.getClass().getSimpleName()

    val dir = getPropertyOrElse("dir",DIAGS_DIR)

    val outFile = dir + me + "-dat.txt"
    
    val fos = new java.io.FileOutputStream(outFile,true)
    val os = new java.io.PrintStream(fos)
    
    os.print(me+" "+ "N: "+n+" ")

    val details = getPropertyOrElse("details",parseBoolean,false)
    
    // Build the portfolio list    
    val jobs = for(i <- 0 until n) yield Job(ran.nextInt(100000)+1,null, null)
    
    // Build the portfolio list
    val t0 = System.nanoTime
    val results = jobs.par.map(price)
    val t1 = System.nanoTime

    // Generate the detailed output report
    if(details) {
      println("%6s %10.10s %-5s %-2s".format("PortId","Price","Bonds","dt"))

      results.foreach { output =>
        val id = output.result.portfId

        val dt = (output.result.t1 - output.result.t0) / 1000000000.0

        val bondCount = output.result.bondCount

        val price = output.result.value

        println("%6d %10.2f %5d %6.4f %12d %12d".format(id, price, bondCount, dt, output.result.t1 - t0, output.result.t0 - t0))
      }
    }

    val dt1 = results.foldLeft(0.0) { (sum,result) =>
      sum + (result.result.t1 - result.result.t0)

    } / 1000000000.0

    val dtN = (t1 - t0) / 1000000000.0

    val speedup = dt1 / dtN

    val numCores = Runtime.getRuntime().availableProcessors()

    val e = speedup / numCores

    os.println("dt(1): %7.4f  dt(N): %7.4f  cores: %d  R: %5.2f  e: %5.2f ".
        format(dt1,dtN,numCores,speedup,e))

    os.flush

    os.close

    println(me+" DONE! %d %7.4f".format(n,dtN))
  }

  /**
    * Price a portfolio
    * @param job Portfolio
    * @return Valuation
    */
  def price(job: Job): Job = {

    // Value each bond in the portfolio
    val t0 = System.nanoTime

    // Retrieve the portfolio
    val portfId = job.portfId

    val portfsQuery = MongoDbObject("id" -> portfId)

    val portfsCursor = MongoHelper.portfolioCollection.find(portfsQuery)

    // Get the bonds in the portfolio
    val bids = MongoHelper.asList(portfsCursor,"instruments")

    val bondIds = for(i <- 0 until bids.size) yield Job(bids(i),null,null)

//    val bondIds = asList(portfsCursor,"instruments")

    val output = bondIds.par.map { bondId =>
      // Get the bond from the bond collection
      val bondQuery = MongoDbObject("id" -> bondId.portfId)

      val bondCursor = MongoHelper.bondCollection.find(bondQuery)

      val bond = MongoHelper.asBond(bondCursor)

      val valuator = new SimpleBondValuator(bond, Helper.yieldCurve)

      val price = valuator.price

      new SimpleBond(bond.id,bond.coupon,bond.freq,bond.tenor,price)
    }.par.reduce(sum)

    MongoHelper.updatePrice(job.portfId,output.maturity)

    val t1 = System.nanoTime

    Job(job.portfId,null,Result(job.portfId,output.maturity,bondIds.size,t0,t1))
  }

  /**
    * Reduces to simple bond prices.
    * @param a Bond a
    * @param b Bond b
    * @return Reduced bond price
    */
  def sum(a: SimpleBond, b:SimpleBond) : SimpleBond = {
    new SimpleBond(0,0,0,0,a.maturity+b.maturity)
  }  
}