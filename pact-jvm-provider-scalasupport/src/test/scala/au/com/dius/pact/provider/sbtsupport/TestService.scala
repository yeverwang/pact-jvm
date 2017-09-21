package au.com.dius.pact.provider.sbtsupport

import com.typesafe.scalalogging.StrictLogging
import au.com.dius.pact.provider.unfiltered.Conversions
import au.com.dius.pact.model.{Request, Response}
import au.com.dius.pact.provider.sbtsupport.AnimalServiceResponses.responses
import groovy.json.JsonSlurper
import io.netty.channel.ChannelHandler.Sharable
import _root_.unfiltered.netty.{ReceivedMessage, ServerErrorResponse, cycle}
import _root_.unfiltered.request.HttpRequest
import _root_.unfiltered.response.ResponseFunction

object TestService extends StrictLogging {
  var state: String = ""

  @Sharable
  case class RequestHandler(port: Int) extends cycle.Plan
    with cycle.SynchronousExecution
    with ServerErrorResponse {
      import io.netty.handler.codec.http.{HttpResponse => NHttpResponse}

    def parse(body: String): java.util.Map[Any, Any] = {
      new JsonSlurper().parseText(body).asInstanceOf[java.util.Map[Any, Any]]
    }

    def handle(request:HttpRequest[ReceivedMessage]): ResponseFunction[NHttpResponse] = {
        val response = if(request.uri.endsWith("enterState")) {
          val pactRequest: Request = Conversions.unfilteredRequestToPactRequest(request)
          val json = parse(pactRequest.getBody.getValue)
          state = json.get("state").toString
          new Response(200)
        } else {
          responses.get(state).flatMap(_.get(request.uri)).getOrElse(new Response(400))
        }
        Conversions.pactToUnfilteredResponse(response)
      }

      def intent = PartialFunction[HttpRequest[ReceivedMessage], ResponseFunction[NHttpResponse]](handle)
  }

  def apply(port:Int) = {
    val server = _root_.unfiltered.netty.Server.local(port).handler(RequestHandler(port))
    logger.info(s"starting unfiltered app at 127.0.0.1 on port $port")
    server.start()
    server
  }
}
