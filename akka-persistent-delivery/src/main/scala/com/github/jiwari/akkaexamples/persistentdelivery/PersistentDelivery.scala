package com.github.jiwari.akkaexamples.persistentdelivery

import akka.actor.{Actor, ActorLogging, ActorSelection, ActorSystem, Props}
import akka.persistence.{AtLeastOnceDelivery, PersistentActor, RecoveryCompleted}
import com.github.jiwari.akkaexamples.persistentdelivery.Courier.{SendMessage, ShutdownCourier}

object PersistentDelivery extends App {
  // Create the actors
  val system = ActorSystem("actor-system")
  val recipient = system.actorOf(Props[Recipient])
  val recipientPath: ActorSelection = system.actorSelection(recipient.path)
  val courier = system.actorOf(Courier.props("10001", recipientPath))
  Thread.sleep(1500)

  // Sends 2 messages, the Recipient actor will not reply for the second
  courier ! SendMessage("Alpha et Omega")
  courier ! SendMessage("Principium et Finis")
  Thread.sleep(500)
  courier ! ShutdownCourier
  Thread.sleep(500)

  val courierReborn = system.actorOf(Courier.props("10001", recipientPath))
  Thread.sleep(1500)

  system.terminate()
}

class Courier(courierId: String, recipient: ActorSelection) extends PersistentActor
  with AtLeastOnceDelivery
  with ActorLogging {
  import Courier._
  import Recipient._

  log.info(s"Starting up courier id#$courierId")

  override def persistenceId: String = courierId

  override def receiveRecover: Receive = {
    case event: Event =>
      updateState(event)
    case RecoveryCompleted =>
      recipient ! s"Recovery of Courier[$courierId] finished."
      log.info(s"Recovery of Courier[$courierId] finished!")
  }

  override def receiveCommand: Receive = {
    case SendMessage(text) =>
      persist(MessageSent(text))(updateState)
    case Confirm(id) =>
      sender ! MessageConfirmed(id)
      persist(MessageConfirmed(id))(updateState)
    case ShutdownCourier =>
      sender ! ShutdownCourier
      context.stop(self)
    case _ =>
      log.info("Action not mapped")
  }

  def updateState(event: Event): Unit =
    event match {
      case MessageSent(text) =>
        log.info("Sending message to recipient")
        deliver(recipient)(id => Message(id, text))
      case MessageConfirmed(id) =>
        log.info(s"Message id [$id] confirmed by the recipient")
        confirmDelivery(id)
    }
}

class Recipient extends Actor with ActorLogging{
  import Courier._
  import Recipient._

  // The counter is just used to make a simple rule where some messages will be replied, and other won't
  var counter = 0

  override def receive: Receive = {
    case Message(id, text) =>
      counter += 1
      log.info(s"Message received with text $text. Current counter $counter")
      if (counter % 2 != 0) sender ! Confirm(id)
      else log.info(s"Not confirming message id $id")
  }
}

object Recipient {
  sealed case class Message(id: Long, text: String)
}

object Courier {

  def props(courierId: String, actor: ActorSelection): Props = Props(new Courier(courierId, actor))

  // Actions
  sealed case class Confirm(id: Long)
  sealed case class SendMessage(text: String)

  // Events
  sealed trait Event
  sealed case class MessageSent(text: String) extends Event
  sealed case class MessageConfirmed(id: Long) extends Event

  case object ShutdownCourier
}