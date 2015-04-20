package org.learningconcurrency.ch3

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import scala.annotation.tailrec
import scala.collection.{concurrent, mutable}
import scala.concurrent.ExecutionContext

object Exercises extends App {

  class PiggybackContext extends ExecutionContext {
    override def execute(runnable: Runnable): Unit = runnable.run()
    override def reportFailure(cause: Throwable): Unit = cause.printStackTrace()
  }

  final class TreiberStack[T] {
    private val ref = new AtomicReference[List[T]](Nil)

    @tailrec def push(x: T): Unit = {
      val oldStack = ref.get
      val modifiedStack = x :: oldStack
      if (!ref.compareAndSet(oldStack, modifiedStack)) push(x)
    }

    @tailrec def pop(): T = {
      val oldStack = ref.get
      val modifiedStack = oldStack.tail
      if (!ref.compareAndSet(oldStack, modifiedStack)) pop() else oldStack.head
    }
  }

  final class ConcurrentSortedList[T](implicit val ord: Ordering[T]) {
    private val list: AtomicReference[List[T]] = new AtomicReference(Nil)

    @tailrec def add(x: T): Unit = {
      val oldList = list.get
      val newList = oldList.takeWhile(ord.gteq(x, _)) ::: x :: oldList.dropWhile(ord.gteq(x, _))
      if (!list.compareAndSet(oldList, newList)) add(x)
    }

    def iterator: Iterator[T] = list.get.iterator
  }

  class LazyCell[T](initialization: =>T) {
    private val initialized = new AtomicBoolean
    private var cache: T = _

    def apply(): T = {
      if (initialized.compareAndSet(false, true)) cache = initialization
      cache
    }
  }

  class SyncConcurrentMap[A, B] extends concurrent.Map[A, B] {
    private val map = mutable.Map[A, B]()

    override def putIfAbsent(k: A, v: B): Option[B] = map.synchronized {
      val ret = map.get(k)
      if (ret.isEmpty) map.put(k, v)
      ret
    }

    override def replace(k: A, oldValue: B, newValue: B): Boolean = map.synchronized {
      val ret = map.get(k).contains(oldValue)
      if (ret) map.put(k, newValue)
      ret
    }

    override def replace(k: A, v: B): Option[B] = map.synchronized {
      val ret = map.get(k)
      if (ret.nonEmpty) map.put(k, v)
      ret
    }

    override def remove(k: A, v: B): Boolean = map.synchronized {
      map.remove(k).contains(v)
    }

    override def -=(key: A): this.type = map.synchronized {
      map -= key
      this
    }

    override def +=(kv: (A, B)): this.type = map.synchronized {
      map += kv
      this
    }

    override def get(key: A): Option[B] = map.synchronized {
      map.get(key)
    }

    override def iterator: Iterator[(A, B)] = map.synchronized {
      map.iterator
    }
  }

}
