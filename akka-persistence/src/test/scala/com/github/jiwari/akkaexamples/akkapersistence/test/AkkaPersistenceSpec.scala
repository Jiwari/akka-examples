package com.github.jiwari.akkaexamples.akkapersistence.test

import akka.actor.{ActorSystem, PoisonPill}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}
import com.github.jiwari.akkaexamples.akkapersistence.Bakery._
import com.github.jiwari.akkaexamples.akkapersistence.Bakery
import com.github.jiwari.akkaexamples.akkapersistence.Random

class AkkaPersistenceSpec(_system: ActorSystem) extends TestKit(_system)
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll
  with ImplicitSender {

  def this() = this(ActorSystem("akka-persistence-spec"))

  override protected def afterAll(): Unit = shutdown(system)

  "Persistence Bakery" should {
    "allow to Make items and restore them" in {
      val bakery = system.actorOf(Bakery.props("bakery-001"))
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
      val bakeryRestarted = system.actorOf(Bakery.props("bakery-001"))
      bakeryRestarted ! LookupStorage
      expectMsg(Storage(Map(item -> qtd)))
    }
    "allow to Sell items and restore them" in {
      val bakery = system.actorOf(Bakery.props("bakery-002"))
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
      val bakeryRestarted = system.actorOf(Bakery.props("bakery-002"))
      bakeryRestarted ! LookupStorage
      expectMsg(Storage(Map(item -> 4)))
    }
  }

}
