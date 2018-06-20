package com.github.jiwari.akkaexamples.akkaask.test

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.util.Timeout
import com.github.jiwari.akkaexamples.akkaask.Suspect._
import com.github.jiwari.akkaexamples.akkaask._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class AkkaAskSpec(_system: ActorSystem) extends TestKit(_system)
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with ImplicitSender {

  def this() = this(ActorSystem("AkkaAskSpec"))

  implicit val timeout: Timeout = Timeout(5, TimeUnit.SECONDS)

  override protected def afterAll(): Unit = shutdown(system)

  "AkkaAsk" should {
    "be able to receive answers with 'ask' method" in {
      val suspect = TestActorRef[Suspect]

      val answer = ask(suspect, Question)

      ScalaFutures.whenReady(answer) { result =>
        result shouldBe a [java.lang.Boolean]
      }
    }
    "be able to receive answers with '?' method" in {
      val suspect = TestActorRef[Suspect]

      val answer = suspect ? Question

      ScalaFutures.whenReady(answer) { result =>
        result shouldBe a [java.lang.Boolean]
      }
    }
    "be able to receive answers with 'ask' implicit method" in {
      val suspect = TestActorRef[Suspect]

      val answer = suspect ask Question

      ScalaFutures.whenReady(answer) { result =>
        result shouldBe a [java.lang.Boolean]
      }
    }
  }
}
