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
      import scala.concurrent.duration._
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

  def coarseMapReduce[K, V, K2, V2](
                                     input: List[(K, V)],
                                     mapping: (K, V) => List[(K2, V2)],
                                     reducing: (K2, List[V2]) => List[V2],
                                     numMappers: Int, numReducers: Int): Map[K2, List[V2]] = {

    case class Intermediate(list: List[(K2, V2)])

    case class Reduced(key: K2, values: List[V2])

    val futures: Iterator[Future[List[Intermediate]]] =
      for (group <- input.grouped(input.length / numMappers)) yield Future {
        for ((key, value) <- group) yield
          Intermediate(mapping(key, value))
    }

    val intermediates = futures.foldLeft(List[(K2, V2)]()) { (list, future) =>
      import scala.concurrent.duration._
      val result = Await.result(future, 100 seconds)

      result.foldLeft(list) { (list, intermediate) =>
        list ++ intermediate.list
      }
    }

    var dict = Map[K2, List[V2]]() withDefault (k => List())

    for ((key, value) <- intermediates)
      dict += (key -> (value :: dict(key)))

    for (group <- dict.grouped(dict.size / numReducers))
      actor {
        for ((key, values) <- group)
          master ! Reduced(key, reducing(key, values))
      }

    var result = Map[K2, List[V2]]()

    for (_ <- 1 to dict.size)
      receive {
        case Reduced(key, values) =>
          result += (key -> values)
      }
    result
  }
}
