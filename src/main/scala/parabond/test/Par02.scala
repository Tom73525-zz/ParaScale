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

import parascale.parabond.casa.MongoHelper
import parascale.parabond.casa.MongoHelper.PortfIdToBondsMap
import parascale.parabond.util.{Work, Helper, Result}
import parascale.parabond.value.SimpleBondValuator
import scala.collection.mutable.ListBuffer
import scala.util.Random
import parascale.parabond.entry.SimpleBond
import parabond.mr.PORTF_NUM

/** Test driver */
object Par02 {
  def main(args: Array[String]): Unit = {
    new Par02 test
  }
}

/**
 * This class runs a parallel collections unit test for n portfolios in the
 * parabond database. It uses one portfolio per map by loading all the bonds
 * into memory.
 * @author Ron Coleman
 */
class Par02 {
  /** Initialize the random number generator */
  val ran = new Random(0)   
  
  /** Write a detailed report */
  val details = false

  def test {
    // Set the number of portfolios to analyze
    val arg = System.getProperty("n")
    
    val n = if(arg == null) PORTF_NUM else arg.toInt
    
    print("\n"+this.getClass()+" "+ "N: "+n+" ")
    
    val details = if(System.getProperty("details") != null) true else false
    
    val t2 = System.nanoTime
    val input = loadPortfsPar2(n)
    val t3 = System.nanoTime   

    val t0 = System.nanoTime

    // Build the portfolio list
    val results = input.par.map(price)

    val value = results.par.reduce { (a: Work, b:Work) =>
      Work(0,null,Result(0,a.result.value + b.result.value,0,0,0))
    }
    val t1 = System.nanoTime

    // Generate the output report
    if(details)
      println("%6s %10.10s %-5s %-2s".format("PortId","Price","Bonds","dt"))

    val dt1 = results.foldLeft(0.0) { (sum,result) =>
      sum + (result.result.t1 - result.result.t0)

    } / 1000000000.0

    val dtN = (t1 - t0) / 1000000000.0

    val speedup = dt1 / dtN

    val numCores = Runtime.getRuntime().availableProcessors()

    val e = speedup / numCores

    println("dt(1): %7.4f  dt(N): %7.4f  cores: %d  R: %5.2f  e: %5.2f ".
        format(dt1,dtN,numCores,speedup,e))

    println("load t: %8.4f ".format((t3-t2)/1000000000.0))
  }

  /**
    * Price a portfolio.
    * @param portf Portfolio
    * @return Result data
    */
  def price(portf: Work): Work = {

    // Value each bond in the portfolio in parallel
    val t0 = System.nanoTime

//    val results = portf.bonds.par.map { bond =>
//      val valuator = new SimpleBondValuator(bond, Helper.curveCoeffs)
//
//      val price = valuator.price
//
//      new SimpleBond(bond.id,bond.coupon,bond.freq,bond.tenor,price)
//    }

    val results = portf.bonds.par.map(finePrice)

    // Sum the bond prices.
    val bondsValue = results.par.reduce { (a:SimpleBond, b: SimpleBond) =>
      new SimpleBond(0,0,0,0,a.maturity+b.maturity)
    }

    // Save the portfolio value in the database
    MongoHelper.updatePrice(portf.portfId,bondsValue.maturity)

    val t1 = System.nanoTime

    Work(portf.portfId,null,Result(portf.portfId,bondsValue.maturity,portf.bonds.size,t0,t1))
  }

  /**
    * Price a simple bond
    * @param bond Bond
    * @return Bond price
    */
  def finePrice(bond: SimpleBond): SimpleBond = {
      val valuator = new SimpleBondValuator(bond, Helper.curveCoeffs)

      val price = valuator.price
      
      new SimpleBond(bond.id,bond.coupon,bond.freq,bond.tenor,price)
  }
  
  /**
   * Parallel load the portfolios with embedded bonds.
   */
  def loadPortfsPar(n: Int): List[Work] = {
    val lotteries = for(i <- 0 to n) yield ran.nextInt(100000)+1 
    
    val list = lotteries.par.foldLeft (List[Work]())
    { (portfIdBonds,portfId) =>
      val intermediate = MongoHelper.fetchBonds(portfId)
      
      Work(portfId,intermediate.bonds,null) :: portfIdBonds
    }
    
    list
  }  

    /**
   * Parallel load the portfolios and bonds into memory (future-based).
   */

  /**
    * Parallel load the portfolios and bonds into memory (future-based).
    * @param n Number of portfolios to retrieve
    * @return Collection of portfolios with bond parameters
    */
  def loadPortfsPar2(n : Int) : ListBuffer[Work] = {
    import scala.concurrent.{Await, Future}
    import scala.concurrent.ExecutionContext.Implicits.global

    val futures: IndexedSeq[Future[PortfIdToBondsMap]] = for(_ <- 1 to n) yield Future {
      // Select a portfolio
      val portfId = ran.nextInt(100000) + 1

      // Fetch its bonds
      MongoHelper.fetchBonds(portfId)
    }

    futures.foldLeft(ListBuffer[Work]()) { (list, future) =>
      import scala.concurrent.duration._
      val result: PortfIdToBondsMap = Await.result(future, 100 seconds)

      list ++ List(Work(result.portfId, result.bonds, null))
    }
  }
}