import akka.stream.{Attributes, FlowShape, Inlet, Outlet, SourceShape}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import org.slf4j.Logger

import scala.collection.mutable


case class ObservationPoint[T](name: String, logEvery: Int = 10, averageOver: Int = 10)(implicit logger: Logger) extends GraphStage[FlowShape[T, T]] {
  val in = Inlet[T](f"ObservationPoint${name}.in")
  val out = Outlet[T](f"ObservationPoint${name}.out")
  override val shape = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) with InHandler with OutHandler {
    val downstreamLatencies: mutable.Queue[Long] = mutable.Queue()
    val upstreamLatencies: mutable.Queue[Long] = mutable.Queue()
    var lastPush: Option[Long] = None
    var lastPull: Option[Long] = None
    var count: Long = 0L

    def recordItem(latency: Long, list: mutable.Queue[Long]): Unit = {
      list += latency
      if (list.length > averageOver) {
        list.dequeue()
      }
    }

    def record(push: Boolean): Unit = {
      if (push) {
        lastPush = Some(System.currentTimeMillis())
      } else {
        lastPull = Some(System.currentTimeMillis())
      }
      for {
        lastPushActual <- lastPush
        lastPullActual <- lastPull
      } {
        if (push) {
          val upstreamLatency = lastPushActual - lastPullActual
          recordItem(upstreamLatency, upstreamLatencies)
        } else {
          val downstreamLatency = lastPullActual - lastPushActual
          recordItem(downstreamLatency, downstreamLatencies)
        }
      }
    }

    def getFormattedIps(latencyMs: Long): String = {
      latencyMs match {
        case 0 =>
          "Inf"
        case _ =>
          val ips = 1000.0 / latencyMs.toFloat
          "%.2f".format(ips)
      }
    }

    def logPull(): Unit = {
      if (downstreamLatencies.nonEmpty) {
        val downstreamLatency = downstreamLatencies.sum / downstreamLatencies.length
        logger.info(s"ObservationPoint($name) - downstream latency ${downstreamLatency}ms, effective IPS: ${getFormattedIps(downstreamLatency)}")
      }
    }


    def logPush(): Unit = {
      if (upstreamLatencies.nonEmpty) {
        val upstreamLatency = upstreamLatencies.sum / upstreamLatencies.length
        logger.info(s"ObservationPoint($name) - upstream latency ${upstreamLatency}ms , effective IPS: ${getFormattedIps(upstreamLatency)}")
      }
    }

    override def onPush(): Unit = {
      count += 1
      record(true)
      if (count % logEvery == 0) {
        logPush()
      }
      push(out, grab(in))
    }

    override def onPull(): Unit = {
      record(false)
      if (count % logEvery == 0) {
        logPull()
      }
      pull(in)
    }

    setHandlers(in, out, this)
  }
}