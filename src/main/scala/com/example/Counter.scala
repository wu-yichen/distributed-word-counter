package com.example

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, ActorSystem, Identify, PoisonPill, Props}
import com.typesafe.config.ConfigFactory

import scala.language.postfixOps

object WordCountDomain {
  case class Initialise(numberOfWorker: Int)

  case class WordCountJob(sentence: String)

  case class WordCountResult(result: Int)
}

class WordCountMain extends Actor with ActorLogging {

  import WordCountDomain._

  override def receive: Receive = {
    case Initialise(numberOfWorker) =>
      val selections = (1 to numberOfWorker).map { i =>
        context.actorSelection(s"akka://WorkerApp@localhost:2552/user/worker$i")
      }
      selections.foreach { selection =>
        log.info(s"Main ----  find actor $selection")
        selection ! Identify("whatever")
      }

      context.become(start(Seq.empty, 0))
  }

  def start(actors: Seq[ActorRef], number: Int): Receive = {
    case ActorIdentity("whatever", Some(ref)) =>
      if (number == 4) {
        log.info(s"number = $number, finding all the actors, ready to become execution $actors")
        context.become(executeWork(actors :+ ref, 0, 0))
      } else {
        log.info(s"finding actor $ref, number is $number")
        context.become(start(actors :+ ref, number + 1))
      }
  }

  def executeWork(workers: Seq[ActorRef], remainingJob: Int, totalCount: Int): Receive = {
    case text: String =>
      val sentences = text.split(" \\. ")
      Iterator.continually(workers).flatten.zip(sentences.iterator).foreach { pair =>
        val (worker, sentence) = pair
        worker ! WordCountJob(sentence)
      }
      context.become(executeWork(workers, remainingJob + sentences.length, totalCount))
    case WordCountResult(result) =>
      if (remainingJob == 1) {
        log.info(s"Total Result: ${totalCount + result}")
        workers.foreach(_ ! PoisonPill)
        context.stop(self)
      } else {
        context.become(executeWork(workers, remainingJob - 1, totalCount + result))
      }
  }
}

class WordCountWorker extends Actor with ActorLogging {

  import WordCountDomain._

  override def preStart(): Unit = {
    log.info(s"Worker $self is starting")
  }

  override def receive: Receive = {
    case WordCountJob(sentence) =>
      log.info(s"I am processing $sentence")
      sender() ! WordCountResult(sentence.split(" ").length)
  }
}

object MainApp extends App {

  import WordCountDomain._

  val config = ConfigFactory.parseString(
    """
      |akka.remote.artery.canonical.port = 2551
      |""".stripMargin
  ).withFallback(ConfigFactory.load("WordCounter.conf"))

  val system = ActorSystem("mainApp", config)

  val actor = system.actorOf(Props[WordCountMain])

  actor ! Initialise(5)

  Thread.sleep(1000)

  val source = scala.io.Source.fromFile("src/main/resources/lipsum.txt")
  source.getLines().foreach { line =>
    actor ! line
  }
  source.close()
}

object WorkerApp extends App {

  val config = ConfigFactory.parseString(
    """
      |akka.remote.artery.canonical.port = 2552
      |""".stripMargin
  ).withFallback(ConfigFactory.load("WordCounter.conf"))

  val system = ActorSystem("WorkerApp", config)

  (1 to 5).map(i => system.actorOf(Props[WordCountWorker], s"worker$i"))
}
