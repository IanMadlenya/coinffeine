package coinffeine.gui.application.stats

import javafx.animation.{Animation, KeyFrame, Timeline}
import javafx.event.{ActionEvent, EventHandler}
import javafx.util.Duration
import scala.concurrent.duration._
import scalafx.Includes
import scalafx.scene.chart.{AreaChart, NumberAxis, XYChart}

import coinffeine.gui.util.FxExecutor
import coinffeine.model.currency.FiatCurrency
import coinffeine.model.market._
import coinffeine.peer.api.MarketStats

class OrderBookChart[C <: FiatCurrency](stats: MarketStats,
                                        market: Market[C]) extends AreaChart[Number, Number](
    OrderBookChart.xAxis(market), OrderBookChart.yAxis()) with Includes {

  private val reloader = new Timeline(new KeyFrame(
    Duration.millis(OrderBookChart.UpdateInterval.toMillis), new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent) = reloadData()
    }))

  title = "Order Book"

  startDataReload()

  private def startDataReload(): Unit = {
    reloadData()
    reloader.setCycleCount(Animation.INDEFINITE)
    reloader.play()
  }

  private def reloadData(): Unit = {
    implicit val executor = FxExecutor.asContext
    stats.openOrders(market).onSuccess {
      case entries =>
        data().clear()
        data() += toSeries(entries, Bid)
        data() += toSeries(entries, Ask)
    }
  }

  private def toSeries(data: Set[OrderBookEntry[C]], orderType: OrderType) = {
    val series = new XYChart.Series[Number, Number]() {
      name = orderType.toString
    }
    val seriesData = data.toSeq
      .filter(_.orderType == orderType)
      .groupBy(_.price.value.toDouble)
      .mapValues(sumCurrencyAmount)
    series.data = seriesData.toSeq.map(toChartData)
    series
  }

  private def sumCurrencyAmount(entries: Seq[OrderBookEntry[C]]) =
    entries.map(_.amount.value.toDouble).sum

  private def toChartData(data: (Double, Double)) = XYChart.Data[Number, Number](data._1, data._2)
}

object OrderBookChart {

  val UpdateInterval = 20.seconds

  private def xAxis[C <: FiatCurrency](market: Market[C]) = new NumberAxis {
    autoRanging = true
    label = s"Price (${market.currency}/BTC)"
    tickLabelFormatter = NumberAxis.DefaultFormatter(
      this, "", market.currency.javaCurrency.getSymbol)
  }

  private def yAxis[C <: FiatCurrency]() = new NumberAxis {
    autoRanging = true
    label = s"Bitcoins"
    tickLabelFormatter = NumberAxis.DefaultFormatter(this, "", "BTC")
  }
}