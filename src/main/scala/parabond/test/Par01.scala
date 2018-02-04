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
import parascale.parabond.util.{Task, Helper, Result}
import parascale.parabond.value.SimpleBondValuator
import parascale.util.{getPropertyOrElse, parseBoolean}

import scala.collection.mutable.ListBuffer
import scala.util.Random
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/** Test driver */
object Par01 {
  def main(args: Array[String]): Unit = {
    new Par01 test
  }
}

/**
 * This class uses parallel collections to price n portfolios in the
 * parabond database using the memory-bound "naive" algorithm.
 * @author Ron Coleman
 */
class Par01 {
  /** Number of bond portfolios to analyze */
  val PORTF_NUM = 100
  
  /** Initialize the random number generator */
  val ran = new Random(0)   
  
  /** Write a detailed report */
  val details = false
  
  def test {
    // Set the number of portfolios to analyze
    val n = getPropertyOrElse("n",PORTF_NUM)

    val me =  this.getClass().getSimpleName()
    val outFile = me + "-dat.txt"
    
    val fos = new java.io.FileOutputStream(outFile,true)
    val os = new java.io.PrintStream(fos)
    
    os.print(me+" "+ "N: "+n+" ")

    val details = getPropertyOrElse("details",parseBoolean,false)
    
    // Load all the bonds into into memory
    // Note: the input is a list of Data instances, each element of which contains a list
    // of bonds
    val t2 = System.nanoTime
    val inputs = loadPortfsParFold(n)
    val t3 = System.nanoTime   
    
    // Process the data
    val t0 = System.nanoTime
    val results = inputs.par.map(price)
    val t1 = System.nanoTime

    // Generate the detailed output report
    if(details) {
      println("%6s %10.10s %-5s %-2s".format("PortId","Price","Bonds","dt"))

      results.foreach { output =>
        val id = output.portfId

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

    os.print("dt(1): %7.4f  dt(N): %7.4f  cores: %d  R: %5.2f  e: %5.2f ".
        format(dt1,dtN,numCores,speedup,e))

    os.println("load t: %8.4f ".format((t3-t2)/1000000000.0))

    os.flush

    os.close

    println(me+" DONE! %d %7.4f".format(n,dtN))
  }

  /**
   * Prices a portfolio assuming all the bonds for a portfolio are already loaded
   * into memory.
   */
  def price(portf: Task): Task = {
    
    // Value each bond in the portfolio
    val t0 = System.nanoTime
    
    val value = portf.bonds.foldLeft(0.0) { (sum, bond) =>      
      // Price the bond
      val valuator = new SimpleBondValuator(bond, Helper.curveCoeffs)

      val price = valuator.price
      
      // The price into the aggregate sum
      sum + price
    }    
    
    MongoHelper.updatePrice(portf.portfId,value)
    
    val t1 = System.nanoTime
    
    // Return the result for this portfolio
    Task(portf.portfId,null,Result(portf.portfId,value,portf.bonds.size,t0,t1))
  }  
  
  /**
   * Parallel load the portfolios with embedded bonds.
   * Note: This version does NOT improve performance because fold left
   * is inherently serial
   */
  def loadPortfsParFoldLeft(n: Int): List[Task] = {
    val lotteries = for(i <- 0 to n) yield ran.nextInt(100000)+1 
    
    val list = lotteries.par.foldLeft (List[Task]())
    { (portfIdBonds,portfId) =>
      val intermediate = MongoHelper.fetchBonds(portfId)
      
      Task(portfId,intermediate.bonds,null) :: portfIdBonds
    }
    
    list
  }  
  
  /**
   * Parallel load the portfolios with embedded bonds.
   */
  def loadPortfsParFold(n: Int): List[Task] = {
    // Initialize the portfolios to retrieve
    val portfs = for(i <- 0 until n) yield Task(ran.nextInt(100000)+1,null,null)
    
    val z = List[Task]()
    
    // Load the data into memory in parallel
    val list = portfs.par.fold(z) { (a,b) =>
      // Make a into a list -- this is the way "casts" work in Scala
      // Initially a = z
      val opa = a match {
        case y : List[_] =>
          y
      }
      
      b match {
        // If b is a list, append the a and b lists
        case opb : List[_] =>
          opb ++ opa
        
        // If b is a Data instance, fetch the bonds and append them to the data list
        case data : Task =>
          val bonds = MongoHelper.fetchBonds(data.portfId)
          
          List(Task(data.portfId,bonds.bonds,null)) ++ opa
      }         

    }
    
    // Cast the list to a data list
    list match {
      case l : List[_] =>
        l.asInstanceOf[List[Task]]
      case _ =>
        List[Task]()
    }
  }  
  
  
   /**
   * Parallel load n portfolios and their bonds into memory (future-based).
   */
  def loadPortfsParallel(n : Int) : ListBuffer[Task] = {
    val futures = for(_ <- 1 to n) yield Future {
      // Select a portfolio
      val lottery = ran.nextInt(100000) + 1

      // Fetch its bonds
      MongoHelper.fetchBonds(lottery)
    }

    val list = futures.foldLeft(ListBuffer[Task]()) { (list, future) =>
      import scala.concurrent.duration._
      val result = Await.result(future, 100 seconds)

      list ++ List(Task(result.portfId, result.bonds, null))
    }

    list
  }
}