package com.github.jiwari.akkaexamples.persistentdelivery.test

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.github.jiwari.akkaexamples.persistentdelivery.Courier
import com.github.jiwari.akkaexamples.persistentdelivery.Courier._
import com.github.jiwari.akkaexamples.persistentdelivery.Recipient._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.util.Random

class PersistentDeliverySpec(_system: ActorSystem) extends TestKit(_system)
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with ImplicitSender {

  def this() = this(ActorSystem("persistent-delivery-spec"))

  override protected def afterAll(): Unit = shutdown(system)

  "Persistent Delivery Courier" should {
    "send received messages to the Recipient actor" in {
      val test = initiateCourier()

      val text = "Sample message"
      test.courier ! SendMessage(text)

      val result = test.probe.expectMsgAnyClassOf(classOf[Message])
      result.text shouldBe text
    }
    "should resend messages that were not confirmed" in {
      val test = initiateCourier()

      //Sends two messages
      val firstMessage = "First sample message"
      val secondMessage = "Second sample message"
      test.courier ! SendMessage(firstMessage)
      test.courier ! SendMessage(secondMessage)

      // Verifies if both messages were received
      val result = test.probe.expectMsgAllClassOf(classOf[Message], classOf[Message])
      result.head.text shouldBe firstMessage
      result.last.text shouldBe secondMessage

      // Confirms one of the messages to the courier
      test.courier ! Confirm(result.head.id)
      expectMsg(MessageConfirmed(result.head.id))
      // Shuts down the courier
      test.courier ! ShutdownCourier
      expectMsg(ShutdownCourier)

      // reInitiate courier
      val reTest = initiateCourier(test.courierId)

      val reResult = reTest.probe.expectMsgAllClassOf(classOf[Message])
      reResult.last.text shouldBe secondMessage

      reTest.courier ! Confirm(reResult.head.id)
      expectMsg(MessageConfirmed(reResult.last.id))
    }
  }

  def initiateCourier(courierId: String): TestCourier = {
    val probe = TestProbe()
    val pathOfProbe = system.actorSelection(probe.ref.path)
    val courier = system.actorOf(Courier.props(courierId, pathOfProbe))

    new TestCourier(courierId, courier, probe)
  }
  def initiateCourier(): TestCourier = {
    val courierId = "courier-" + Random.nextInt()
    val testCourier = initiateCourier(courierId)
    testCourier.probe.expectMsg(s"Recovery of Courier[$courierId] finished.")
    testCourier
  }

  protected class TestCourier(val courierId: String, val courier: ActorRef, val probe: TestProbe)
}
