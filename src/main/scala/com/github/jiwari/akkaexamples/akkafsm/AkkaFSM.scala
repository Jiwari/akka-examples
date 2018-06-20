package com.github.jiwari.akka_examples.akkafsm

import akka.actor.{ActorLogging, ActorRef, ActorSystem, FSM, Props}
import com.github.jiwari.akka_examples.akkafsm.Bakery._

object AkkaFSM extends App {
  val system: ActorSystem = ActorSystem.create("actor-system")
  val bakery: ActorRef = system.actorOf(Props[Bakery])

  // As the state is Close, will fall into 'whenUnhandled' rule
  bakery ! Sell(Bread -> 5)
  bakery ! Produce(Bread -> 5)

  // Changes State
  bakery ! Start

  // Produce
  bakery ! Produce(Cake -> 1)
  bakery ! Sell(Cake -> 1)
  bakery ! Produce(Bread -> 2)
  bakery ! Produce(Bread -> 2)
  bakery ! Produce(Cookies -> 5)
  bakery ! Sell(Cookies -> 3)
  bakery ! Sell(Cookies -> 3)
  bakery ! Produce(Cookies -> 7)

  bakery ! End

  Thread.sleep(500)

  system.terminate()
}

class Bakery extends FSM[Status, Data] with ActorLogging {

  startWith(Close, Empty)

  when(Close) {
    case Event(Start, _) =>
      goto(Open) using Empty
  }

  when(Open) {
    case Event(End, data) =>
      goto(Close) using data
    case Event(Produce(item), Empty) =>
      produceItem(item, Storage(Map.empty))
    case Event(Produce(item), items: Storage) =>
      produceItem(item, items)
    case Event(Sell(item), Empty) =>
      sellItem(item, Storage(Map.empty))
    case Event(Sell(item), items: Storage) =>
      sellItem(item, items)
  }

  def produceItem(item: ItemAction, items: Storage): FSM.State[Status, Data] = {
    val (name, qtd) = item
    log.info(s"Producing $qtd $name")
    stay() using items.add(item)
  }

  def sellItem(item: ItemAction, items: Storage): FSM.State[Status, Data] = {
    val (name, qtd) = item
    val availableItems: Integer = items.items.getOrElse(name, 0)
    if (availableItems == 0) {
      log.info(s"There are no $name items available to sell. Nothing sold.")
      stay using items
    } else if (qtd > availableItems) {
      log.info(s"There are not enough $name items available to sell. Selling everything.")
      stay using Storage(items.items + (name -> 0))
    } else {
      val newAmount = availableItems - qtd
      log.info(s"Selling $qtd $name items. There are $newAmount of $name items still available")
      stay using Storage(items.items + (name -> newAmount))
    }
  }

  whenUnhandled {
    case Event(_: Action, _) =>
      log.info("No action will be taken")
      stay
  }

  onTransition {
    case Open -> Close =>
      log.info("Moving from Open to Close state")
      stateData match {
        case storage: Storage =>
          val availableItems = storage.items.map(f => s"\n\t${f._1}: ${f._2}").mkString
          log.info(s"Available items: $availableItems")
        case _ =>
          log.info("No items on storage")
      }
    case Close -> Open =>
      log.info("Moving from Close to Open state")
  }

  initialize()
}


object Bakery {
  sealed trait Status
  case object Open extends Status
  case object Close extends Status

  type ItemAction = (Goods, Integer)

  sealed trait Action
  case object End extends Action
  case object Start extends Action
  sealed case class Produce(item: ItemAction) extends Action
  sealed case class Sell(item: ItemAction) extends Action

  sealed trait Data
  case object Empty extends Data
  sealed case class Storage(items: Map[Goods, Integer]) extends Data {
    def add(item: ItemAction): Data = {
      val (name, qtd) = item
      if (items.contains(name)) {
        val newQtd: Integer = items(name) + qtd
        Storage(items ++ Map(name -> newQtd))
      } else {
        Storage(items ++ Map(name -> qtd))
      }
    }
  }

  sealed trait Goods
  case object Bread extends Goods
  case object Cake extends Goods
  case object Cookies extends Goods
}