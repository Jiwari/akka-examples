package com.github.jiwari.akkaexamples.akkastate.test

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.github.jiwari.akkaexamples.akkastate.Bakery
import com.github.jiwari.akkaexamples.akkastate.Bakery._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class AkkaStateSpec(_system: ActorSystem) extends TestKit(_system: ActorSystem)
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll
  with ImplicitSender {

  def this() = this(ActorSystem("AkkaStateSpec"))

  override protected def afterAll(): Unit = shutdown(system)

  "Bakery" should {
    "start in closed state" in {
      val bakery = TestActorRef[Bakery]

      bakery ! AskState

      expectMsg(Closed)
    }
    "not execute Sell tasks on Closed state" in {
      val bakery = TestActorRef[Bakery]

      bakery ! Sell(Bread)

      expectMsgAllOf(Message.closed)
    }
    "not execute Make tasks on Closed state" in {
      val bakery = TestActorRef[Bakery]

      bakery ! Make(Bread)

      expectMsgAllOf(Message.closed)
    }
    "change state to open" in {
      val bakery = openBakery

      bakery ! AskState

      expectMsgAllOf(Open)
    }
    "allow to execute Make task on Open state" in {
      val bakery = openBakery

      bakeryItems.foreach(item => bakery ! Make(item))

      bakeryItems.foreach(
        item => expectMsg(Message.make(item))
      )
    }
    "allow to execute Sell task on Open state" in {
      val bakery = openBakery

      bakeryItems.foreach(item => bakery ! Sell(item))

      bakeryItems.foreach(
        item => expectMsg(Message.sell(item))
      )
    }
    "allow to Close on Open state" in {
      val bakery = openBakery

      bakery ! Closed
      bakery ! AskState

      expectMsgAllOf(
        "Bakery is closing its business for the day",
        Closed
      )
    }
  }

  val bakeryItems = Seq(Bread, Cookies, Cake)

  private def openBakery = {
    val bakery = TestActorRef[Bakery]
    bakery ! Open
    expectMsg(Message.open)
    bakery
  }

  implicit class BakeryImplicits(val bakery: TestActorRef[Bakery]) {
    def isOnState(state: State): Boolean = {
      val currentState = bakery.underlyingActor.receive
      currentState.isDefinedAt(state)
    }
  }

  private object Message {
    val sell: Item => String = (item: Item) => s"Selling some $item!"
    val make: Item => String = (item: Item) => s"Producing some $item!"
    val closed = "The bakery is closed, no tasks will be executed"
    val open = "Opening the Bakery back again!"
  }

}
