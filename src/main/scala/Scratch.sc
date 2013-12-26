import java.util.concurrent.Executors
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import scala.language.postfixOps

object Scratch {
  println("Welcome to the Scala worksheet")       //> Welcome to the Scala worksheet

  case class Test(one: String = "one", two: String = "two")
  Test("myone", "mytwo")                          //> res0: Scratch.Test = Test(myone,mytwo)
  Test(two= "mytwo")                              //> res1: Scratch.Test = Test(one,mytwo)
 
  val execService = Executors.newCachedThreadPool()
                                                  //> execService  : java.util.concurrent.ExecutorService = java.util.concurrent.T
                                                  //| hreadPoolExecutor@29f10c35[Running, pool size = 0, active threads = 0, queue
                                                  //| d tasks = 0, completed tasks = 0]
  implicit val execContext = ExecutionContext.fromExecutorService(execService)
                                                  //> execContext  : scala.concurrent.ExecutionContextExecutorService = scala.conc
                                                  //| urrent.impl.ExecutionContextImpl$$anon$1@1caa270d
 
  val future1 = Future {
    (1 to 3).foldLeft(0) { (a, i) =>
      println("Future 1 - "+i);
      Thread.sleep(5)
      a + i
    }
  }                                               //> future1  : scala.concurrent.Future[Int] = scala.concurrent.impl.Promise$Defa
                                                  //| ultPromise@4c59ee42
  val future2 = Future {
    ('A' to 'C').foldLeft("") { (a, c) =>
      println("Future 2 - "+c);
      Thread.sleep(5)
      a + c
    }
  }                                               //> future2  : scala.concurrent.Future[String] = scala.concurrent.impl.Promise$D
                                                  //| efaultPromise@1540de0
 
  val result = for {
    numsum <- future1
    string <- future2
  } yield (numsum, string)                        //> Future 1 - 1
                                                  //| Future 1 - 2
                                                  //| result  : scala.concurrent.Future[(Int, String)] = scala.concurrent.impl.Pro
                                                  //| mise$DefaultPromise@3e7cddcb
  
  Await.result(result, 1 second)                  //> Future 1 - 3
                                                  //| Future 2 - A
                                                  //| Future 2 - B
                                                  //| Future 2 - C
                                                  //| res2: (Int, String) = (6,ABC)-
  
}