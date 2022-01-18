package funstack.lambda.ws

import net.exoego.facade.aws_lambda._

import scala.scalajs.js
import mycelium.core.message._
import sloth._
import chameleon.{Serializer, Deserializer}
import scala.scalajs.js.JSConverters._
import cats.effect.IO
import scala.concurrent.Future

object Handler {
  import scala.concurrent.ExecutionContext.Implicits.global

  type FunctionType = js.Function2[APIGatewayWSEvent, Context, js.Promise[APIGatewayProxyStructuredResultV2]]

  def handle[T, Event, Failure](
      router: Router[T, IO]
  )(implicit
      deserializer: Deserializer[ClientMessage[T], String],
      serializer: Serializer[ServerMessage[T, Event, Failure], String],
  ): FunctionType = handleF[T, Event, Failure, IO](router, _.map(Right.apply).unsafeToFuture())

  def handleF[T, Event, Failure, F[_]](
      router: Router[T, F],
      convert: F[T] => Future[Either[Failure, T]],
  )(implicit
      deserializer: Deserializer[ClientMessage[T], String],
      serializer: Serializer[ServerMessage[T, Event, Failure], String],
  ): FunctionType = handleWithContext[T, Event, Failure, F](router, (f, _) => convert(f))

  def handleFuture[T, Event, Failure](
      router: Router[T, Future],
  )(implicit
      deserializer: Deserializer[ClientMessage[T], String],
      serializer: Serializer[ServerMessage[T, Event, Failure], String],
  ): FunctionType = handleF[T, Event, Failure, Future](router, _.map(Right.apply))

  def handleWithContext[T, Event, Failure, F[_]](
      router: Router[T, F],
      convert: (F[T], APIGatewayWSRequestContext) => Future[Either[Failure, T]],
  )(implicit
      deserializer: Deserializer[ClientMessage[T], String],
      serializer: Serializer[ServerMessage[T, Event, Failure], String],
  ): FunctionType = { (event, context) =>
    // println(js.JSON.stringify(event))
    // println(js.JSON.stringify(context))
    val result: js.Promise[ServerMessage[T, Event, Failure]] = Deserializer[ClientMessage[T], String].deserialize(event.body) match {
      case Left(error) => js.Promise.reject(new Exception(s"Deserializer: $error"))
      case Right(Ping) => js.Promise.resolve[Pong.type](Pong)
      case Right(CallRequest(seqNumber, path, payload)) =>
        router(Request(path, payload)).toEither match {
          case Right(result) => convert(result, event.requestContext).toJSPromise.`then`[ServerMessage[T, Event, Failure]](CallResponse(seqNumber, _))
          case Left(error)   => js.Promise.reject(new Exception(error.toString))
        }
    }

    result
      .`then`[String](Serializer[ServerMessage[T, Event, Failure], String].serialize)
      .`then`[APIGatewayProxyStructuredResultV2](
        payload => APIGatewayProxyStructuredResultV2(body = payload, statusCode = 200),
        (((e: Any) => APIGatewayProxyStructuredResultV2(body = e.toString, statusCode = 500)): js.Function1[
          Any,
          APIGatewayProxyStructuredResultV2,
        ]): js.UndefOr[js.Function1[Any, APIGatewayProxyStructuredResultV2]],
      )
      .`then`[APIGatewayProxyStructuredResultV2]({ (result: APIGatewayProxyStructuredResultV2) =>
        println(js.JSON.stringify(result))
        result: APIGatewayProxyStructuredResultV2
      }: js.Function1[APIGatewayProxyStructuredResultV2, APIGatewayProxyStructuredResultV2])
  }
}
