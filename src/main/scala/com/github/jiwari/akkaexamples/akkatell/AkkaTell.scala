package com.github.jiwari.akka_examples.akkatell

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

/**
  * Simplest example of actor usage using `tell`
  */
object AkkaTell extends App {

  import com.github.jiwari.akka_examples.akkatell.Player._

  // Instantiate the actor system
  val system: ActorSystem = ActorSystem.create("actor-system")
  // With the actor system, create the ActorRef of Player
  val player: ActorRef = system.actorOf(Props[Player])

  // Sends the message 'Run' to Player actor using the '!' sign
  player ! Run
  player ! Rest

  // Same as above, but without the sugar syntax
  player.tell(Run, ActorRef.noSender)
  player.tell(Rest, ActorRef.noSender)

  // Nothing will happen as the actor doesn't map this type of message
  player ! "Unmapped message"

  system.terminate()
}

class Player extends Actor with ActorLogging {

  import com.github.jiwari.akka_examples.akkatell.Player._

  /**
    * Every actor needs to define a PartialFunction on the 'receive' method
    * On this Player actor, only two messages are mapped: Run and Rest
    * Any other message would cause an exception
    * @return
    */
  override def receive: Receive = {
    case Run =>
      log.info("Player is running!")
    case Rest =>
      log.info("Player is rest...")
  }
}

object Player {

  sealed trait Order

  case object Run extends Order

  case object Rest extends Order
}
