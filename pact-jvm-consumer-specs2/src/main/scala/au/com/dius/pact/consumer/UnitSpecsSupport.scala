package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.dsl.DslPart
import au.com.dius.pact.consumer.specs2.VerificationResultAsResult
import au.com.dius.pact.model._
import au.com.dius.pact.model.matchingrules.MatchingRules
import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragments

import scala.collection.JavaConverters._

trait UnitSpecsSupport extends Specification {

  def pactFragment: PactFragment

  protected lazy val pact = pactFragment.toPact
  protected val providerConfig = MockProviderConfig.createDefault(PactSpecVersion.V3)
  protected val server = DefaultMockProvider(providerConfig)
  protected val consumerPactRunner = new ConsumerPactRunner(server)

  override def map(fragments: => Fragments) = {
    step(server.start(pact)) ^
      fragments ^
      step(server.stop()) ^
      fragmentFactory.example(
        "Should match all mock server records",
        VerificationResultAsResult(consumerPactRunner.writePact(pact, PactSpecVersion.V3))
      )
  }

  def buildRequest(path: String,
                   method: String = "GET",
                   query: String = "",
                   headers: Map[String, String] = Map(),
                   body: String = "",
                   matchers: MatchingRules = new MatchingRules()): Request =
    new Request(method, path, PactReader.queryStringToMap(query), headers.asJava, OptionalBody.body(body), matchers)

  def buildResponse(status: Int = 200,
                    headers: Map[String, String] = Map(),
                    maybeBody: Option[String] = None,
                    matchers: MatchingRules = new MatchingRules()): Response = {
    val optionalBody = maybeBody match {
      case Some(body) => OptionalBody.body(body)
      case None => OptionalBody.missing()
    }

    new Response(status, headers.asJava, optionalBody, matchers)
  }

  def buildResponse(status: Int,
                    headers: Map[String, String],
                    bodyAndMatchers: DslPart): Response = {
    val matchers = new MatchingRules()
    matchers.addCategory(bodyAndMatchers.getMatchers)
    new Response(status, headers.asJava, OptionalBody.body(bodyAndMatchers.toString), matchers)
  }

  def buildInteraction(description: String, states: List[ProviderState], request: Request, response: Response): RequestResponseInteraction =
    new RequestResponseInteraction(description, states.asJava, request, response)

  def buildPactFragment(consumer: String, provider: String, interactions: List[RequestResponseInteraction]): PactFragment =
    new PactFragment(new Consumer(consumer), new Provider(provider), interactions)
}
