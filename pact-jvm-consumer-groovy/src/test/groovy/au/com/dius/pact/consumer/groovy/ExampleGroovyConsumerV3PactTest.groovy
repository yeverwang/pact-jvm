package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.PactConsumerConfig
import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.model.PactSpecVersion
import groovy.json.JsonSlurper
import groovyx.net.http.RESTClient
import org.junit.Test

import static java.util.TimeZone.getTimeZone

class ExampleGroovyConsumerV3PactTest {

    @Test
    void "example V3 spec test"() {

        def date = new Date()
        def aliceService = new PactBuilder()
        aliceService {
            serviceConsumer 'V3Consumer'
            hasPactWith 'V3Service'
        }

        aliceService {
            given('a provider state')
            given('another provider state', [valueA: 'A', valueB: 100])
            given('a third provider state', [valueC: date])
            uponReceiving('a retrieve Mallory request')
            withAttributes(method: 'get', path: '/mallory', query: [name: 'ron', status: 'good'])
            willRespondWith(
                status: 200,
                headers: ['Content-Type': 'text/html'],
                body: '"That is some good Mallory."'
            )
        }

        PactVerificationResult result = aliceService.runTest(specificationVersion: PactSpecVersion.V3) { mockServer ->
            def client = new RESTClient(mockServer.url)
            def aliceResponse = client.get(path: '/mallory', query: [status: 'good', name: 'ron'])

            assert aliceResponse.status == 200
            assert aliceResponse.contentType == 'text/html'

            def data = aliceResponse.data.text()
            assert data == '"That is some good Mallory."'
        }
        assert result == PactVerificationResult.Ok.INSTANCE

      def pactFile = new File("${PactConsumerConfig.pactRootDir()}/V3Consumer-V3Service.json")
      def json = new JsonSlurper().parse(pactFile)
      assert json.metadata['pact-specification'].version == '3.0.0'
      def providerStates = json.interactions.first().providerStates
      assert providerStates.size() == 3
      assert providerStates[0] == [name: 'a provider state']
      assert providerStates[1] == [name: 'another provider state', params: [valueA: 'A', valueB: 100]]
      assert providerStates[2] == [name: 'a third provider state',
                                   params: [valueC: date.format('yyyy-MM-dd\'T\'HH:mm:ssZ', getTimeZone('GMT'))]
      ]
    }
}
