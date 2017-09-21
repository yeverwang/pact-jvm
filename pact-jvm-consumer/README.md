Pact consumer
=============

Pact Consumer is used by projects that are consumers of an API.

Most projects will want to use pact-consumer via one of the test framework specific projects. If your favourite
framework is not implemented, this module should give you all the hooks you need.

Provides a DSL for use with Java to build consumer pacts.

## Dependency

The library is available on maven central using:

* group-id = `au.com.dius`
* artifact-id = `pact-jvm-consumer_2.11`

## DSL Usage

Example in a JUnit test:

```java
import au.com.dius.pact.model.MockProviderConfig;
import au.com.dius.pact.model.RequestResponsePact;
import org.apache.http.entity.ContentType;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest;
import static org.junit.Assert.assertEquals;

public class PactTest {

  @Test
  public void testPact() {
    RequestResponsePact pact = ConsumerPactBuilder
      .consumer("Some Consumer")
      .hasPactWith("Some Provider")
      .uponReceiving("a request to say Hello")
      .path("/hello")
      .method("POST")
      .body("{\"name\": \"harry\"}")
      .willRespondWith()
      .status(200)
      .body("{\"hello\": \"harry\"}")
      .toPact();

    MockProviderConfig config = MockProviderConfig.createDefault();
    PactVerificationResult result = runConsumerTest(pact, config, new PactTestRun() {
      @Override
      public void run(@NotNull MockServer mockServer) throws IOException {
        Map expectedResponse = new HashMap();
        expectedResponse.put("hello", "harry");
        assertEquals(expectedResponse, new ConsumerClient(mockServer.getUrl()).post("/hello",
            "{\"name\": \"harry\"}", ContentType.APPLICATION_JSON));
      }
    });

    if (result instanceof PactVerificationResult.Error) {
      throw new RuntimeException(((PactVerificationResult.Error)result).getError());
    }

    assertEquals(PactVerificationResult.Ok.INSTANCE, result);
  }

}
```

The DSL has the following pattern:

```java
.consumer("Some Consumer")
.hasPactWith("Some Provider")
.given("a certain state on the provider")
    .uponReceiving("a request for something")
        .path("/hello")
        .method("POST")
        .body("{\"name\": \"harry\"}")
    .willRespondWith()
        .status(200)
        .body("{\"hello\": \"harry\"}")
    .uponReceiving("another request for something")
        .path("/hello")
        .method("POST")
        .body("{\"name\": \"harry\"}")
    .willRespondWith()
        .status(200)
        .body("{\"hello\": \"harry\"}")
    .
    .
    .
.toPact()
```

You can define as many interactions as required. Each interaction starts with `uponReceiving` followed by `willRespondWith`.
The test state setup with `given` is a mechanism to describe what the state of the provider should be in before the provider
is verified. It is only recorded in the consumer tests and used by the provider verification tasks.

### Building JSON bodies with PactDslJsonBody DSL

The body method of the ConsumerPactBuilder can accept a PactDslJsonBody, which can construct a JSON body as well as
define regex and type matchers.

For example:

```java
PactDslJsonBody body = new PactDslJsonBody()
    .stringType("name")
    .booleanType("happy")
    .hexValue("hexCode")
    .id()
    .ipAddress("localAddress")
    .numberValue("age", 100)
    .timestamp();
```

#### DSL Matching methods

The following matching methods are provided with the DSL. In most cases, they take an optional value parameter which
will be used to generate example values (i.e. when returning a mock response). If no example value is given, a random
one will be generated.

| method | description |
|--------|-------------|
| string, stringValue | Match a string value (using string equality) |
| number, numberValue | Match a number value (using Number.equals)\* |
| booleanValue | Match a boolean value (using equality) |
| stringType | Will match all Strings |
| numberType | Will match all numbers\* |
| integerType | Will match all numbers that are integers (both ints and longs)\* |
| decimalType | Will match all real numbers (floating point and decimal)\* |
| booleanType | Will match all boolean values (true and false) |
| stringMatcher | Will match strings using the provided regular expression |
| timestamp | Will match string containing timestamps. If a timestamp format is not given, will match an ISO timestamp format |
| date | Will match string containing dates. If a date format is not given, will match an ISO date format |
| time | Will match string containing times. If a time format is not given, will match an ISO time format |
| ipAddress | Will match string containing IP4 formatted address. |
| id | Will match all numbers by type |
| hexValue | Will match all hexadecimal encoded strings |
| uuid | Will match strings containing UUIDs |
| includesStr | Will match strings containing the provided string |
| equalsTo | Will match using equals |
| matchUrl | Defines a matcher for URLs, given the base URL path and a sequence of path fragments. The path fragments could be
             strings or regular expression matchers |

_\* Note:_ JSON only supports double precision floating point values. Depending on the language implementation, they
may parsed as integer, floating point or decimal numbers.

#### Ensuring all items in a list match an example (2.2.0+)

Lots of the time you might not know the number of items that will be in a list, but you want to ensure that the list
has a minimum or maximum size and that each item in the list matches a given example. You can do this with the `arrayLike`,
`minArrayLike` and `maxArrayLike` functions.

| function | description |
|----------|-------------|
| `eachLike` | Ensure that each item in the list matches the provided example |
| `maxArrayLike` | Ensure that each item in the list matches the provided example and the list is no bigger than the provided max |
| `minArrayLike` | Ensure that each item in the list matches the provided example and the list is no smaller than the provided min |

For example:

```java
    DslPart body = new PactDslJsonBody()
        .minArrayLike("users")
            .id()
            .stringType("name")
        .closeObject()
        .closeArray();
```

This will ensure that the users list is never empty and that each user has an identifier that is a number and a name that is a string.


#### Matching JSON values at the root (Version 3.2.2/2.4.3+)

For cases where you are expecting basic JSON values (strings, numbers, booleans and null) at the root level of the body
and need to use matchers, you can use the `PactDslJsonRootValue` class. It has all the DSL matching methods for basic
values that you can use.

For example:

```java
.consumer("Some Consumer")
.hasPactWith("Some Provider")
    .uponReceiving("a request for a basic JSON value")
        .path("/hello")
    .willRespondWith()
        .status(200)
        .body(PactDslJsonRootValue.integerType())
```

#### Root level arrays that match all items (version 2.2.11+)

If the root of the body is an array, you can create PactDslJsonArray classes with the following methods:

| function | description |
|----------|-------------|
| `arrayEachLike` | Ensure that each item in the list matches the provided example |
| `arrayMinLike` | Ensure that each item in the list matches the provided example and the list is no bigger than the provided max |
| `arrayMaxLike` | Ensure that each item in the list matches the provided example and the list is no smaller than the provided min |

For example:

```java
PactDslJsonArray.arrayEachLike()
    .date("clearedDate", "mm/dd/yyyy", date)
    .stringType("status", "STATUS")
    .decimalType("amount", 100.0)
.closeObject()
```

This will then match a body like:

```json
[ {
  "clearedDate" : "07/22/2015",
  "status" : "C",
  "amount" : 15.0
}, {
  "clearedDate" : "07/22/2015",
  "status" : "C",
  "amount" : 15.0
}, {

  "clearedDate" : "07/22/2015",
  "status" : "C",
  "amount" : 15.0
} ]
```

#### Matching arrays of arrays (version 3.2.12/2.4.14+)

For the case where you have arrays of arrays (GeoJSON is an example), the following methods have been provided:

| function | description |
|----------|-------------|
| `eachArrayLike` | Ensure that each item in the array is an array that matches the provided example |
| `eachArrayWithMaxLike` | Ensure that each item in the array is an array that matches the provided example and the array is no bigger than the provided max |
| `eachArrayWithMinLike` | Ensure that each item in the array is an array that matches the provided example and the array is no smaller than the provided min |

For example (with GeoJSON structure):

```java
new PactDslJsonBody()
  .stringType("type","FeatureCollection")
  .eachLike("features")
    .stringType("type","Feature")
    .object("geometry")
      .stringType("type","Point")
      .eachArrayLike("coordinates") // coordinates is an array of arrays 
        .decimalType(-7.55717)
        .decimalType(49.766896)
      .closeArray()
      .closeArray()
    .closeObject()
    .object("properties")
      .stringType("prop0","value0")
    .closeObject()
  .closeObject()
  .closeArray()
```

This generated the following JSON:

```json
{
  "features": [
    {
      "geometry": {
        "coordinates": [[-7.55717, 49.766896]],
        "type": "Point"
      },
      "type": "Feature",
      "properties": { "prop0": "value0" }
    }
  ],
  "type": "FeatureCollection"
}
```

and will be able to match all coordinates regardless of the number of coordinates.

#### Matching any key in a map (3.3.1/2.5.0+)

The DSL has been extended for cases where the keys in a map are IDs. For an example of this, see 
[#313](https://github.com/DiUS/pact-jvm/issues/131). In this case you can use the `eachKeyLike` method, which takes an 
example key as a parameter.

For example:

```java
DslPart body = new PactDslJsonBody()
  .object("one")
    .eachKeyLike("001", PactDslJsonRootValue.id(12345L)) // key like an id mapped to a matcher
  .closeObject()
  .object("two")
    .eachKeyLike("001-A") // key like an id where the value is matched by the following example
      .stringType("description", "Some Description")
    .closeObject()
  .closeObject()
  .object("three")
    .eachKeyMappedToAnArrayLike("001") // key like an id mapped to an array where each item is matched by the following example
      .id("someId", 23456L)
      .closeObject()
    .closeArray()
  .closeObject();

```

For an example, have a look at [WildcardKeysTest](src/test/java/au/com/dius/pact/consumer/WildcardKeysTest.java).

**NOTE:** The `eachKeyLike` method adds a `*` to the matching path, so the matching definition will be applied to all keys
 of the map if there is not a more specific matcher defined for a particular key. Having more than one `eachKeyLike` condition
 applied to a map will result in only one being applied when the pact is verified (probably the last).

### Matching on paths (version 2.1.5+)

You can use regular expressions to match incoming requests. The DSL has a `matchPath` method for this. You can provide
a real path as a second value to use when generating requests, and if you leave it out it will generate a random one
from the regular expression.

For example:

```java
  .given("test state")
    .uponReceiving("a test interaction")
        .matchPath("/transaction/[0-9]+") // or .matchPath("/transaction/[0-9]+", "/transaction/1234567890")
        .method("POST")
        .body("{\"name\": \"harry\"}")
    .willRespondWith()
        .status(200)
        .body("{\"hello\": \"harry\"}")
```

### Matching on headers (version 2.2.2+)

You can use regular expressions to match request and response headers. The DSL has a `matchHeader` method for this. You can provide
an example header value to use when generating requests and responses, and if you leave it out it will generate a random one
from the regular expression.

For example:

```java
  .given("test state")
    .uponReceiving("a test interaction")
        .path("/hello")
        .method("POST")
        .matchHeader("testreqheader", "test.*value")
        .body("{\"name\": \"harry\"}")
    .willRespondWith()
        .status(200)
        .body("{\"hello\": \"harry\"}")
        .matchHeader("Location", ".*/hello/[0-9]+", "/hello/1234")
```

### Matching on query parameters (version 3.3.7+)

You can use regular expressions to match request query parameters. The DSL has a `matchQuery` method for this. You can provide
an example value to use when generating requests, and if you leave it out it will generate a random one
from the regular expression.

For example:

```java
  .given("test state")
    .uponReceiving("a test interaction")
        .path("/hello")
        .method("POST")
        .matchQuery("a", "\\d+", "100")
        .matchQuery("b", "[A-Z]", "X")
        .body("{\"name\": \"harry\"}")
    .willRespondWith()
        .status(200)
        .body("{\"hello\": \"harry\"}")
```
