package com.github.jiwari.akkaexamples.akkafsm.test

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import com.github.jiwari.akkaexamples.akkafsm.Bakery
import com.github.jiwari.akkaexamples.akkafsm.Bakery._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class AkkaFSMSpec(_system: ActorSystem) extends TestKit(_system)
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("AkkaFSMSpec"))

  override protected def afterAll(): Unit = shutdown(system)

  "Bakery FSM" should {
    "start on Closed state and Empty data" in {
      val (actor, _) = bakeryTestActor

      actor.stateData shouldBe Empty
      actor.stateName shouldBe Close
    }
    "move from Close state to Open" in {
      val (actor, bakery) = bakeryTestActor

      bakery ! OpenBusiness

      actor.stateData shouldBe Empty
      actor.stateName shouldBe Open
    }
    "accept Produce messages" in {
      val (actor, bakery) = openBakery
      val items = defaultItems

      items.foreach(bakery ! Produce(_))

      actor.stateName shouldBe Open
      actor.stateData shouldBe Storage(items.toMap)
    }
    "allow to add more of the same item" in {
      val (actor, bakery) = openBakery

      bakery ! Produce(Bread -> 4)
      bakery ! Produce(Bread -> 3)

      actor.stateName shouldBe Open
      actor.stateData shouldBe Storage(Map(Bread -> 7))
    }
    "accept Sell message when the item is available" in {
      val (actor, bakery, _) = bakeryWith(Bread -> 5)

      bakery ! Sell(Bread -> 3)

      actor.stateName shouldBe Open
      actor.stateData shouldBe Storage(Map(Bread -> 2))
    }
    "sell all of the requested item when there is not enough on stock" in {
      val (actor, bakery, _) = bakeryWith(Cake -> 10)

      bakery ! Sell(Cake -> 15)

      actor.stateName shouldBe Open
      actor.stateData shouldBe Storage(Map(Cake -> 0))
    }
    "reply to the sender when the requested item is not on stock" in {
      val (actor, bakery, items) = bakeryWith(Bread -> 5)
      val probe: TestProbe = new TestProbe(system, "test-probe")
      val requestedItem = Cookies

      bakery.tell(Sell(requestedItem -> 1), probe.ref)

      actor.stateName shouldBe Open
      actor.stateData shouldBe Storage(Map(items.head))
      probe.expectMsg(s"We are out of $requestedItem. Sorry for the inconvenience")
    }
  }

  private def defaultItems: Seq[ItemAction] = {
    Seq(Bread -> 5, Cookies -> 3, Cake -> 2)
  }

  private def bakeryWith(itemsAction: ItemAction*): (Bakery, TestActorRef[Bakery], Seq[ItemAction]) = {
    val (actor, bakery) = openBakery
    val items = itemsAction.toSeq
    items.foreach(bakery ! Produce(_))
    (actor, bakery, items)
  }

  private def openBakery: (Bakery, TestActorRef[Bakery]) = {
    val (actor, bakery) = bakeryTestActor
    bakery ! OpenBusiness
    (actor, bakery)
  }

  private def bakeryTestActor: (Bakery, TestActorRef[Bakery]) = {
    val bakery = TestActorRef[Bakery]
    val actor = bakery.underlyingActor
    (actor, bakery)
  }
}
