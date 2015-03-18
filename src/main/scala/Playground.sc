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