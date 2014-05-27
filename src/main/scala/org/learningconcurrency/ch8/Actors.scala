package org.learningconcurrency
package ch8



import akka.actor.Actor
import akka.actor.ActorRef
import akka.event.Logging
import akka.actor.Props
import akka.actor.ActorSystem
import scala.io.Source
import scala.collection._



class HelloActor(val hello: String) extends Actor {
  val log = Logging(context.system, this)
  def receive = {
    case `hello` =>
      log.info(s"Received a '$hello'... $hello!")
    case msg     =>
      log.info(s"Unexpected message '$msg'")
      context.stop(self)
  }
}


object HelloActor {
  def props(hello: String) = Props(new HelloActor(hello))
  def propsVariant(hello: String) = Props(classOf[HelloActor], hello)
}


object ActorsCreate extends App {
  val hiActor = ourSystem.actorOf(HelloActor.props("hi"), name = "greeter")
  hiActor ! "hi"
  Thread.sleep(1000)
  hiActor ! "hola"
  Thread.sleep(1000)
  ourSystem.shutdown()
}


class DeafActor extends Actor {
  val log = Logging(context.system, this)
  def receive = PartialFunction.empty
  override def unhandled(msg: Any) = log.info(s"could not handle '$msg'")
}


object ActorsUnhandled extends App {
  val deafActor = ourSystem.actorOf(Props[DeafActor], name = "deafy")
  deafActor ! "hi"
  Thread.sleep(1000)
  deafActor ! "can you hear me?"
  ourSystem.stop(deafActor)
  Thread.sleep(1000)
  ourSystem.shutdown()
}


class DictionaryActor extends Actor {
  private val log = Logging(context.system, this)
  private val dictionary = mutable.Set[String]()
  def receive = uninitialized
  def uninitialized: PartialFunction[Any, Unit] = {
    case DictionaryActor.Init => // from /usr/share/dict/words 
      val stream = getClass.getResourceAsStream("/org/learningconcurrency/words.txt")
      val words = Source.fromInputStream(stream)
      for (w <- words.getLines) dictionary += w
      context.become(initialized)
    case msg =>
      log.info(s"Unknown message '$msg' - did you initialize me?")
  }
  def initialized: PartialFunction[Any, Unit] = {
    case DictionaryActor.IsWord(w) =>
      log.info(s"word '$w' exists: ${dictionary(w)}")
    case DictionaryActor.End =>
      dictionary.clear()
      context.become(uninitialized)
  }
}


object DictionaryActor {
  case object Init
  case class IsWord(w: String)
  case object End
}


object ActorsBecome extends App {
  val dict = ourSystem.actorOf(Props[DictionaryActor], "dictionary")
  dict ! DictionaryActor.IsWord("program")
  Thread.sleep(1000)
  dict ! DictionaryActor.Init
  Thread.sleep(1000)
  dict ! DictionaryActor.IsWord("program")
  Thread.sleep(1000)
  dict ! DictionaryActor.IsWord("balaban")
  Thread.sleep(1000)
  dict ! DictionaryActor.End
  Thread.sleep(1000)
  dict ! DictionaryActor.IsWord("termination")
  Thread.sleep(1000)
  ourSystem.stop(dict)
  Thread.sleep(1000)
  ourSystem.shutdown()
}


class ParentActor extends Actor {
  val log = Logging(context.system, this)
  def receive = {
    case "create" =>
      context.actorOf(Props[ChildActor], "kid")
      log.info(s"created a new child - children = ${context.children}")
    case "sayhi" =>
      log.info("Kids, say hi!")
      for (c <- context.children) c ! "sayhi"
    case "stop" =>
      log.info("parent stopping")
      context.stop(self)
  }
}


class ChildActor extends Actor {
  val log = Logging(context.system, this)
  def receive = {
    case "sayhi" =>
      val parent = context.parent
      log.info(s"my parent $parent made me say hi!")
  }
  override def postStop() {
    log.info("kid got stopped!")
  }
}


object ActorsHierarchy extends App {
  val parent = ourSystem.actorOf(Props[ParentActor], "parent")
  parent ! "create"
  Thread.sleep(1000)
  parent ! "sayhi"
  Thread.sleep(1000)
  parent ! "stop"
  Thread.sleep(1000)
  ourSystem.shutdown()
}


class CheckActor extends Actor {
  import akka.actor.{Identify, ActorIdentity}
  val log = Logging(context.system, this)
  def receive = {
    case path: String =>
      log.info(s"checking path $path")
      context.actorSelection(path) ! Identify(7)
    case ActorIdentity(7, Some(ref)) =>
      log.info(s"found actor $ref")
    case ActorIdentity(7, None) =>
      log.info("could not find an actor")
  }
}


object ActorsIdentify extends App {
  val checker = ourSystem.actorOf(Props[CheckActor], "checker")
  checker ! "../*"
  Thread.sleep(1000)
  checker ! "../../*"
  Thread.sleep(1000)
  checker ! "/system/*"
  Thread.sleep(1000)
  checker ! "/user/checker2"
  Thread.sleep(1000)
  checker ! "akka://OurExampleSystem/system"
  Thread.sleep(1000)
  ourSystem.stop(checker)
  Thread.sleep(1000)
  ourSystem.shutdown()
}


class ExampleActor extends Actor {
  val log = Logging(context.system, this)
  var child: ActorRef = _
  def receive = {
    case num: Double  => log.info(s"got a double - $num")
    case num: Int     => log.info(s"got an integer - $num")
    case lst: List[_] => log.info(s"list - ${lst.head}, ...")
    case txt: String  => child ! txt
  }
  override def preStart(): Unit = {
    log.info("about to start")
    child = context.actorOf(Props[StringPrinter], "kiddo")
  }
  override def preRestart(reason: Throwable, msg: Option[Any]): Unit = {
    log.info(s"about to restart because of $reason")
    super.preRestart(reason, msg)
  }
  override def postRestart(reason: Throwable): Unit = {
    log.info(s"just restarted due to $reason")
    super.postRestart(reason)
  }
  override def postStop() = log.info("just stopped")
}


class StringPrinter extends Actor {
  val log = Logging(context.system, this)
  def receive = {
    case msg => log.info(s"child got message '$msg'")
  }
  override def preStart(): Unit = log.info(s"child about to start.")
  override def postStop(): Unit = log.info(s"child just stopped.")
}


object ActorsLifecycle extends App {
  val example = ourSystem.actorOf(Props[ExampleActor], "example")
  example ! math.Pi
  Thread.sleep(1000)
  example ! 7
  Thread.sleep(1000)
  example ! "hi there!"
  Thread.sleep(1000)
  example ! Nil
  Thread.sleep(1000)
  example ! "sorry about that"
  Thread.sleep(1000)
  ourSystem.stop(example)
  Thread.sleep(1000)
  ourSystem.shutdown()
}











