package scalaparallel.parabond.mr

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

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

    val master = self

    for (group <- input.grouped(input.length / numMappers))
      actor {
        for ((key, value) <- group)
          master ! Intermediate(mapping(key, value))
      }

    var intermediates = List[(K2, V2)]()

    for (_ <- 1 to input.length)
      receive {
        case Intermediate(list) => intermediates :::= list
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
