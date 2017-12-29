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
package parascale.parabond.mr

import parascale.parabond.entry.SimpleBond
import parascale.parabond.util.Result

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * This object contains convenience methods for running mapreduce operations
  * on a portfolio of bonds.
  */
object MapReduce {
  /**
    *
    * @param input Portfolio ids
    * @param mapping Mapping function
    * @param reducing Reducing function
    * @return Mapping from portfolio id to result list (only index zero has value)
    */
  def basic (
                                    input: List[Int],
                                    mapping: Int => List[(Int, Result)],
                                    reducing: (Int, List[Result]) => List[Result]): Map[Int, List[Result]] = {
    case class Intermediate(list: List[(Int, Result)])

    // Value the portfolios in parallel
    val futures = for (portfid <- input) yield Future {
      Intermediate(mapping(portfid))
    }

    // Wait for the portfolios to finish and gather the intermediate results of (portf id, value) pairs
    // NOTE: mapping may already have reduced the bond prices to a single value.
    val intermediates = futures.foldLeft(List[(Int, Result)]()) { (list, future) =>
      val result = Await.result(future, 100 seconds)

      list ++ result.list
    }

    // Copy intermediate results to a dictionary: portf -> intermediate results
    var dict = Map[Int, List[Result]]() withDefault (k => List())

    for ((key, value) <- intermediates)
      dict += (key -> (value :: dict(key)))

    // Reduce the prices to a single price: portfid -> result
    var result = Map[Int, List[Result]]()

    for ((key, value) <- dict)
      result += (key -> reducing(key, value))

    result
  }

  /**
    * Values the portfolios in groups, ie, a single future values multiple portfs instead of a single portf.
    * @param input List of portfolio ids
    * @param mapping Mapping function
    * @param reducing Reducing function
    * @param numMappers Number of mappers
    * @param numReducers Number of reducers
    * @return Map from portfolio id to a list of its values (actually only the first value)
    */
  def coarse (
                                     input: List[Int],
                                     mapping: Int => List[(Int, Result)],
                                     reducing: (Int, List[Result]) => List[Result],
                                     numMappers: Int,
                                     numReducers: Int): Map[Int, List[Result]] = {

    case class Intermediate(list: List[(Int, Result)])
    case class Reduced(portfid: Int, values: List[Result])

    // Value a group of portfolios within a single future in parallel
    val mapfutures: Iterator[Future[List[Intermediate]]] =
      for (group <- input.grouped(input.length / numMappers)) yield Future {
        for (portfid <- group) yield
          Intermediate(mapping(portfid))
      }

    // Wait for the futures to finish
    val intermediates = mapfutures.foldLeft(List[(Int, Result)]()) { (list, future) =>
      val result = Await.result(future, 100 seconds)

      result.foldLeft(list) { (list, intermediate) =>
        list ++ intermediate.list
      }
    }

    // Copy the result to a dictionary: portfid -> intermediate results
    var dict = Map[Int, List[Result]]() withDefault (k => List())

    for ((key, value) <- intermediates)
      dict += (key -> (value :: dict(key)))

    // Reduce the results in parallel
    val reducedfutures: Iterator[Future[Iterable[Reduced]]] =
      for (group <- dict.grouped(dict.size / numReducers)) yield Future {
        for((portfid, results) <- group) yield
          Reduced(portfid, reducing(portfid, results))
      }

    // Wait for the reduction to finish and build the map: portfid -> results
    val reduceds = reducedfutures.foldLeft(Map[Int,List[Result]]()) { (map, future) =>
      val result = Await.result(future, 100 seconds)

      result.foldLeft(Map[Int, List[Result]]()) { (map, rsult) =>
        map + (rsult.portfid -> rsult.values)
      }
    }

    reduceds
  }

  /**
    * Values the portfolios given all the portfolios and bonds have been loaded into memory.
    * @param input List of pairs of portfolio ids and bonds (not bond ids)
    * @param mapping Mapping function
    * @param reducing Reducing function
    * @return Mapping from portfid -> value
    */
  def memorybound (input: List[(Int, List[SimpleBond])],
                                  mapping: (Int,List[SimpleBond]) => List[(Int,Result)],
                                  reducing: (Int,List[Result]) => List[Result]): Map[Int, List[Result]] = {
    case class Intermediate(list: List[(Int, Result)])
    case class Reduced(key: Int, values: List[Result])

    val futures = for((portf, bonds) <- input) yield Future {
        Intermediate(mapping(portf,bonds))
    }

    val intermediates = futures.foldLeft(List[(Int, Result)]()) { (list, future) =>
      val result = Await.result(future, 100 seconds)

      list ++ result.list
    }

    var dict = Map[Int, List[Result]]() withDefault (k => List())

    for ((portfid, results) <- intermediates)
      dict += (portfid -> (results :: dict(portfid)))

    var result = Map[Int, List[Result]]()

    for ((key, value) <- dict)
      result += (key -> reducing(key, value))

    result
  }
}


