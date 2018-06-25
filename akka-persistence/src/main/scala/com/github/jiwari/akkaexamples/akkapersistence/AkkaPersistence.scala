package com.github.jiwari.akkaexamples.akkapersistence

import akka.actor.{ActorLogging, ActorRef, ActorSystem, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.github.jiwari.akkaexamples.akkapersistence.Bakery._

object AkkaPersistence extends App {
  val system = ActorSystem("actor-system")
  val bakery: ActorRef = system.actorOf(Bakery.props("bakery-1"))

  bakery ! Make(Random.good -> Random.quantity)
  bakery ! Sell(Random.good -> Random.quantity)

  Thread.sleep(1000)

  system.terminate()
}

class Bakery(id: String) extends PersistentActor with ActorLogging {

  override def persistenceId: String = id

  var storage = Storage()

  override def receiveCommand: Receive = {
    case LookupStorage =>
      log.info("Looking up storage")
      sender ! storage
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

  def executeAction(action: Action): Unit = {
    val (updatedStorage, replyMessage) = action match {
      case Sell(item) =>
        sellItem(item)
      case Make(item) =>
        makeItem(item)
      case LookupStorage =>
        (storage, ReplyMessage("Lookup storage"))
    }

    storage = updatedStorage

    replyMessage match {
      case SellReply(message, _) =>
        sender ! SellReply(message, updatedStorage)
      case MakeReply(message, _) =>
        sender ! MakeReply(message, updatedStorage)
      case reply: ReplyMessage =>
        sender ! ReplyMessage(reply.message)
    }
  }

  def makeItem(item: ItemAction): (Bakery.Storage, ReplyMessage) = {
    val (name, qtd) = item
    val message = s"Producing $qtd $name"
    log.info(message)
    (storage.update(item), MakeReply(message))
  }

  def sellItem(item: ItemAction): (Bakery.Storage, ReplyMessage) = {
    val (name, qtd) = item
    val availableItems: Integer = storage.items.getOrElse(name, 0)
    if (availableItems == 0) {
      log.info(s"There are no $name items available to sell. Nothing sold.")
      val message = s"We are out of $name. Sorry for the inconvenience"
      (storage, SellReply(message))
    } else if (qtd > availableItems) {
      val message = s"There are not enough $name items available to sell. Selling all the $availableItems available."
      log.info(message)
      (Storage(storage.items + (name -> 0)), SellReply(message))
    } else {
      val newAmount = availableItems - qtd
      val message = s"Selling $qtd $name items."
      log.info(message)
      (Storage(storage.items + (name -> newAmount)), SellReply(message))
    }
  }
}

object Bakery {

  def props(id: String): Props = Props(new Bakery(id))

  type ItemAction = (Goods, Integer)

  sealed trait Goods
  case object Bread extends Goods
  case object Cookies extends Goods
  case object Cake extends Goods

  sealed trait Action
  sealed case class Sell(action: ItemAction) extends Action
  sealed case class Make(action: ItemAction) extends Action
  case object LookupStorage extends Action

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

  object Storage {
    def empty = Storage(Map())
  }

  sealed class ReplyMessage(val message: String)
  sealed case class SellReply(override val message: String, storage: Storage) extends ReplyMessage(message) {
    def this(message: String) = this(message, Storage.empty)
  }
  sealed case class MakeReply(override val message: String, storage: Storage) extends ReplyMessage(message)

  object SellReply {
    def apply(message: String): SellReply = SellReply(message, Storage.empty)
  }
  object MakeReply {
    def apply(message: String): MakeReply = MakeReply(message, Storage.empty)
  }

  object ReplyMessage {
    def apply(message: String): ReplyMessage = new ReplyMessage(message)
  }
}