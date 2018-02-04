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
package parabond.cluster

import org.apache.log4j.Logger
import parabond.cluster.MemoryBoundDrone.LOG
import parabond.mr.PORTF_NUM
import parascale.parabond.casa.{MongoDbObject, MongoHelper}
import parascale.parabond.casa.MongoHelper.{PortfIdToBondsMap, bondCollection, mongo}
import parascale.parabond.entry.SimpleBond
import parascale.parabond.util.{Task, Helper, Result}
import parascale.parabond.util.Constant.NUM_PORTFOLIOS
import parascale.parabond.value.SimpleBondValuator
import parascale.util.getPropertyOrElse
import scala.collection.GenSeq
import scala.util.Random

object MemoryBoundDrone extends App {
  val LOG = Logger.getLogger(getClass)

  val analysis = new MemoryBoundDrone analyze

  report(LOG, analysis)
}

/**
  * Loads all the bonds of all the portfolios into memory then prices each portfolio per core.
  */
class MemoryBoundDrone extends Drone {
  def analyze: Analysis = {
    // Clock in
    val t0 = System.nanoTime

    // Seed must be same for ever host in cluster as this establishes
    // the randomized portfolio sequence
    val seed = getPropertyOrElse("seed",0)
    Random.setSeed(seed)

    // Number of portfolios to analyze
    val n = getPropertyOrElse("n",PORTF_NUM)

    // Start and end (inclusive) in analysis sequence
    val beginIndex = getPropertyOrElse("begin", 0)
    val endIndex = beginIndex + n

    // Size of the database
    val size = getPropertyOrElse("size", NUM_PORTFOLIOS)

    // Shuffled deck of portfolios
    val deck = Random.shuffle(0 to size-1)

    // Indices in the deck we're working on
    // Note: k+1 since portf ids are 1-based
    val indices = for(k <- beginIndex to endIndex) yield Task(deck(k) + 1)

    // Get the proper collection depending on whether we're measuring T1 or TN
    val tasks = if(getPropertyOrElse("par",true)) loadPortfsParallel(indices).par else loadPortfsSequential(indices)

    // Run the analysis
    val results = tasks.map(price)

    // Clock out
    val t1 = System.nanoTime

    Analysis(results, t0, t1)
  }

  def price(task: Task): Task = {
    // Value each bond in the portfolio
    val t0 = System.nanoTime

    // We already have to bonds in memory.
    val value = task.bonds.foldLeft(0.0) { (sum, bond) =>
      // Price the bond
      val valuator = new SimpleBondValuator(bond, Helper.curveCoeffs)

      val price = valuator.price

      // Updated portfolio price so far
      sum + price
    }

    MongoHelper.updatePrice(task.portfId,value)

    val t1 = System.nanoTime

    // Return the result for this portfolio
    Task(task.portfId,null,Result(task.portfId,value,task.bonds.size,t0,t1))
  }

  /**
    * Loads portfolios using and their bonds into memory serially.
    */
  def loadPortfsSequential(tasks: Seq[Task]) : Seq[Task] = {
    // Connect to the portfolio collection
    val portfsCollecton = mongo("Portfolios")

    val portfIdToBondsPairs = tasks.foldLeft(List[Task] ()) { (list, input) =>
      // Select a portfolio
      val portfId = input.portfId

      // Retrieve the portfolio
      val portfsQuery = MongoDbObject("id" -> portfId)

      val portfsCursor = portfsCollecton.find(portfsQuery)

      // Get the bonds in the portfolio
      val bondIds = MongoHelper.asList(portfsCursor, "instruments")

      val bonds = bondIds.foldLeft(List[SimpleBond]()) { (bonds, id) =>
        // Get the bond from the bond collection
        val bondQuery = MongoDbObject("id" -> id)

        val bondCursor = bondCollection.find(bondQuery)

        val bond = MongoHelper.asBond(bondCursor)

        // The price into the aggregate sum
        bonds ++ List(bond)
      }

      Task(portfId,bonds,null) :: list
    }

    portfIdToBondsPairs
  }

  /**
    * Parallel loads portfolios using and their bonds into memory using futures.
    */
  def loadPortfsParallel(tasks: GenSeq[Task]) : List[Task] = {
    import scala.concurrent.{Await, Future}
    import scala.concurrent.ExecutionContext.Implicits.global

    val futures = for(input <- tasks) yield Future {
      // Select a portfolio
      val portfId = input.portfId

      // Fetch its bonds
      MongoHelper.fetchBonds(portfId)
    }

    val list = futures.foldLeft(List[Task]()) { (list, future) =>
      import scala.concurrent.duration._
      import parascale.parabond.util.Constant._
      val result = Await.result(future, MAX_WAIT_TIME seconds)

      // Use null because we don't have result yet -- completed when we analyze the portfolio
      Task(result.portfId, result.bonds, null) :: list
    }

    list
  }
}
