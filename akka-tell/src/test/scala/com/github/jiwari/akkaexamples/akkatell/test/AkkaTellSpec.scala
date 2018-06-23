package com.github.jiwari.akkaexamples.akkatell.test

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.github.jiwari.akkaexamples.akkatell.Player
import com.github.jiwari.akkaexamples.akkatell.Player._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class AkkaTellSpec(_system: ActorSystem) extends TestKit(_system)
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll
  with ImplicitSender {

  def this() = this(ActorSystem("AkkaTellSpec"))

  val runReply = "Run action done"
  val restReply = "Rest action done"

  override protected def afterAll(): Unit = shutdown(system)

  "Player" should {
    "reply when the run action is done" in {
      val player = TestActorRef[Player]

      player ! Run

      expectMsg(runReply)
    }
    "reply when the rest is done" in {
      val player = TestActorRef[Player]

      player ! Rest

      expectMsg(restReply)
    }
    "reply when multiple actions are done" in {
      val player = TestActorRef[Player]

      player ! Rest
      player ! Rest
      player ! Run
      player ! Run

      expectMsgAllOf(
        restReply,
        runReply,
        restReply,
        runReply
      )
    }
  }
}
