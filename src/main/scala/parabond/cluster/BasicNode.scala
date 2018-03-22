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
import parascale.parabond.casa.{MongoDbObject, MongoHelper}
import parascale.parabond.util.{Work, Helper, Result}
import parascale.parabond.util.Constant.{NUM_PORTFOLIOS, PORTF_NUM}
import parascale.parabond.value.SimpleBondValuator
import parascale.util.getPropertyOrElse
import scala.util.Random

object BasicNode extends App {
  val LOG = Logger.getLogger(getClass)

  val analysis = new BasicNode analyze

  report(LOG, analysis)
}

/**
  * Prices one portfolio per core using the basic or "naive" algorithm.
  */
class BasicNode extends Node {
  def analyze: Analysis = {
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
    val indices = for(k <- begin to end) yield Work(deck(k) + 1)

    // Get the proper collection depending on whether we're measuring T1 or TN
    val tasks = if(getPropertyOrElse("par", true)) indices.par else indices

    // Run the analysis
    val results = tasks.map(price)

    // Clock out
    val t1 = System.nanoTime

    Analysis(results, t0, t1)
  }

  /**
    * Prices a portfolio using the "naive" algorithm.
    * It makes two requests of the database:<p>
    * 1) fetch the portfolio<p>
    * 2) fetch bonds in that portfolio.<p>
    * After the second fetch the bond is then valued and added to the portfoio value
    */
  def price(work: Work): Work = {
    // Value each bond in the portfolio
    val t0 = System.nanoTime

    // Retrieve the portfolio
    val portfId = work.portfId

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
      val valuator = new SimpleBondValuator(bond, Helper.yieldCurve)

      val price = valuator.price

      // Update portfolio price
      sum + price
    }

    // Update the portfolio price in the database
    MongoHelper.updatePrice(portfId,value)

    val t1 = System.nanoTime

    Work(portfId,work.bonds,Result(portfId,value,bondIds.size,t0,t1))
  }
}
