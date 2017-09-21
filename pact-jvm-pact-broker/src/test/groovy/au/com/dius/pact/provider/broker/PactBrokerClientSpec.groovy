package au.com.dius.pact.provider.broker

import au.com.dius.pact.pactbroker.IHalClient
import au.com.dius.pact.pactbroker.NotFoundHalResponse
import au.com.dius.pact.provider.broker.com.github.kittinunf.result.Result
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('UnnecessaryGetter')
class PactBrokerClientSpec extends Specification {

  private PactBrokerClient pactBrokerClient
  private File pactFile
  private String pactContents

  def setup() {
    pactBrokerClient = new PactBrokerClient('http://localhost:8080')
    pactFile = File.createTempFile('pact', '.json')
    pactContents = '''
      {
          "provider" : {
              "name" : "Provider"
          },
          "consumer" : {
              "name" : "Foo Consumer"
          },
          "interactions" : []
      }
    '''
    pactFile.write pactContents
  }

  def 'when fetching consumers, sets the auth if there is any'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate(_, _) >> halClient
    halClient.forAll(_, _) >> { args -> args[1].accept([name: 'bob', href: 'http://bob.com/']) }

    def client = Spy(PactBrokerClient, constructorArgs: ['http://pactBrokerUrl']) {
      newHalClient() >> halClient
    }
    client.options.authentication = ['Basic', '1', '2']

    when:
    def consumers = client.fetchConsumers('provider')

    then:
    consumers != []
    consumers.first().name == 'bob'
    consumers.first().source == 'http://bob.com/'
    consumers.first().pactFileAuthentication == ['Basic', '1', '2']
  }

  def 'when fetching consumers for an unknown provider, returns an empty pacts list'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate(_, _) >> halClient
    halClient.forAll(_, _) >> { args -> throw new NotFoundHalResponse() }

    def client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumers('provider')

    then:
    consumers == []
  }

  def 'when fetching consumers, decodes the URLs to the pacts'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate(_, _) >> halClient
    halClient.forAll(_, _) >> { args -> args[1].accept([name: 'bob', href: 'http://bob.com/a%20b']) }

    def client = Spy(PactBrokerClient, constructorArgs: ['http://pactBrokerUrl']) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumers('provider')

    then:
    consumers != []
    consumers.first().name == 'bob'
    consumers.first().source == 'http://bob.com/a b'
  }

  def 'fetches consumers with specified tag successfully'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate(_, _) >> halClient
    halClient.forAll(_, _) >> { args -> args[1].accept([name: 'bob', href: 'http://bob.com/']) }

    def client = Spy(PactBrokerClient, constructorArgs: ['http://pactBrokerUrl']) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumersWithTag('provider', 'tag')

    then:
    consumers != []
    consumers.first().name == 'bob'
    consumers.first().source == 'http://bob.com/'
  }

  def 'when fetching consumers with specified tag, sets the auth if there is any'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate(_, _) >> halClient
    halClient.forAll(_, _) >> { args -> args[1].accept([name: 'bob', href: 'http://bob.com/']) }

    def client = Spy(PactBrokerClient, constructorArgs: ['http://pactBrokerUrl']) {
      newHalClient() >> halClient
    }
    client.options.authentication = ['Basic', '1', '2']

    when:
    def consumers = client.fetchConsumersWithTag('provider', 'tag')

    then:
    consumers.first().pactFileAuthentication == ['Basic', '1', '2']
  }

  def 'when fetching consumers with specified tag, decodes the URLs to the pacts'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate(_, _) >> halClient
    halClient.forAll(_, _) >> { args -> args[1].accept([name: 'bob', href: 'http://bob.com/a%20b']) }

    def client = Spy(PactBrokerClient, constructorArgs: ['http://pactBrokerUrl']) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumersWithTag('provider', 'tag')

    then:
    consumers != []
    consumers.first().name == 'bob'
    consumers.first().source == 'http://bob.com/a b'
  }

  def 'when fetching consumers with specified tag for an unknown provider, returns an empty pacts list'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate(_, _) >> halClient
    halClient.forAll(_, _) >> { args -> throw new NotFoundHalResponse() }

    def client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumersWithTag('provider', 'tag')

    then:
    consumers == []
  }

  def 'returns an error when uploading a pact fails'() {
    given:
    def halClient = Mock(IHalClient)
    def client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }

    when:
    def result = client.uploadPactFile(pactFile, '10.0.0')

    then:
    1 * halClient.uploadJson('/pacts/provider/Provider/consumer/Foo Consumer/version/10.0.0', pactContents, _) >>
      { args -> args[2].apply('Failed', 'Error') }
    result == 'FAILED! Error'
  }

  @Unroll
  def 'when publishing verification results, return a #result if #reason'() {
    given:
    def halClient = Mock(IHalClient)
    def client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }
    halClient.postJson('URL', _) >> new Result.Success(true)

    expect:
    client.publishVerificationResults(attributes, true, '0', null).class.simpleName == result

    where:

    reason                              | attributes                                         | result
    'there is no verification link'     | [:]                                                | Result.Failure.simpleName
    'the verification link has no href' | ['pb:publish-verification-results': [:]]           | Result.Failure.simpleName
    'the broker client returns success' | ['pb:publish-verification-results': [href: 'URL']] | Result.Success.simpleName
    'the links have different case'     | ['pb:Publish-Verification-Results': [HREF: 'URL']] | Result.Success.simpleName
  }

  def 'when fetching a pact, return the results as a Map'() {
    given:
    def halClient = Mock(IHalClient)
    def client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }
    def url = 'https://test.pact.dius.com.au' +
      '/pacts/provider/Activity%20Service/consumer/Foo%20Web%20Client%202/version/1.0.2'
    def json = new JsonObject()
    json.addProperty('a', 'a')
    json.addProperty('b', 100)
    json.add('_links', new JsonObject())
    def array = new JsonArray()
    array.with {
      it.add(true)
      it.add(10.2)
      it.add('test')
    }
    json.add('c', array)

    when:
    def result = client.fetchPact(url)

    then:
    1 * halClient.fetch(url) >> json
    result.pactFile == [a: 'a', b: 100, _links: [:], c: [true, 10.2, 'test']]
  }
}
