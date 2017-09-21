package au.com.dius.pact.consumer.v3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.MessagePactProviderRule;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.model.v3.messaging.MessagePact;
import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;


public class ExampleMessageWithMetadataConsumerTest {

    @Rule
    public MessagePactProviderRule mockProvider = new MessagePactProviderRule(this);

    @Pact(provider = "test_provider", consumer = "test_consumer_v3")
    public MessagePact createPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringValue("testParam1", "value1");
        body.stringValue("testParam2", "value2");

        Map<String, String> metadata = new HashMap<>();
        metadata.put("metadata1", "metadataValue1");
        metadata.put("metadata2", "metadataValue2");

        return builder.given("SomeProviderState")
                .expectsToReceive("a test message with metadata")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Test
    @PactVerification({"test_provider", "SomeProviderState"})
    public void test() throws Exception {
        assertNotNull(mockProvider.getMessage());
        assertNotNull(mockProvider.getMetadata());
        assertEquals("metadataValue1", mockProvider.getMetadata().get("metadata1"));
        assertEquals("metadataValue2", mockProvider.getMetadata().get("metadata2"));
    }

}
