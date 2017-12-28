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
package scalaparallel.parabond.test

import scalaparallel.parabond.casa.{MongoDbObject, MongoHelper}
import scalaparallel.parabond.util.{Data, Helper, Result}
import scalaparallel.parabond.value.SimpleBondValuator
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
    val arg = System.getProperty("n")
    
    val n = if(arg == null) PORTF_NUM else arg.toInt
    
    val me =  this.getClass().getSimpleName()
    val outFile = me + "-dat.txt"
    
    val fos = new java.io.FileOutputStream(outFile,true)
    val os = new java.io.PrintStream(fos)
    
    os.print(me+" "+ "N: "+n+" ")
    
    val details = if(System.getProperty("details") != null) true else false
    
    // Load all the bonds into into memory
    // Note: the input is a list of Data instances, each element of which contains a list
    // of bonds
    val t2 = System.nanoTime
    val inputs = loadPortfsParFold(n)
    val t3 = System.nanoTime   
    
    // Process the data
    val now = System.nanoTime  
    val outputs = inputs.par.map(priced)     
    val t1 = System.nanoTime
    
    // Generate the detailed output report
    if(details) {
      println("%6s %10.10s %-5s %-2s".format("PortId","Price","Bonds","dt"))
      
      outputs.foreach { output =>
        val id = output.id

        val dt = (output.result.t1 - output.result.t0) / 1000000000.0

        val bondCount = output.result.bondCount

        val price = output.result.price

        println("%6d %10.2f %5d %6.4f %12d %12d".format(id, price, bondCount, dt, output.result.t1 - now, output.result.t0 - now))
      }
    }
    
    val dt1 = outputs.foldLeft(0.0) { (sum,result) =>      
      sum + (result.result.t1 - result.result.t0)
      
    } / 1000000000.0
    
    val dtN = (t1 - now) / 1000000000.0
    
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
  def priced(portf: Data): Data = {
    
    // Value each bond in the portfolio
    val t0 = System.nanoTime
    
    val value = portf.bonds.foldLeft(0.0) { (sum, bond) =>      
      // Price the bond
      val valuator = new SimpleBondValuator(bond, Helper.curveCoeffs)

      val price = valuator.price
      
      // The price into the aggregate sum
      sum + price
    }    
    
    MongoHelper.updatePrice(portf.id,value)     
    
    val t1 = System.nanoTime
    
    // Return the result for this portfolio
    Data(portf.id,null,Result(portf.id,value,portf.bonds.size,t0,t1))
  }  
  
  /**
   * Parallel load the portfolios with embedded bonds.
   * Note: This version does NOT improve performance because fold left
   * is inherently serial
   */
  def loadPortfsParFoldLeft(n: Int): List[Data] = {
    val lotteries = for(i <- 0 to n) yield ran.nextInt(100000)+1 
    
    val list = lotteries.par.foldLeft (List[Data]())
    { (portfIdBonds,portfId) =>
      val intermediate = MongoHelper.fetchBonds(portfId)
      
      Data(portfId,intermediate.list,null) :: portfIdBonds
    }
    
    list
  }  
  
  /**
   * Parallel load the portfolios with embedded bonds.
   */
  def loadPortfsParFold(n: Int): List[Data] = {    
    // Initialize the portfolios to retrieve
    val portfs = for(i <- 0 until n) yield Data(ran.nextInt(100000)+1,null,null) 
    
    val z = List[Data]()
    
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
        case data : Data =>
          val bonds = MongoHelper.fetchBonds(data.id) 
          
          List(Data(data.id,bonds.list,null)) ++ opa
      }         

    }
    
    // Cast the list to a data list
    list match {
      case l : List[_] =>
        l.asInstanceOf[List[Data]]
      case _ =>
        List[Data]()
    }
  }  
  
  
   /**
   * Parallel load n portfolios and their bonds into memory (future-based).
   */
  def loadPortfsParallel(n : Int) : ListBuffer[Data] = {
    val futures = for(_ <- 1 to n) yield Future {
      // Select a portfolio
      val lottery = ran.nextInt(100000) + 1

      // Fetch its bonds
      MongoHelper.fetchBonds(lottery)
    }

    val list = futures.foldLeft(ListBuffer[Data]()) { (list,future) =>
      import scala.concurrent.duration._
      val result = Await.result(future, 100 seconds)

      list ++ List(Data(result.portfId, result.list, null))
    }

    list
  }
}