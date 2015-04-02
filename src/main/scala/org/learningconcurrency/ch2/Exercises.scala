package org.learningconcurrency
package ch2

import org.learningconcurrency.ch2.SynchronizedProtectedUid

object Exercises extends App {
  /** Takes two computation blocks and starts each of them in a new thread.
    *
    * @return a tuple with the result values of both computations
    */
  def parallel[A, B](a: =>A, b: =>B): (A, B) = {
    val t1 = new Thread {
      var result :Option[A] = None
      override def run() { result = Some(a) }
    }
    val t2 = new Thread {
      var result :Option[B] = None
      override def run() { result = Some(b) }
    }
    t1.start(); t2.start()
    t1.join(); t2.join()
    (t1.result.get, t2.result.get)
  }

  /** Starts a thread that executes a computation block every so often.
    *
    * @param duration number of milliseconds between executions
    * @param b the block of code to execute every `duration` milliseconds
    */
  def periodically(duration: Long, b: =>Unit): Unit = {
    val t1 = new Thread {
      import scala.annotation.tailrec
      override def run() {
        Thread.sleep(duration)
        b
        run()
      }
    }
    t1.start()
  }

  /** Used to exchange values between two or more threads.
    *
    * When created, the object is empty. After a value is added, it is
    * non-empty.
    *
    * @tparam T the type of variable stored
    */
  class SyncVar[T] {
    var value: Option[T] = None

    /** If non-empty, returns the current value and changes state to empty;
      * throws an exception otherwise.
      */
    def get(): T = value match {
      case Some(x) => value = None; x
      case None => throw new IllegalStateException("Cannot get before put.")
    }

    /** If empty, adds a value to this object and changes state to non-empty;
      * throws an exception otherwise.
      */
    def put(x: T): Unit = {
      value match {
        case Some(value) => throw new IllegalStateException("Cannot put after put.")
        case None => value = Some(x)
      }
    }

    def isEmpty(): Boolean = value == None

    def nonEmpty(): Boolean = value != None

    /** Waits until this object is non-empty, then returns the current value and
      * changes state to empty.
      */
    def getWait(): T = ???

    /** Waits until this object is empty, then adds a value to this object and
      * changes state to non-empty.
      */
    def putWait(x: T): Unit = ???
  }

  /** Like `SyncVar` but can hold a number of values specified in the
    * constructor.
    *
    * @tparam T the type of variable stored
    */
  class SyncQueue[T](n: Int) {
    def get(): T = ???
    def put(x: T): Unit = ???
    def isEmpty(): Boolean = ???
    def nonEmpty(): Boolean = ???
    def getWait(): T = ???
    def putWait(x: T): Unit = ???
  }

  class Account(val id: Int, var money: Int)

  /** Transfers all the money from `accounts` to `target`. Ensure deadlock
    * cannot occur.
    */
  def sendAll(accounts: Set[Account], target: Account): Unit = ???

  /** A single worker thread picks tasks submitted to the pool and executes them.
    * Whenever the worker thread picks a new task from the pool for execution,
    * that task must have the highest priority in the pool.
    */
  class PriorityTaskPool {

    /** Add a `task` with a given `priority`to the pool, to be executed by a
      * worker thread.
      */
    def asynchronous(priority: Int)(task: =>Unit): Unit = ???
  }

  /** Supports `p` worker threads */
  class MultithreadedPriorityTaskPool(val p: Int) extends PriorityTaskPool {
    override def asynchronous(priority: Int)(task: => Unit): Unit = ???
  }

  class AbortablePriorityTaskPool(override val p: Int, val important: Int) extends MultithreadedPriorityTaskPool(p) {

    /** When this method is called, all tasks with priorities greater than
      * `important` must be completed, and the rest of the tasks discarded.
      */
    def shutdown(): Unit = ???
  }

  //println(parallel[Integer,Integer]({ Thread.sleep(2000); 2 + 3 }, { Thread.sleep(5000); 3 + 5 }))
  //periodically(2000, { println("...") })
  val myvar = new SyncVar[Integer]()
  println(myvar.isEmpty())
  println(myvar.nonEmpty())
  myvar.put(1)
  println(myvar.isEmpty())
  println(myvar.nonEmpty())
  println(myvar.get())
}
