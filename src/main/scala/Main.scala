import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.slf4j.{Logger, LoggerFactory}

object Main extends App {

  implicit val log: Logger = LoggerFactory.getLogger("Main")
  implicit val system: ActorSystem = ActorSystem("StreamObs")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  Source.fromIterator(() => Iterator.from(0)).mapAsync(4)(item => Future {
    Thread.sleep(2000)
    item
  }).via(ObservationPoint("A")).mapAsync(2)(item => Future {
    Thread.sleep(1000)
    item
  }).via(ObservationPoint("B")).runForeach(item => log.info(f"Completed for item ${item}"))
}
