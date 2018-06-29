package com.github.jiwari.akkaexamples.akkapersistentfsm.test

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import com.github.jiwari.akkaexamples.akkapersistentfsm.Account._
import com.github.jiwari.akkaexamples.akkapersistentfsm.Account

import scala.util.Random

class AkkaPersistentFSMSpec(_system: ActorSystem) extends TestKit(_system)
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with ImplicitSender {

  def this() = this(ActorSystem("AkkaPersistentFSMSpec"))

  override protected def afterAll(): Unit = shutdown(system)

  "PersistentFSM Account" should {
    "allow to do Credit operations on an Empty Account" in {
      val accountId = "account-1"
      val account = system.actorOf(Account.props(accountId))

      val amount: Float = 500

      account ! Operation(amount, Credit)
      account ! CheckBalance

      expectMsgAllOf(
        AcceptedTransaction(amount, Credit),
        amount
      )
    }
    "allow to do Credit operations on an Open Account" in {
      val initialBalance: Float = 500
      val (_, account) = accountWithCredit(initialBalance)

      val additionalAmount: Float = 200
      account ! Operation(additionalAmount, Credit)
      account ! CheckBalance

      expectMsgAllOf(
        AcceptedTransaction(additionalAmount, Credit),
        initialBalance + additionalAmount)
    }
    "not allow to do Debit operations on an Empty Account" in {
      val (_, account) = accountWithoutCredit

      val amount: Float = 200
      account ! Operation(amount, Debit)

      expectMsg(
        RejectTransaction(amount, Debit, "Cannot debit on empty account")
      )
    }
    "allow to do Debit operations on an Open Account when there are funds" in {
      val initialAmount: Float = 500
      val (_, account) = accountWithCredit(initialAmount)

      val amount: Float = 200
      account ! Operation(amount, Debit)
      account ! CheckBalance

      expectMsgAllOf(
        AcceptedTransaction(amount, Debit),
        initialAmount - amount
      )
    }
    "save the state of the Account" in {
      val initialBalance = 1000
      val (accountId, account) = accountWithCredit(initialBalance)

      val firstDebit: Float = 300
      val secondDebit: Float = 500
      val credit: Float = 200
      account ! Operation(firstDebit, Debit)
      account ! Operation(secondDebit, Debit)
      account ! Operation(credit, Credit)
      account ! CheckBalance
      account ! ShutdownAccount

      val finalBalance = initialBalance + credit - firstDebit - secondDebit
      expectMsgAllOf(
        AcceptedTransaction(firstDebit, Debit),
        AcceptedTransaction(credit, Credit),
        AcceptedTransaction(secondDebit, Debit),
        finalBalance
      )

      val recoveredAccount = system.actorOf(Account.props(accountId))
      recoveredAccount ! CheckBalance
      expectMsg(finalBalance)
    }
  }
  def accountWithoutCredit: (String, ActorRef) = {
    val accountId: String = Random.nextString(5)
    val account = system.actorOf(Account.props(accountId))
    (accountId, account)
  }

  def accountWithCredit(amount: Float): (String, ActorRef) = {
    val (accountId, account) = accountWithoutCredit
    account ! Operation(amount, Credit)
    expectMsg(AcceptedTransaction(amount, Credit))
    (accountId, account)
  }
}
