package org.learningconcurrency.ch2

object Exercises extends App {
  def parallel[A, B](a: =>A, b: =>B): (A, B) = {
    val threadA = new Thread() {
      var result: Option[A] = None
      override def run(): Unit = { result = Some(a) }
    }
    val threadB = new Thread() {
      var result: Option[B] = None
      override def run(): Unit = { result = Some(b) }
    }
    threadA.start(); threadB.start()
    threadA.join(); threadB.join()
    (threadA.result.get, threadB.result.get)
  }

  def periodically(duration: Long)(b: =>Unit) = {
    val thread = new Thread {
      override def run(): Unit = while (true) {
        b
        Thread.sleep(duration)
      }
    }
    thread.start(); thread.join()
  }

  class SyncVar[T] {
    private var t: Option[T] = None
    def get(): T = t.synchronized {
      val ret = t.get
      t = None
      ret
    }
    def put(x: T): Unit = t.synchronized {
      if (t.nonEmpty) throw new IllegalStateException
      t = Some(x)
    }
    def isEmpty(): Boolean = t.synchronized { t.isEmpty }
    def nonEmpty(): Boolean = t.synchronized { t.nonEmpty }
    def getWait(): T = this.synchronized {
      while (t.isEmpty) this.wait()
      val ret = t.get
      t = None
      this.notify()
      ret
    }
    def putWait(x: T): Unit = this.synchronized {
      while (t.nonEmpty) this.wait()
      t = Some(x)
      this.notify()
    }
  }

  import scala.collection._

  class SyncQueue[T](n: Integer) {
    private val queue = new mutable.Queue[T]
    def getWait(): T = this.synchronized {
      while (queue.isEmpty) this.wait()
      this.notify()
      queue.dequeue()
    }
    def putWait(x: T): Unit = this.synchronized {
      while (queue.size == n) this.wait()
      this.notify()
      queue.enqueue(x)
    }
  }

  class Account(val id: Int, var money: Int)

  def sendAll(accounts: Set[Account], target: Account): Unit = {
    def sendAll(source: Account, target: Account) = {
      target.money += source.money
      source.money = 0
    }
    for (account <- accounts) {
      if (account.id < target.id)
        account.synchronized { target.synchronized { sendAll(account, target) } }
      else
        target.synchronized { account.synchronized { sendAll(account, target) } }
    }
  }

  class PriorityTaskPool {
    class Task(val priority: Int, val work: () => Unit)
    private val queue = new mutable.PriorityQueue[Task]()(Ordering.by[Task, Int](_.priority))
    val worker = new Thread(new Runnable {
      override def run(): Unit = {
        while (true) {
          var task: Task = null
          queue.synchronized {
            while (queue.isEmpty) queue.wait()
            task = queue.dequeue()
          }
          task.work()
        }
      }
    })
    worker.start()

    def asynchronous(priority: Int)(task: =>Unit): Unit = {
      queue.synchronized {
        queue.enqueue(new Task(priority, () => task))
        queue.notify()
      }
    }
  }

  class MultithreadedPriorityTaskPool(p: Int) {
    class Task(val priority: Int, val work: () => Unit)
    private val queue = new mutable.PriorityQueue[Task]()(Ordering.by[Task, Int](_.priority))
    for (_ <- 1 to p) {
      val worker = new Thread(new Runnable {
        override def run(): Unit = {
          while (true) {
            var task: Task = null
            queue.synchronized {
              while (queue.isEmpty) queue.wait()
              task = queue.dequeue()
            }
            task.work()
          }
        }
      })
      worker.start()
    }

    def asynchronous(priority: Int)(task: =>Unit): Unit = {
      queue.synchronized {
        queue.enqueue(new Task(priority, () => task))
        queue.notify()
      }
    }
  }

  import scala.annotation.tailrec

  class AbortablePriorityTaskPool(p: Int, important: Int) {
    class Task(val priority: Int, val work: () => Unit)
    private val queue = new mutable.PriorityQueue[Task]()(Ordering.by[Task, Int](_.priority))
    private val workers = for (_ <- 1 to p) yield new Thread {
      private var highPriority = false
      def setHighPriority() = queue.synchronized {
        highPriority = true
        queue.notify()
      }

      def poll(): Option[Task] = queue.synchronized {
        @tailrec def firstImportant(): Option[Task] = {
          if (queue.nonEmpty) {
            val task = queue.dequeue()
            if (task.priority >= important) Some(task) else firstImportant()
          } else {
            None
          }
        }
        while (queue.isEmpty && !highPriority) queue.wait()
        if (!highPriority) Some(queue.dequeue())
        else firstImportant()
      }

      @tailrec override def run(): Unit = poll() match {
        case Some(task) => task.work(); run()
        case None =>
      }
    }
    workers.foreach(_.start())

    def asynchronous(priority: Int)(task: =>Unit): Unit = {
      queue.synchronized {
        queue.enqueue(new Task(priority, () => task))
        queue.notify()
      }
    }

    def shutdown(): Unit = {
      workers.foreach(_.setHighPriority())
    }
  }
}
