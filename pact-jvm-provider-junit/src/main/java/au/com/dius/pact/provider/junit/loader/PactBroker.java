package au.com.dius.pact.provider.junit.loader;

import au.com.dius.pact.provider.junit.PactRunner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to point {@link PactRunner} to source of pacts for contract tests
 * Default values can be set by setting the `pactbroker.*` system properties
 *
 * @see PactBrokerLoader pact loader
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@PactSource(PactBrokerLoader.class)
public @interface PactBroker {
    /**
     * @return host of pact broker
     */
    String host() default "${pactbroker.host:}";

    /**
     * @return port of pact broker
     */
    String port() default "${pactbroker.port:}";

    /**
     * HTTP protocol, defaults to http
     */
    String protocol() default "${pactbroker.protocol:http}";

    /**
     * Tags to use to fetch pacts for, defaults to `latest`
     * If you set the tags through the `pactbroker.tag` system property, separate the tags by commas
     */
    String[] tags() default "${pactbroker.tags:latest}";

  /**
   * If the test should fail if no pacts are found for the provider, default is true
   * @deprecated Use a @IgnoreNoPactsToVerify annotation on the test class instead
   */
  @Deprecated
  boolean failIfNoPactsFound() default true;

  /**
   * Authentication to use with the pact broker, by default no authentication is used
   */
  PactBrokerAuth authentication() default @PactBrokerAuth(scheme = "${pactbroker.auth.scheme:basic}", username = "${pactbroker.auth.username:}", password = "${pactbroker.auth.password:}");
}
