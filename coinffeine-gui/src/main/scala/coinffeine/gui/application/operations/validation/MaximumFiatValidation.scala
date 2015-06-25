package coinffeine.gui.application.operations.validation

import scalaz.NonEmptyList

import coinffeine.model.currency.CurrencyAmount
import coinffeine.model.market.Spread
import coinffeine.model.order.OrderRequest
import coinffeine.peer.amounts.AmountsCalculator

private class MaximumFiatValidation(amountsCalculator: AmountsCalculator) extends OrderValidation {

  override def apply(request: OrderRequest, spread: Spread): OrderValidation.Result = {
    val maximum = amountsCalculator.maxFiatPerExchange(request.price.currency)
    val tooHighRequestOpt = for {
      price <- request.estimatedPrice(spread)
      requestedFiat = price.of(request.amount)
      if requestedFiat > maximum
    } yield requestedFiat
    tooHighRequestOpt.fold[OrderValidation.Result](OrderValidation.OK)(amount =>
      maximumAmountViolated(amount, maximum)
    )
  }

  private def maximumAmountViolated(requested: CurrencyAmount[_], maximum: CurrencyAmount[_]) =
    OrderValidation.Error(NonEmptyList(
      s"Maximum allowed fiat amount is $maximum, but you requested $requested"))
}
