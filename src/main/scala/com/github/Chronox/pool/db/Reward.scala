package com.github.Chronox.pool.db
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.KeyedEntity
import org.squeryl.dsl.CompositeKey2

class Reward (
  var userId: Long,
  var blockId: Long,
  var currentPercent: BigDecimal,
  var historicalPercent: BigDecimal,
  var isPaid: Boolean,
) extends KeyedEntity[CompositeKey2[Long, Long]] {
  def id = compositeKey(userId, blockId)
  def this() = this(0, 0, BigDecimal.valueOf(0), 
    BigDecimal.valueOf(0), false)
  def canEqual(a: Any) = a.isInstanceOf[Reward]
  override def equals(that: Any): Boolean =
    that match {
      case that: Reward => that.canEqual(this) && this.hashCode == that.hashCode
      case _ => false
    }
  override def hashCode: Int = {
    var result = userId.hashCode ^ blockId.hashCode ^ 
      currentPercent.hashCode ^ historicalPercent.hashCode
    return (if (isPaid) ~result else result)
  }
  override def toString: String = {
    return "|userId: " + userId + " ,blockId: " + blockId + " percents: (" + 
    currentPercent + "," + historicalPercent + "), isPaid: " + isPaid + "|"
  }
}