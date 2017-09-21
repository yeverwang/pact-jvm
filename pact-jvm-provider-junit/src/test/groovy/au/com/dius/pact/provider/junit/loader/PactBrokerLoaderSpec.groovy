package au.com.dius.pact.provider.junit.loader

import au.com.dius.pact.model.Pact
import au.com.dius.pact.pactbroker.InvalidHalResponse
import au.com.dius.pact.pactbroker.PactBrokerConsumer
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.broker.PactBrokerClient
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

import static au.com.dius.pact.provider.junit.sysprops.PactRunnerExpressionParser.VALUES_SEPARATOR

@PactBroker(host = 'pactbroker.host', port = '1000', failIfNoPactsFound = false)
class PactBrokerLoaderSpec extends Specification {

  private Closure<PactBrokerLoader> pactBrokerLoader
  private String host
  private String port
  private String protocol
  private List tags
  private PactBrokerClient brokerClient
  private Pact mockPact

  void setup() {
    host = 'pactbroker'
    port = '1234'
    protocol = 'http'
    tags = ['latest']
    brokerClient = Mock(PactBrokerClient)
    mockPact = Mock(Pact)

    pactBrokerLoader = { boolean failIfNoPactsFound = true ->
      def loader = new PactBrokerLoader(host, port, protocol, tags) {
        @Override
        PactBrokerClient newPactBrokerClient(URI url) throws URISyntaxException {
          brokerClient
        }

        @Override
        Pact loadPact(ConsumerInfo consumer, Map options) {
          mockPact
        }
      }
      loader.failIfNoPactsFound = failIfNoPactsFound
      loader
    }
  }

  def 'Raises an exception if the pact broker client returns an empty list'() {
    when:
    pactBrokerLoader().load('test')

    then:
    1 * brokerClient.fetchConsumers('test') >> []
    thrown(NoPactsFoundException)
  }

  def 'Returns Empty List if flagged to do so and the pact broker client returns an empty list'() {
    when:
    def result = pactBrokerLoader(false).load('test')

    then:
    1 * brokerClient.fetchConsumers('test') >> []
    result == []
  }

  def 'Throws any Exception On Execution Exception'() {
    given:
    brokerClient.fetchConsumers('test') >> { throw new InvalidHalResponse('message') }

    when:
    pactBrokerLoader().load('test')

    then:
    thrown(InvalidHalResponse)
  }

  def 'Throws an Exception if the broker URL is invalid'() {
    given:
    host = '!@#%$^%$^^'

    when:
    pactBrokerLoader().load('test')

    then:
    thrown(IOException)
  }

  void 'Loads Pacts Configured From A Pact Broker Annotation'() {
    given:
    pactBrokerLoader = {
      new PactBrokerLoader(this.class.getAnnotation(PactBroker)) {
        @Override
        PactBrokerClient newPactBrokerClient(URI url) throws URISyntaxException {
          assert url.host == 'pactbroker.host'
          assert url.port == 1000
          brokerClient
        }
      }
    }

    when:
    def result = pactBrokerLoader().load('test')

    then:
    result == []
    1 * brokerClient.fetchConsumers('test') >> []
  }

  def 'Loads pacts for each provided tag'() {
    given:
    tags = ['latest', 'a', 'b', 'c']

    when:
    def result = pactBrokerLoader().load('test')

    then:
    1 * brokerClient.fetchConsumersWithTag('test', 'latest') >> [ new PactBrokerConsumer('test', 'latest', '', []) ]
    1 * brokerClient.fetchConsumersWithTag('test', 'a') >> [ new PactBrokerConsumer('test', 'a', '', []) ]
    1 * brokerClient.fetchConsumersWithTag('test', 'b') >> [ new PactBrokerConsumer('test', 'b', '', []) ]
    1 * brokerClient.fetchConsumersWithTag('test', 'c') >> [ new PactBrokerConsumer('test', 'c', '', []) ]
    result.size() == 4
  }

  @RestoreSystemProperties
  @SuppressWarnings('GStringExpressionWithinString')
  def 'Processes tags before pact load'() {
    given:
    System.setProperty('composite', "one${VALUES_SEPARATOR}two")
    tags = ['${composite}']

    when:
    def result = pactBrokerLoader().load('test')

    then:
    1 * brokerClient.fetchConsumersWithTag('test', 'one') >> [ new PactBrokerConsumer('test', 'one', '', []) ]
    1 * brokerClient.fetchConsumersWithTag('test', 'two') >> [ new PactBrokerConsumer('test', 'two', '', []) ]
    result.size() == 2
  }

  def 'Loads the latest pacts if no tag is provided'() {
    given:
    tags = []

    when:
    def result = pactBrokerLoader().load('test')

    then:
    result.size() == 1
    1 * brokerClient.fetchConsumers('test') >> [ new PactBrokerConsumer('test', 'latest', '', []) ]
  }

}
