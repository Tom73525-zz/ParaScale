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

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * This object contains convenience methods for running mapreduce operations
  * on a portfolio of bonds.
  */
object MapReduce {
  def mapreduceBasic[K, V, K2, V2](
                                    input: List[(K, V)],
                                    mapping: (K, V) => List[(K2, V2)],
                                    reducing: (K2, List[V2]) => List[V2]): Map[K2, List[V2]] = {
    case class Intermediate(list: List[(K2, V2)])

    val futures = for ((key, value) <- input) yield Future {
      Intermediate(mapping(key, value))
    }

    val intermediates = futures.foldLeft(List[(K2, V2)]()) { (list, future) =>
      val result = Await.result(future, 100 seconds)

      list ++ result.list
    }

    var dict = Map[K2, List[V2]]() withDefault (k => List())

    for ((key, value) <- intermediates)
      dict += (key -> (value :: dict(key)))

    var result = Map[K2, List[V2]]()

    for ((key, value) <- dict)
      result += (key -> reducing(key, value))

    result
  }

  /**
    *
    * @param input List of (portfId,
    * @param mapping
    * @param reducing
    * @param numMappers
    * @param numReducers
    * @tparam K
    * @tparam V
    * @tparam K2
    * @tparam V2
    * @return Map from portfolio id to a list of its values (actually only the first value)
    */
  def coarseMapReduce[K, V, K2, V2](
                                     input: List[(K, V)],
                                     mapping: (K, V) => List[(K2, V2)],
                                     reducing: (K2, List[V2]) => List[V2],
                                     numMappers: Int,
                                     numReducers: Int): Map[K2, List[V2]] = {

    case class Intermediate(list: List[(K2, V2)])

    case class Reduced(key: K2, values: List[V2])

    val mapfutures: Iterator[Future[List[Intermediate]]] =
      for (group <- input.grouped(input.length / numMappers)) yield Future {
        for ((key, value) <- group) yield
          Intermediate(mapping(key, value))
      }

    val intermediates = mapfutures.foldLeft(List[(K2, V2)]()) { (list, future) =>
      val result = Await.result(future, 100 seconds)

      result.foldLeft(list) { (list, intermediate) =>
        list ++ intermediate.list
      }
    }

    var dict = Map[K2, List[V2]]() withDefault (k => List())

    for ((key, value) <- intermediates)
      dict += (key -> (value :: dict(key)))

    val reducedfutures: Iterator[Future[Iterable[Reduced]]] =
      for (group <- dict.grouped(dict.size / numReducers)) yield Future {
        for((key, values) <- group) yield
          Reduced(key, reducing(key, values))
      }

    val reduceds = reducedfutures.foldLeft(Map[K2,List[V2]]()) { (map, future) =>
      val result = Await.result(future, 100 seconds)

      result.foldLeft(Map[K2, List[V2]]()) { (map, rsult) =>
        map + (rsult.key -> rsult.values)
      }
    }

    reduceds
  }

//  /**
//    *
//    * @param input List of portfolio ids
//    * @param mapping Maps a portfolio id to a list of (portfolio, result) pairs
//    * @param reducing Reduces a (portolio id, list of bond values) pair to portfolio value as a single result
//    * @param numMappers Number of mappers to use
//    * @param numReducers Number of reduces to use
//    * @tparam K Portfolio identifier
//    * @tparam V Result
//    * @return Map from portfolio id to portfolio value (just one in the list)
//    */
//  def coarseMapReduce2[K, V](
//                                     input: List[K],
//                                     mapping: K => List[(K, V)],
//                                     reducing: (K, List[V]) => List[V],
//                                     numMappers: Int,
//                                     numReducers: Int): Map[K, List[V]] = {
//
//    case class Intermediate(list: List[(K, V)])
//
//    case class Reduced(key: K, values: List[V])
//
//    val mapfutures: Iterator[Future[List[Intermediate]]] =
//      for (group <- input.grouped(input.length / numMappers)) yield Future {
//        for ((key, value) <- group) yield
//          Intermediate(mapping(key, value))
//      }
//
//    val intermediates = mapfutures.foldLeft(List[(K, V)]()) { (list, future) =>
//      val result = Await.result(future, 100 seconds)
//
//      result.foldLeft(list) { (list, intermediate) =>
//        list ++ intermediate.list
//      }
//    }
//
//    var dict = Map[K, List[V]]() withDefault (k => List())
//
//    for ((key, value) <- intermediates)
//      dict += (key -> (value :: dict(key)))
//
//    val reducedfutures: Iterator[Future[Iterable[Reduced]]] =
//      for (group <- dict.grouped(dict.size / numReducers)) yield Future {
//        for((key, values) <- group) yield
//          Reduced(key, reducing(key, values))
//      }
//
//    val reduceds = reducedfutures.foldLeft(Map[K,List[V]]()) { (map, future) =>
//      val result = Await.result(future, 100 seconds)
//
//      for((k, v) <- result) {
//        map(k) = v
//      }
//
//      map
//    }
//
//    reduceds
//  }

}


