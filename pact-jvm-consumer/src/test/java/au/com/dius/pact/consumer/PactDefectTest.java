package au.com.dius.pact.consumer;

import au.com.dius.pact.model.MockProviderConfig;
import au.com.dius.pact.model.PactFragment;
import au.com.dius.pact.model.PactSpecVersion;
import au.com.dius.pact.model.RequestResponsePact;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest;
import static org.junit.Assert.assertEquals;

public class PactDefectTest {
    private static final String method = "POST";
    private static final String path = "/ping";

    @Test
    public void json() {
        test("{\"msg\" : \"ping\"}", "{\"msg\":\"pong\"}", "application/json");
    }

    @Test
    public void jsonWithCharset() {
        test("{\"msg\" : \"ping\"}", "{\"msg\":\"pong\"}", "application/json; charset=utf8");
    }

    @Test
    public void xml() {
        test("<ping />", "<pong />", "application/xml");
    }

    @Test
    public void xmlWithCharset() {
        test("<ping />", "<pong />", "application/xml; charset=utf-8");
    }

    @Test
    public void plain() {
        test("ping", "pong", "text/plain");
    }

    @Test
    public void plainWithCharset() {
        test("ping", "pong", "text/plain; charset=utf-8");
    }

    private void test(final String requestBody, final String expectedResponseBody, final String contentType) {

        RequestResponsePact pact = ConsumerPactBuilder
            .consumer("ping_consumer")
            .hasPactWith("ping_provider")
            .uponReceiving("Ping with " + contentType)
            .path(path)
            .method(method)
            .body(requestBody, contentType)
            .willRespondWith()
            .status(200)
            .body(expectedResponseBody)
            .toPact();

        PactVerificationResult result = runConsumerTest(pact, new MockProviderConfig("localhost", 0, PactSpecVersion.V3), new PactTestRun() {
            @Override
            public void run(@NotNull MockServer mockServer) throws IOException {
              try {
                  URL url = new URL(mockServer.getUrl() + path);
                  String response = post(url, contentType, requestBody);
                  assertEquals(expectedResponseBody, response);
              } catch (Exception e) {
                  throw new RuntimeException(e);
              }
            }
        });

      if (result instanceof PactVerificationResult.Error) {
        throw new RuntimeException(((PactVerificationResult.Error)result).getError());
      }

      Assert.assertEquals(PactVerificationResult.Ok.INSTANCE, result);
    }

    private String post(URL url, String contentType, String requestBody) {
        try {
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("POST");
            http.setRequestProperty("Content-Type", contentType);
            http.setDoOutput(true);
            http.setDoInput(true);
            DataOutputStream wr = new DataOutputStream(http.getOutputStream());
            wr.writeBytes(requestBody);
            wr.flush();
            wr.close();

            BufferedReader in = new BufferedReader(
                new InputStreamReader(http.getInputStream()));
            String inputLine;
            StringBuilder httpResponse = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                httpResponse.append(inputLine);
            }
            in.close();
            return httpResponse.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);

        }
    }
}
