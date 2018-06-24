package com.github.jiwari.akkaexamples.akkapersistence

import akka.actor.{ActorLogging, ActorRef, ActorSystem, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.github.jiwari.akkaexamples.akkapersistence.Bakery._

object AkkaPersistence extends App {
  val system = ActorSystem("actor-system")
  val bakery: ActorRef = system.actorOf(Props[Bakery])

  bakery ! Make(Random.good -> Random.quantity)
  bakery ! Sell(Random.good -> Random.quantity)

  Thread.sleep(1000)

  system.terminate()
}

class Bakery extends PersistentActor with ActorLogging {

  var storage = Storage()

  override def receiveCommand: Receive = {
    case action: Action =>
      log.info("Action on ReceiveCommand")
      log.info("Storage: " + storage.toString)
      persist(action) { act =>
        executeAction(act)
      }
    case _ => log.info("Something else on ReceiveCommand")
  }

  override def receiveRecover: Receive = {
    case action: Action =>
      log.info("Recovering Action")
      executeAction(action)
    case SnapshotOffer(_, snapshot: Storage) =>
      log.info(s"Creating snapshot of data: $storage")
      storage = snapshot
  }

  override def persistenceId: String = "bakery-cache"

  def executeAction(action: Action): Unit = {
    storage = action match {
      case Sell(item) =>
        sellItem(item)
      case Make(item) =>
        makeItem(item)
    }
  }
  def makeItem(item: ItemAction): Bakery.Storage = {
    val (name, qtd) = item
    log.info(s"Producing $qtd $name")
    storage.update(item)
  }

  def sellItem(item: ItemAction): Bakery.Storage = {
    val (name, qtd) = item
    val availableItems: Integer = storage.items.getOrElse(name, 0)
    if (availableItems == 0) {
      log.info(s"There are no $name items available to sell. Nothing sold.")
      sender ! s"We are out of $name. Sorry for the inconvenience"
      storage
    } else if (qtd > availableItems) {
      log.info(s"There are not enough $name items available to sell. Selling all the $qtd available.")
      Storage(storage.items + (name -> 0))
    } else {
      val newAmount = availableItems - qtd
      log.info(s"Selling $qtd $name items. There are $newAmount of $name items still available")
      Storage(storage.items + (name -> newAmount))
    }
  }
}

object Bakery {

  type ItemAction = (Goods, Integer)

  sealed trait Goods
  case object Bread extends Goods
  case object Cookies extends Goods
  case object Cake extends Goods

  sealed trait Action
  sealed case class Sell(action: ItemAction) extends Action
  sealed case class Make(action: ItemAction) extends Action

  sealed case class Storage(items: Map[Goods, Integer] = Map()) {
    def update(item: ItemAction): Storage = {
      val (name, qtd) = item
      if (items.contains(name)) {
        val newQtd: Integer = items(name) + qtd
        Storage(items ++ Map(name -> newQtd))
      } else {
        Storage(items ++ Map(name -> qtd))
      }
    }
  }
}