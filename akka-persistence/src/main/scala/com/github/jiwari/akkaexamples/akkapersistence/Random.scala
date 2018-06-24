package com.github.jiwari.akkaexamples.akkapersistence

import com.github.jiwari.akkaexamples.akkapersistence.Bakery._

import scala.util.{ Random => sRandom}

object Random {

  private val allGoods = Seq(Bread, Cake, Cookies)

  def good: Goods = {
    allGoods(sRandom.nextInt(allGoods.size))
  }

  def quantity: Integer = {
    sRandom.nextInt(10) + 1
  }

  def itemAction: ItemAction ={
    (good, quantity)
  }
}
