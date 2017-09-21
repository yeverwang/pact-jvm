package au.com.dius.pact.provider.groovysupport

import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.PactReader
import au.com.dius.pact.model.Request
import au.com.dius.pact.provider.HttpClientFactory
import au.com.dius.pact.provider.ProviderClient
import org.apache.http.Header
import org.apache.http.StatusLine
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.ContentType
import org.apache.http.impl.client.CloseableHttpClient
import org.junit.Before
import org.junit.Test
import org.mockito.invocation.InvocationOnMock

import static org.mockito.Matchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class ProviderClientTest {

  private ProviderClient client
  private Request request
  private provider
  private mockHttpClient
  private HttpUriRequest args
  private HttpClientFactory httpClientFactory

  @Before
  void setup() {
    provider = [
      protocol: 'http',
      host: 'localhost',
      port: 8080,
      path: '/'
    ]
    mockHttpClient = mock CloseableHttpClient
    httpClientFactory = [newClient: { provider -> mockHttpClient } ] as HttpClientFactory
    client = new ProviderClient(request: request, provider: provider, httpClientFactory: httpClientFactory)
    when(mockHttpClient.execute(any())).thenAnswer { InvocationOnMock invocation ->
      args = invocation.arguments.first()
      [
        getStatusLine: { [getStatusCode: { 200 } ] as StatusLine },
        getAllHeaders: { [] as Header[] },
        getEntity: { },
        close: { }
      ] as CloseableHttpResponse
    }
  }

  @Test
  void 'URL decodes the path'() {
    String path = '%2Fpath%2FTEST+PATH%2F2014-14-06+23%3A22%3A21'
    client.request = new Request('GET', path, [:], [:], OptionalBody.body(''))
    client.makeRequest()
    assert args.URI.path == '/path/TEST PATH/2014-14-06 23:22:21'
  }

  @Test
  void 'query parameters must be placed in the body for URL encoded FORM POSTs'() {
    client.request = new Request('POST', '/', PactReader.queryStringToMap('a=1&b=11&c=Hello World'),
      ['Content-Type': ContentType.APPLICATION_FORM_URLENCODED.toString()], OptionalBody.nullBody())
    client.makeRequest()
    assert args.params.parameters == [:]
    assert args.entity.content.text == 'a=1&b=11&c=Hello+World'
  }

}
