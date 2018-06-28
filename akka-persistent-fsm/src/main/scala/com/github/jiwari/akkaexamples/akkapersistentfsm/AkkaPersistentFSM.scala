package com.github.jiwari.akkaexamples.akkapersistentfsm

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState
import com.github.jiwari.akkaexamples.akkapersistentfsm.Account._

import scala.reflect.ClassTag

object AkkaPersistentFSM extends App {
  val system = ActorSystem("actor-system")
  val account = system.actorOf(Account.props("account-1"))

  account ! Operation(500, Credit)
  account ! Operation(100, Debit)
  account ! CheckBalance

  account ! ShutdownAccount
  Thread.sleep(1000)

  val sameAccount = system.actorOf(Account.props("account-1"))
  sameAccount ! Operation(500, Credit)
  sameAccount ! Operation(100, Debit)
  sameAccount ! CheckBalance

  Thread.sleep(1000)
  system.terminate()
}

class Account(accountId: String) extends PersistentFSM[Status, Data, AccountEvent] with ActorLogging {
  override def applyEvent(event: AccountEvent, account: Data): Data = {
    event match {
      case AcceptedTransaction(amount, transaction@Credit) =>
        log.info(s"Transaction accepted: type $transaction with amount $amount.")
        Balance(account.balance + amount)
      case AcceptedTransaction(amount, transaction@Debit) =>
        log.info(s"Transaction accepted: type $transaction with amount $amount")
        if (amount == account.balance) {
          ZeroBalance
        } else {
          Balance(account.balance - amount)
        }
      case RejectTransaction(amount, transaction, reason) =>
        log.info(s"Transaction rejected. Amount $amount of type $transaction. Reason: $reason")
        account
    }
  }

  override def persistenceId: String = accountId

  override def domainEventClassTag: ClassTag[AccountEvent] = scala.reflect.classTag[AccountEvent]

  startWith(Empty, ZeroBalance)

  when(Empty) {
    case Event(Operation(amount, Credit), _) =>
      val acceptedTransaction = AcceptedTransaction(amount, Credit)
      sender ! acceptedTransaction
      goto(Open) applying acceptedTransaction
    case Event(Operation(amount, Debit), _) =>
      val rejectTransaction = RejectTransaction(amount, Debit, "Cannot debit on empty account")
      sender ! rejectTransaction
      stay applying rejectTransaction
  }

  when(Open) {
    case Event(Operation(amount, Credit), _) =>
      val acceptedTransaction = AcceptedTransaction(amount, Credit)
      sender ! acceptedTransaction
      stay applying acceptedTransaction
    case Event(Operation(amount, Debit), account) =>
      if (amount > account.balance) {
        val rejectTransaction = RejectTransaction(amount, Debit, "Not enough balance to perform debit")
        sender ! rejectTransaction
        stay applying rejectTransaction
      }
      else if (amount == account.balance) {
        val acceptedTransaction = AcceptedTransaction(amount, Debit)
        sender ! acceptedTransaction
        goto(Empty) applying acceptedTransaction
      }
      else {
        val acceptedTransaction = AcceptedTransaction(amount, Debit)
        sender ! acceptedTransaction
        stay applying acceptedTransaction
      }
  }

  /**
    * Shutting down Persistent actors should be avoided PoisonPill
    * https://doc.akka.io/docs/akka/2.5/persistence.html#safely-shutting-down-persistent-actors
   */
  whenUnhandled {
    case Event(CheckBalance, account) =>
      log.info(s"Account balance $account")
      sender ! account.balance
      stay
    case Event(ShutdownAccount, _) =>
      log.info(s"Shutting down Account actor id#$accountId")
      context.stop(self)
      stay
    case Event(message, _) =>
      log.info(s"Message not recognized. Event: $message")
      stay
  }
}

object Account {

  def props(accountId: String): Props = Props(new Account(accountId))

  // States
  sealed trait Status extends FSMState
  case object Open extends Status {
    override def identifier: String = "open-state"
  }
  case object Empty extends Status {
    override def identifier: String = "empty-state"
  }

  // Data
  sealed trait Data {
    val balance: Float
  }
  case object ZeroBalance extends Data {
    override val balance = 0.0f
  }
  sealed case class Balance(override val balance: Float) extends Data

  // Events
  sealed trait AccountEvent
  sealed case class AcceptedTransaction(amount: Float,
                                        transactionType: TransactionType) extends AccountEvent
  sealed case class RejectTransaction(amount: Float,
                                      transactionType: TransactionType,
                                      reason: String) extends AccountEvent

  sealed trait TransactionType
  case object Credit extends TransactionType
  case object Debit extends TransactionType

  // Commands
  sealed case class Operation(amount: Float,
                              actionType: TransactionType)
  case object CheckBalance

  case object ShutdownAccount

}