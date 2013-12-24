object Scratch {;import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(60); 
  println("Welcome to the Scala worksheet")

  case class Test(one: String = "one", two: String = "two");$skip(86); val res$0 = 
  Test("myone", "mytwo");System.out.println("""res0: Scratch.Test = """ + $show(res$0));$skip(21); val res$1 = 
  Test(two= "mytwo");System.out.println("""res1: Scratch.Test = """ + $show(res$1))}
}
