package au.com.dius.pact.matchers

import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.Request
import au.com.dius.pact.model.matchingrules.MatchingRules
import au.com.dius.pact.model.matchingrules.NumberTypeMatcher
import spock.lang.Specification

class TypeMatcherSpec extends Specification {

  private final Boolean allowUnexpectedKeys = true

  def 'match integers should accept integer values'() {
    given:
    def matchingRules = new MatchingRules()
    matchingRules.addCategory('body').addRule('$.value', new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))
    def expected = new Request('get', '/', null, null, OptionalBody.body('{"value": 123}'), matchingRules)
    def actual = new Request('get', '/', null, null, OptionalBody.body('{"value": 456}'), null)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, allowUnexpectedKeys)

    then:
    result.empty
  }

  def 'match integers should not match null values'() {
    given:
    def matchingRules = new MatchingRules()
    matchingRules.addCategory('body').addRule('$.value', new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))
    def expected = new Request('get', '/', null, null, OptionalBody.body('{"value": 123}'), matchingRules)
    def actual = new Request('get', '/', null, null, OptionalBody.body('{"value": null}'), null)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, allowUnexpectedKeys)

    then:
    !result.empty
  }

  def 'match integers should fail for non-integer values'() {
    given:
    def matchingRules = new MatchingRules()
    matchingRules.addCategory('body').addRule('$.value', new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))
    def expected = new Request('get', '/', null, null, OptionalBody.body('{"value": 123}'), matchingRules)
    def actual = new Request('get', '/', null, null, OptionalBody.body('{"value": 123.10}'), null)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, allowUnexpectedKeys)

    then:
    !result.empty
  }

  def 'match decimal should accept decimal values'() {
    given:
    def matchingRules = new MatchingRules()
    matchingRules.addCategory('body').addRule('$.value', new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL))
    def expected = new Request('get', '/', null, null, OptionalBody.body('{"value": 123.10}'), matchingRules)
    def actual = new Request('get', '/', null, null, OptionalBody.body('{"value": 456.20}'), null)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, allowUnexpectedKeys)

    then:
    result.empty
  }

  def 'match decimal should handle null values'() {
    given:
    def matchingRules = new MatchingRules()
    matchingRules.addCategory('body').addRule('$.value', new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL))
    def expected = new Request('get', '/', null, null, OptionalBody.body('{"value": 123.10}'), matchingRules)
    def actual = new Request('get', '/', null, null, OptionalBody.body('{"value": null}'), null)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, allowUnexpectedKeys)

    then:
    !result.empty
  }

  def 'match decimal should fail for non-decimal values'() {
    given:
    def matchingRules = new MatchingRules()
    matchingRules.addCategory('body').addRule('$.value', new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL))
    def expected = new Request('get', '/', null, null, OptionalBody.body('{"value": 123.10}'), matchingRules)
    def actual = new Request('get', '/', null, null, OptionalBody.body('{"value": 123}'), null)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, allowUnexpectedKeys)

    then:
    !result.empty
  }

}
