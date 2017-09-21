package au.com.dius.pact.consumer.pactproviderrule;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRuleMk2;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.exampleclients.ConsumerClient;
import au.com.dius.pact.model.RequestResponsePact;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PactProviderWithMultipleFragmentsTest {

    @Rule
    public PactProviderRuleMk2 mockTestProvider = new PactProviderRuleMk2("test_provider", this);

    @Pact(consumer="test_consumer")
    public RequestResponsePact createFragment(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("testreqheader", "testreqheadervalue");

        return builder
            .given("good state")
            .uponReceiving("PactProviderTest test interaction")
                .path("/")
                .method("GET")
                .headers(headers)
            .willRespondWith()
                .status(200)
                .headers(headers)
                .body("{\"responsetest\": true, \"name\": \"harry\"}")
            .uponReceiving("PactProviderTest second test interaction")
                .method("OPTIONS")
                .headers(headers)
                .path("/second")
                .body("")
            .willRespondWith()
                .status(200)
                .headers(headers)
                .body("")
            .toPact();
    }

    @Pact(consumer="test_consumer")
    public RequestResponsePact createFragment2(PactDslWithProvider builder) {
        return builder
                .given("good state")
                .uponReceiving("PactProviderTest test interaction 2")
                    .path("/")
                    .method("GET")
                .willRespondWith()
                    .status(200)
                    .body("{\"responsetest\": true, \"name\": \"fred\"}")
                .toPact();
    }

    @Test
    @PactVerification(value = "test_provider", fragment = "createFragment2")
    public void runTestWithFragment2() throws IOException {
        Map expectedResponse = new HashMap();
        expectedResponse.put("responsetest", true);
        expectedResponse.put("name", "fred");
        assertEquals(new ConsumerClient(mockTestProvider.getUrl()).getAsMap("/", ""), expectedResponse);
    }

    @Test
    @PactVerification(value = "test_provider", fragment = "createFragment")
    public void runTestWithFragment1() throws IOException {
        Assert.assertEquals(new ConsumerClient(mockTestProvider.getUrl()).options("/second"), 200);
        Map expectedResponse = new HashMap();
        expectedResponse.put("responsetest", true);
        expectedResponse.put("name", "harry");
        assertEquals(new ConsumerClient(mockTestProvider.getUrl()).getAsMap("/", ""), expectedResponse);
    }
}
