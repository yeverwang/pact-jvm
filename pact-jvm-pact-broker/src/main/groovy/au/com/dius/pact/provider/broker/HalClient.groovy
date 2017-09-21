package au.com.dius.pact.provider.broker

import au.com.dius.pact.pactbroker.HalClientBase
import au.com.dius.pact.pactbroker.NotFoundHalResponse
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import groovy.transform.Canonical
import groovy.util.logging.Slf4j
import groovyx.net.http.Method
import groovyx.net.http.RESTClient
import org.apache.http.util.EntityUtils

import java.util.function.BiFunction
import java.util.function.Consumer

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.PUT

/**
 * HAL client for navigating the HAL links
 */
@Slf4j
@Canonical
@SuppressWarnings('DuplicateStringLiteral')
class HalClient extends HalClientBase {

  /**
   * @deprecated Use httpClient from the base class
   */
  @Deprecated
  def http

  HalClient(
    String baseUrl,
    Map<String, ?> options) {
    super(baseUrl, options)
  }

  HalClient(String baseUrl) {
    super(baseUrl)
  }

  /**
   * @deprecated Use setupHttpClient from the base class
   */
  @SuppressWarnings('DuplicateNumberLiteral')
  @Deprecated
  private void setupRestClient() {
    if (http == null) {
      http = newHttpClient()
      if (options.authentication instanceof List) {
        switch (options.authentication.first().toLowerCase()) {
          case 'basic':
            if (options.authentication.size() > 2) {
              http.auth.basic(options.authentication[1].toString(), options.authentication[2].toString())
            } else {
              log.warn('Basic authentication requires a username and password, ignoring.')
            }
            break
        }
      } else if (options.authentication) {
        log.warn('Authentication options needs to be a list of values, ignoring.')
      }
    }
  }

  @Deprecated
  private RESTClient newHttpClient() {
    http = new RESTClient(baseUrl)
    http.parser.'application/hal+json' = http.parser.'application/json'
    http.handler.'404' = {
      throw new NotFoundHalResponse("404 Not Found response from the pact broker (URL: '${baseUrl}'," +
        " LINK: '${lastUrl}')")
    }
    http
  }

  def methodMissing(String name, args) {
    super.initPathInfo()
    JsonElement matchingLink = super.pathInfo['_links'][name]
    if (matchingLink != null) {
      if (args && args.last() instanceof Closure) {
        if (matchingLink.isJsonArray()) {
          return matchingLink.each(args.last() as Closure)
        }
        return args.last().call(matchingLink)
      }
      return matchingLink
    }
    throw new MissingMethodException(name, this.class, args)
  }

  @Override
  void forAll(String linkName, Consumer<Map<String, Object>> just) {
    super.initPathInfo()
    JsonElement matchingLink = pathInfo['_links'][linkName]
    if (matchingLink != null) {
      if (matchingLink.isJsonArray()) {
        matchingLink.asJsonArray.each { just.accept(fromJson(it)) }
      } else {
        just.accept(fromJson(matchingLink.asJsonObject))
      }
    }
  }

  static Map<String, Object> asMap(JsonObject jsonObject) {
    jsonObject.entrySet().collectEntries { Map.Entry<String, JsonElement> entry ->
      [entry.key, fromJson(entry.value)]
    }
  }

  static fromJson(JsonElement jsonValue) {
    if (jsonValue.jsonObject) {
      asMap(jsonValue.asJsonObject)
    } else if (jsonValue.jsonArray) {
      jsonValue.asJsonArray.collect { fromJson(it) }
    } else if (jsonValue.jsonNull) {
      null
    } else {
      JsonPrimitive primitive = jsonValue.asJsonPrimitive
      if (primitive.isBoolean()) {
        primitive.asBoolean
      } else if (primitive.isNumber()) {
        primitive.asBigDecimal
      } else {
        primitive.asString
      }
    }
  }

  String linkUrl(String name) {
    pathInfo.'_links'[name].href
  }

  def uploadJson(String path, String bodyJson, BiFunction<String, String, Object> closure = null) {
    setupRestClient()
    executeUpload(PUT, path, bodyJson, closure)
  }

  protected executeUpload(Method method, String path, String bodyJson,
                               BiFunction<String, String, Object> closure) {
    http.request(method) {
      uri.path = path
      body = bodyJson
      requestContentType = JSON

      response.success = { resp ->
        consumeEntity(resp)
        closure?.apply('OK', resp.statusLine as String)
      }

      response.failure = { resp, body -> handleFailure(resp, body, closure) }

      response.'409' = { resp, body ->
        closure?.apply('FAILED',
          "${resp.statusLine.statusCode} ${resp.statusLine.reasonPhrase} - ${body.readLine()}")
      }
    }
  }

  private static consumeEntity(resp) {
    EntityUtils.consume(resp.entity)
  }

  private static handleFailure(resp, body, BiFunction<String, String, Object> closure) {
    if (body instanceof Reader) {
      closure.apply('FAILED', "${resp.statusLine.statusCode} ${resp.statusLine.reasonPhrase} - ${body.readLine()}")
    } else {
      def error = 'Unknown error'
      if (body?.errors instanceof List) {
        error = body.errors.join(', ')
      } else if (body?.errors instanceof Map) {
        error = body.errors.collect { entry -> "${entry.key}: ${entry.value}" }.join(', ')
      }
      closure.apply('FAILED', "${resp.statusLine.statusCode} ${resp.statusLine.reasonPhrase} - ${error}")
    }
  }
}
