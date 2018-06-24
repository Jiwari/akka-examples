package com.github.jiwari.akkaexamples.akkapersistence.test

import akka.actor.{ActorSystem, PoisonPill}
import akka.testkit.{ImplicitSender, TestKit}
import com.github.jiwari.akkaexamples.akkapersistence.Bakery._
import com.github.jiwari.akkaexamples.akkapersistence.{Bakery, Random}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class AkkaPersistenceSpec(_system: ActorSystem) extends TestKit(_system)
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll
  with ImplicitSender {

  def this() = this(ActorSystem("akka-persistence-spec"))

  override protected def afterAll(): Unit = shutdown(system)

  "Persistence Bakery" should {
    "allow to Make items" in {
      val bakeryId = "bakery-001"
      val bakery = system.actorOf(Bakery.props(bakeryId))
      val (item, qtd) = Random.itemAction

      // Make some items
      bakery ! Make(item -> qtd)
      expectMsg(
        MakeReply(s"Producing $qtd $item",
          Storage(Map(item -> qtd)))
      )
    }
    "allow to Sell items that are available" in {
      val bakeryId = "bakery-002"
      val bakery = system.actorOf(Bakery.props(bakeryId))
      val item = Random.good

      // Make some items
      bakery ! Make(item -> 8)
      bakery ! Sell(item -> 6)
      expectMsgAllOf(
        MakeReply(s"Producing 8 $item",
          Storage(Map(item -> 8))),
        SellReply(s"Selling 6 $item items.",
          Storage(Map(item -> 2)))
      )
    }
    "sell all items when the requested item number is higher than the available amount" in {
      val bakeryId = "bakery-002"
      val bakery = system.actorOf(Bakery.props(bakeryId))
      val item = Random.good

      // Make some items
      bakery ! Make(item -> 4)
      bakery ! Sell(item -> 6)
      expectMsgAllOf(
        MakeReply(s"Producing 4 $item",
          Storage(Map(item -> 4))),
        SellReply(s"There are not enough $item items available to sell. Selling all the 4 available.",
          Storage(Map(item -> 0)))
      )
    }
    "allow to Make items and restore them" in {
      val bakeryId = "bakery-003"
      val bakery = system.actorOf(Bakery.props(bakeryId))
      val (item, qtd) = Random.itemAction

      // Make some items
      bakery ! Make(item -> qtd)
      expectMsg(
        MakeReply(s"Producing $qtd $item",
          Storage(Map(item -> qtd)))
      )
      // Destroy Bakery Actor
      bakery ! PoisonPill

      // Restore
      val bakeryRestarted = system.actorOf(Bakery.props(bakeryId))
      bakeryRestarted ! LookupStorage
      expectMsg(Storage(Map(item -> qtd)))
    }
    "allow to Sell items and restore them" in {
      val bakeryId = "bakery-005"
      val bakery = system.actorOf(Bakery.props(bakeryId))
      val item = Random.good

      // Make & Sell some items
      bakery ! Make(item -> 7)
      bakery ! Sell(item -> 3)
      expectMsgAllOf(
        MakeReply(s"Producing 7 $item",
          Storage(Map(item -> 7))),
        SellReply(s"Selling 3 $item items.",
          Storage(Map(item -> 4)))
      )
      // Destroy Bakery Actor
      bakery ! PoisonPill

      // Restore
      val bakeryRestarted = system.actorOf(Bakery.props(bakeryId))
      bakeryRestarted ! LookupStorage
      expectMsg(Storage(Map(item -> 4)))
    }
  }
}
