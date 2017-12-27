import scala.concurrent.{Await, Future}

def square(x: Int) = x * x
square(2)
2+2
(0 until 5).foreach(println(_))
(0 until 5)
List(1,2,3,4,5).par
"99".toInt
val p1 = List("1")
"2" :: p1
(1 to 10)
for(i <- 1 to 3) yield "hello"+i
val futures = for(i <- 0 until 5) yield Future { 99 }
Await.result(futures(0), 1)







