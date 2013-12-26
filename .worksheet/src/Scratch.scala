import java.util.concurrent.Executors
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import scala.language.postfixOps

object Scratch {;import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(227); 
  println("Welcome to the Scala worksheet")

  case class Test(one: String = "one", two: String = "two");$skip(86); val res$0 = 
  Test("myone", "mytwo");System.out.println("""res0: Scratch.Test = """ + $show(res$0));$skip(21); val res$1 = 
  Test(two= "mytwo");System.out.println("""res1: Scratch.Test = """ + $show(res$1));$skip(54); 
 
  val execService = Executors.newCachedThreadPool();System.out.println("""execService  : java.util.concurrent.ExecutorService = """ + $show(execService ));$skip(79); 
  implicit val execContext = ExecutionContext.fromExecutorService(execService);System.out.println("""execContext  : scala.concurrent.ExecutionContextExecutorService = """ + $show(execContext ));$skip(140); 
 
  val future1 = Future {
    (1 to 3).foldLeft(0) { (a, i) =>
      println("Future 1 - "+i);
      Thread.sleep(5)
      a + i
    }
  };System.out.println("""future1  : scala.concurrent.Future[Int] = """ + $show(future1 ));$skip(143); 
  val future2 = Future {
    ('A' to 'C').foldLeft("") { (a, c) =>
      println("Future 2 - "+c);
      Thread.sleep(5)
      a + c
    }
  };System.out.println("""future2  : scala.concurrent.Future[String] = """ + $show(future2 ));$skip(94); 
 
  val result = for {
    numsum <- future1
    string <- future2
  } yield (numsum, string);System.out.println("""result  : scala.concurrent.Future[(Int, String)] = """ + $show(result ));$skip(36); val res$2 = 
  
  Await.result(result, 1 second);System.out.println("""res2: (Int, String) = """ + $show(res$2))}
  
}
