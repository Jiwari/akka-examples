package com.github.jiwari.akkaexamples.akkastate

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

object AkkaState extends App {

  import com.github.jiwari.akkaexamples.akkastate.Bakery._

  val system = ActorSystem.create("actor-system")
  val bakery = system.actorOf(Props[Bakery])

  // Tries to send Task on Closed state
  bakery ! Make(Bread)

  // Changes the state to Open
  bakery ! Open

  // Sends tasks
  bakery ! Make(Bread)
  bakery ! Make(Cookies)
  bakery ! Make(Donuts)
  bakery ! Sell(Bread)
  bakery ! Sell(Donuts)

  // Changes the state to Close
  bakery ! Closed

  // Tries to send task
  bakery ! Sell(Cookies)

  system.terminate()
}

class Bakery extends Actor with ActorLogging {

  import com.github.jiwari.akkaexamples.akkastate.Bakery._

  override def receive: Receive = closed

  def open: Receive = {
    case Sell(item) =>
      log.info(s"Selling some $item!")
      sender ! s"Selling some $item!"
    case Make(item) =>
      log.info(s"Producing some $item!")
      sender ! s"Producing some $item!"
    case Closed =>
      log.info("Bakery is closing its business for the day")
      sender ! "Bakery is closing its business for the day"
      context.become(closed)
    case AskState =>
      sender ! Open
  }

  def closed: Receive = {
    case Open =>
      log.info("Opening the Bakery back again!")
      sender ! "Opening the Bakery back again!"
      context.become(open)
    case _: Task =>
      sender ! "The bakery is closed, no tasks will be executed"
      log.info("The bakery is closed, no tasks will be executed")
    case AskState =>
      sender ! Closed

  }
}

object Bakery {
  case object AskState

  sealed trait State
  case object Open extends State
  case object Closed extends State

  sealed trait Task
  sealed case class Make(item: Item) extends Task
  sealed case class Sell(item: Item) extends Task

  sealed trait Item
  case object Cake extends Item
  case object Bread extends Item
  case object Cookies extends Item
  case object Donuts extends Item
}

