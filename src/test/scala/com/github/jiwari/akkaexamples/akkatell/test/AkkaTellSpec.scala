package com.github.jiwari.akka_examples.akkatell.test

import akka.actor.ActorSystem
import akka.testkit.TestKit

class AkkaTellSpec(_system: ActorSystem) extends TestKit(_system)
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll {
  def this() = this(ActorSystem("CustomerSpec"))

  override
}
