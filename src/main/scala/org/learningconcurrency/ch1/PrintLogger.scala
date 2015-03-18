package org.learningconcurrency.ch1

trait Logging {
  val warnPrefix = "WARN: "
  val errorPrefix = "ERROR: "

  def log(s: String): Unit
  def warn(s: String) = log(warnPrefix + s)
  def error(s: String) = log(errorPrefix + s)
}

class PrintLogger extends Logging {
  def log(s: String) = println(s)
}