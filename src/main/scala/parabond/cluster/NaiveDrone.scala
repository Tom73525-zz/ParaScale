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
import parabond.mr.PORTF_NUM
import parascale.parabond.casa.{MongoDbObject, MongoHelper}
import parascale.parabond.util.{Data, Helper, Result}
import parascale.parabond.util.Constant.NUM_PORTFOLIOS
import parascale.parabond.value.SimpleBondValuator
import parascale.util.{getPropertyOrElse}
import scala.collection.GenSeq
import scala.util.Random

object NaiveDrone {
  // Connect to the logger
  val LOG = Logger.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    val (results, t0, t1) = test

    val dt = (t1 - t0) seconds

    LOG.info("duration: "+dt)
  }

  def test: Tuple3[GenSeq[Data], Long, Long] = {
    // Clock in
    val t0 = System.nanoTime

    // Seed must be same for ever host in cluster as this establishes
    // the randomized portfolio sequence
    val seed = getPropertyOrElse("seed",0)
    Random.setSeed(seed)

    // Size of database
    val size  = getPropertyOrElse("size", NUM_PORTFOLIOS)

    // Shuffled deck of portfolios
    val deck = Random.shuffle(0 to size-1)

    // Number of portfolios to analyze
    val n = getPropertyOrElse("n", PORTF_NUM)

    // Start and end (inclusive) indices in analysis sequence
    val begin = getPropertyOrElse("begin", 0)
    val end = begin + n

    // The jobs working on, k+1 since portf ids are 1-based
    val jobs = for(k <- begin to end) yield Data(deck(k) + 1)

    // Get the proper collection depending on whether we're measuring T1 or TN
    val inputs = if(getPropertyOrElse("par", true)) jobs.par else jobs

    // Run the analysis
    val results = inputs.map(new NaiveDrone price)

    // Clock out
    val t1 = System.nanoTime

    report(LOG, results, t0, t1)

    (results, t0, t1)
  }
}

class NaiveDrone extends Drone {
  /**
    * Prices a portfolio using the "basic" algorithm.
    */
  override def price(job: Data): Data = {
    // Value each bond in the portfolio
    val t0 = System.nanoTime

    // Retrieve the portfolio
    val portfId = job.portfId

    val portfsQuery = MongoDbObject("id" -> portfId)

    val portfsCursor = MongoHelper.portfolioCollection.find(portfsQuery)

    // Get the bonds ids in the portfolio
    val bondIds = MongoHelper.asList(portfsCursor,"instruments")

    // Price each bond and sum all the prices
    val value = bondIds.foldLeft(0.0) { (sum, id) =>
      // Get the bond from the bond collection by its key id
      val bondQuery = MongoDbObject("id" -> id)

      val bondCursor = MongoHelper.bondCollection.find(bondQuery)

      val bond = MongoHelper.asBond(bondCursor)

      // Price the bond
      val valuator = new SimpleBondValuator(bond, Helper.curveCoeffs)

      val price = valuator.price

      // The price into the aggregate sum
      sum + price
    }

    // Update the portfolio price
    MongoHelper.updatePrice(portfId,value)

    val t1 = System.nanoTime

    Data(portfId,job.bonds,Result(portfId,value,bondIds.size,t0,t1))
  }
}
