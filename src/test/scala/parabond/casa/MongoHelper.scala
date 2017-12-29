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
package parascale.parabond.casa

import java.util.logging.{Level, Logger}

import com.mongodb.client.FindIterable
import org.bson.Document

import scala.util.Random
import parascale.parabond.util.{Constant, Data}
import parascale.parabond.entry.SimpleBond
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * This object implements monngo-specific helper functions.
 * @author Ron Coleman
 */
object MongoHelper {
  /** Sets the mongo host */
  val host: String = getHost

  /** Connects to the parabond DB */
  val mongo = parascale.parabond.casa.MongoConnection(host)("parabond")
  
  val portfCollection = mongo("Portfolios")
  val bondCollection = mongo("Bonds")
  
  /** Initialize the random number generator */
  val ran = new Random(0)
  
  case class Intermediate(portfId: Int, list : List[SimpleBond])  
  case class Intermediate2(bonds: List[SimpleBond])
  
  /**
   * Loads a list of 2-tuples of portfolios x list of bonds
   */
  def loadPortfs(n : Int) : List[(Int,List[SimpleBond])] = {
    // Connect to the portfolio collection
    val portfsCollecton = mongo("Portfolios")
    
    val input = (1 to n).foldLeft(List[(Int,List[SimpleBond])] ()) { (list, pid) =>
      // Select a portfolio
      val lottery = ran.nextInt(100000) + 1      

      // Retrieve the portfolio 
      val portfsQuery = MongoDbObject("id" -> lottery)
      
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
    
      list ++ List((lottery,bonds))
    }
    
    input
  }

  /**
   * Parallel load the portfolios and bonds into memory (actor-based).
   */
  def loadPortfsParallel(n : Int) : List[(Int,List[SimpleBond])] = {

    val futures = for(_ <- 1 to n) yield Future {
      // Select a portfolio
      val lottery = ran.nextInt(Constant.NUM_PORTFOLIOS) + 1

      // Fetch its bonds
      fetchBonds(lottery)
    }

    val list = futures.foldLeft(List[(Int, List[SimpleBond])]()) { (list,future) =>
      import scala.concurrent.duration._
      val result = Await.result(future, 100 seconds)

      list ++ List((result.portfId,result.list))
    }

    list
  }
  
  /**
   * Loads portfolios x bonds into memory
   */
  def loadPortfsPar(n: Int): List[(Int,List[SimpleBond])] = {  
    val lotteries = for(i <- 0 to n) yield ran.nextInt(100000)+1 
    
    val list = lotteries.par.foldLeft (List[(Int,List[SimpleBond])]())
    { (portfIdBonds,portfId) =>
      val intermediate = fetchBonds(portfId)
      
      (portfId,intermediate.list) :: portfIdBonds
    }
    
    list
  }  
  
  /** Converts mongo cursor to scala list of int objects */
  def asList(results: FindIterable[Document], field: String): List[Int] = {
    val cursor = results.iterator
    
    if (cursor.hasNext) {
      val value = cursor.next().get(field)

      value match {
        case list: java.util.List[_] =>
          import scala.collection.JavaConverters._
          list.asInstanceOf[java.util.List[Int]].asScala.toList

        case _ =>
          scala.List[Int]()

      }
    }
    else
      List[Int]()
  }
  
  /**
   * Converts the mongo cursor to a bond -- assuming the query cursor
   * as a single bond
   */
  def asBond(results: FindIterable[Document]) : SimpleBond = {
    val cursor = results.iterator
    
    if(cursor.hasNext) {
      val bondParams = cursor.next()
      
      val id = bondParams.get("id").toString.toInt
      
      val coupon = bondParams.get("coupon").toString.toDouble
      
      val freq = bondParams.get("freq").toString.toInt
      
      val tenor = bondParams.get("tenor").toString.toDouble
      
      val maturity= bondParams.get("maturity").toString.toDouble
      
      SimpleBond(id,coupon,freq,tenor,maturity)
    }
    else
      SimpleBond()
  }
  
  /**
    * Fetches the bonds from the database.
    * @param portfId Portfolio id
    * @return Container of portfolio id and bonds
    */
  def fetchBonds(portfId: Int): Intermediate = {
      // Retrieve the portfolio 
      val portfsQuery = MongoDbObject("id" -> portfId)
      
      val portfsCursor = portfCollection.find(portfsQuery)
      
      // Get the bonds in the portfolio
      val bondIds = MongoHelper.asList(portfsCursor, "instruments")

      // Connect to the bonds collection
      val bondsCollection = mongo("Bonds")

      val bonds = bondIds.foldLeft(List[SimpleBond]()) { (bonds, id) =>
        // Get the bond from the bond collection
        val bondQuery = MongoDbObject("id" -> id)

        val bondCursor = bondsCollection.find(bondQuery)

        val bond = MongoHelper.asBond(bondCursor)

        // The price into the aggregate sum
        bonds ++ List(bond)
      }
      
      // Method below runs out of semaphores on mongo
//      val bonds = fetchBondsParallel(bondIds,bondsCollection)
      
      Intermediate(portfId,bonds)
  }
  
  def updatePrice(portfId: Int, price: Double): Unit = {
    val portfs = mongo("Portfolios")
    
    val portf = MongoDbObject("id" -> portfId)
    
    val newPrice = MongoDbObject("$set" -> MongoDbObject("price" -> price))
    
    portfs.updateOne(portf, newPrice)
        
  }
  
    /**
   * Gets the mongo host
   * */
  def getHost : String = {
    val host = System.getProperty("host")
    
    if(host != null) host else "127.0.0.1"
  }
}