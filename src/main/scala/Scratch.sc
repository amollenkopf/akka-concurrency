object Scratch {
  println("Welcome to the Scala worksheet")       //> Welcome to the Scala worksheet

  case class Test(one: String = "one", two: String = "two")
  Test("myone", "mytwo")                          //> res0: Scratch.Test = Test(myone,mytwo)
  Test(two= "mytwo")                              //> res1: Scratch.Test = Test(one,mytwo)
}