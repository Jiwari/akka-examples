package com.github.jiwari.akkaexamples.akkaask

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.github.jiwari.akkaexamples.akkaask.Suspect.Question

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

object AkkaTellProps extends App {


  implicit val timeout = Timeout(5, TimeUnit.SECONDS)

  val system: ActorSystem = ActorSystem.create("actor-system")

  val suspect: ActorRef = system.actorOf(Props[Suspect])

  private val result: Future[(Boolean, Boolean, Boolean)] = for {
    q1 <- ask(suspect, Question).mapTo[Boolean]
    q2 <- (suspect ? Question).mapTo[Boolean]
    q3 <- (suspect ask Question).mapTo[Boolean]
  } yield (q1, q2, q3)

  result.foreach(f => println(s"Results: $f"))

  system.terminate()
}

class Suspect extends Actor with ActorLogging {
  import com.github.jiwari.akkaexamples.akkaask.Suspect._

  override def receive: Receive = {
    case Question => sender() ! Random.nextBoolean()
  }
}

object Suspect {
  case object Question
}