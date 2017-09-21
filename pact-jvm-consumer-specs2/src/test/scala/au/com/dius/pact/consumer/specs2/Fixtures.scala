package au.com.dius.pact.consumer.specs2

import java.util
import scala.collection.JavaConverters._
import au.com.dius.pact.model.{Consumer, Provider, RequestResponseInteraction, _}

object Fixtures {

  val provider = new Provider("test_provider")
  val consumer = new Consumer("test_consumer")

  val request = new Request("POST", "/", PactReader.queryStringToMap("q=p"),
    Map("testreqheader" -> "testreqheadervalue").asInstanceOf[java.util.Map[String, String]],
    OptionalBody.body("{\"test\": true}"))

  val response = new Response(200,
    Map("testreqheader" -> "testreqheaderval", "Access-Control-Allow-Origin" -> "*").asInstanceOf[java.util.Map[String, String]],
    OptionalBody.body("{\"responsetest\": true}"))

  val interaction = new RequestResponseInteraction("test interaction",
    Seq(new ProviderState("test state")).asJava, request, response)

  val pact: RequestResponsePact = new RequestResponsePact(provider, consumer, util.Arrays.asList(interaction))
}
