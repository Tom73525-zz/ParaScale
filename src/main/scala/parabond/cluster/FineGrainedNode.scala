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
import parascale.parabond.entry.SimpleBond
import parascale.parabond.util.Constant.{NUM_PORTFOLIOS, PORTF_NUM}
import parascale.parabond.util.{Helper, Job, Result}
import parascale.parabond.value.SimpleBondValuator
import parascale.util.getPropertyOrElse

/**
  * Runs a fine grain node which retrieves the portfolios in random order and prices the bonds as
  * a parallel collection.
  */
object FineGrainedNode extends App {
  val LOG = Logger.getLogger(getClass)

  val seed = getPropertyOrElse("seed",0)
  val size = getPropertyOrElse("size", NUM_PORTFOLIOS)
  val n = getPropertyOrElse("n", PORTF_NUM)
  val begin = getPropertyOrElse("begin", 0)

  val analysis = new FineGrainedNode analyze(Partition(seed=seed, n=n, begin=begin))

  report(LOG, analysis)
}

/**
  * Prices one bond per core then rolls these prices into the portfolio price.
  */
class FineGrainedNode extends BasicNode {
  /**
    * Price a portfolio
    * @param work Portfolio ids
    * @return Valuation
    */
  override def price(work: Job): Job = {
    // Value each bond in the portfolio
    val t0 = System.nanoTime

    // Retrieve the portfolio
    val portfId = work.portfId

    val portfsQuery = MongoDbObject("id" -> portfId)

    val portfsCursor = MongoHelper.portfolioCollection.find(portfsQuery)

    // Get the bonds in the portfolio
    val bids = MongoHelper.asList(portfsCursor,"instruments")

    val bondIds = for(i <- 0 until bids.size) yield Job(bids(i),null,null)

    val output = bondIds.par.map { bondId =>
      // Get the bond from the bond collection
      val bondQuery = MongoDbObject("id" -> bondId.portfId)

      val bondCursor = MongoHelper.bondCollection.find(bondQuery)

      val bond = MongoHelper.asBond(bondCursor)

      val valuator = new SimpleBondValuator(bond, Helper.yieldCurve)

      val price = valuator.price

      new SimpleBond(bond.id,bond.coupon,bond.freq,bond.tenor,price)
    }.reduce(sum)

    MongoHelper.updatePrice(work.portfId,output.maturity)

    val t1 = System.nanoTime

    Job(work.portfId, null, Result(work.portfId, output.maturity, bondIds.size, t0, t1))
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
