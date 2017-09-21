package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.model.MockProviderConfig;
import au.com.dius.pact.model.Pact;
import au.com.dius.pact.model.PactSpecVersion;
import au.com.dius.pact.model.RequestResponsePact;
import org.apache.http.entity.ContentType;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest;
import static org.junit.Assert.assertEquals;

public class MimeTypeTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MimeTypeTest.class);

    @Test
    public void testMatchingJson() {
        String body = new PactDslJsonBody()
            .object("person")
                .stringValue("name", "fred")
                .numberValue("age", 100)
            .closeObject()
            .toString();

        String responseBody = "{\"status\":\"OK\"}";

        runTest(buildPact(body, responseBody, "a test interaction with json", ContentType.APPLICATION_JSON),
            body, responseBody, ContentType.APPLICATION_JSON);
    }

    @Test
    public void testMatchingText() {
        String newLine = System.lineSeparator();
        String body = "Define a pact between service consumers and providers, enabling \"consumer driven contract\" testing." + newLine +
            newLine + "Pact provides an RSpec DSL for service consumers to define the HTTP requests they will make to a service" +
            " provider and the HTTP responses they expect back. These expectations are used in the consumers specs " +
            "to provide a mock service provider. The interactions are recorded, and played back in the service " +
            "provider specs to ensure the service provider actually does provide the response the consumer expects.";

        String responseBody = "status=OK";

        runTest(buildPact(body, responseBody, "a test interaction with text", ContentType.TEXT_PLAIN),
            body, responseBody, ContentType.TEXT_PLAIN);
    }

    @Test
    public void testMatchingXml() {
        String body = "<?xml version=\"1.0\"?>\n<someXml></someXml>";

        String responseBody = "<status>OK</status>";

        runTest(buildPact(body, responseBody, "a test interaction with xml", ContentType.APPLICATION_XML),
            body, responseBody, ContentType.APPLICATION_XML);
    }

    private void runTest(RequestResponsePact pact, final String body, final String expectedResponse, final ContentType mimeType) {
        MockProviderConfig config = MockProviderConfig.createDefault(PactSpecVersion.V3);
        PactVerificationResult result = runConsumerTest(pact, config, new PactTestRun() {
            @Override
            public void run(@NotNull MockServer mockServer) throws IOException {
                try {
                    assertEquals(new ConsumerClient(config.url()).postBody("/hello", body, mimeType), expectedResponse);
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        });

        if (result instanceof PactVerificationResult.Error) {
            throw new RuntimeException(((PactVerificationResult.Error)result).getError());
        }

        Assert.assertEquals(PactVerificationResult.Ok.INSTANCE, result);
    }

    private RequestResponsePact buildPact(String body, String responseBody, String description, ContentType contentType) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", contentType.toString());
        return ConsumerPactBuilder
            .consumer("test_consumer")
            .hasPactWith("test_provider")
            .uponReceiving(description)
                .path("/hello")
                .method("POST")
                .body(body)
                .headers(headers)
            .willRespondWith()
                .status(200)
                .body(responseBody)
                .headers(headers)
                .toPact();
    }

}
