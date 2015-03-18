import org.learningconcurrency.ch1.Printer
import org.learningconcurrency.ch1.PrintLogger
import org.learningconcurrency.ch1.TwoThings


val printer = new Printer("Hello")
printer.printMessage()
printer.printNumber(23)
printer.printNumber(42)

val logger = new PrintLogger()
logger.warn("Hello, world!")

val things = new TwoThings("a", 32)
things.first
things.second


// Anonymous functions
val twice = (x: Int) => x * 2
val twice2: Int => Int = x => x * 2
val twice3: Int => Int = _ * 2

twice(21)


// By-name arguments are evaluated later
// as if passed-in value were inside a closure
def notRunTwice(body: Unit) = {
  body
  body
}
def runTwice(body: =>Unit) = {
  body
  body
}

// For expressions
for (i <- 0 until 3) println(i)

for (i <- 0 until 3) yield -i

for (x <- 0 until 3; y <- 0 until 3) yield (x, y)


// String interpolation
val magic = 7
val myMagicNumber = s"My magic number is $magic"


// Pattern matching
val successors = Map(1 -> 2, 2 -> 3, 3 -> 4)
val handleResult = (res: Option[Int]) => {
  res match {
    case Some(n) => println(s"Successor found: $n")
    case None => println("No successor found!")
  }
}
handleResult(successors.get(2))
handleResult(successors.get(5))
