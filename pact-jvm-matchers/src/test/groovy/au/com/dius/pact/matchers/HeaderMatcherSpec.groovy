package au.com.dius.pact.matchers

import au.com.dius.pact.model.matchingrules.MatchingRules
import au.com.dius.pact.model.matchingrules.RegexMatcher
import scala.None$
import scala.collection.JavaConversions
import spock.lang.Specification

class HeaderMatcherSpec extends Specification {

  def "matching headers - be true when headers are equal"() {
    expect:
    HeaderMatcher.compareHeader('HEADER', 'HEADER', 'HEADER',
      new MatchingRules()) == None$.MODULE$
  }

  def "matching headers - be false when headers are not equal"() {
    expect:
    HeaderMatcher.compareHeader('HEADER', 'HEADER', 'HEADSER',
      new MatchingRules()) != None$.MODULE$
  }

  def "matching headers - exclude whitespace from the comparison"() {
    expect:
    HeaderMatcher.compareHeader('HEADER', 'HEADER1, HEADER2,   3', 'HEADER1,HEADER2,3',
      new MatchingRules()) == None$.MODULE$
  }

  def "matching headers - delegate to a matcher when one is defined"() {
    given:
    def matchers = new MatchingRules()
    matchers.addCategory('header').addRule('HEADER', new RegexMatcher('.*'))

    expect:
    HeaderMatcher.compareHeader('HEADER', 'HEADER', 'XYZ', matchers) == None$.MODULE$
  }

  def "matching headers - content type header - be true when headers are equal"() {
    expect:
    HeaderMatcher.compareHeader('CONTENT-TYPE', 'application/json;charset=UTF-8',
      'application/json; charset=UTF-8', new MatchingRules()) == None$.MODULE$
  }

  def "matching headers - content type header - be false when headers are not equal"() {
    expect:
    HeaderMatcher.compareHeader('CONTENT-TYPE', 'application/json;charset=UTF-8',
      'application/pdf;charset=UTF-8', new MatchingRules()) != None$.MODULE$
  }

  def "matching headers - content type header - be false when charsets are not equal"() {
    expect:
    HeaderMatcher.compareHeader('CONTENT-TYPE', 'application/json;charset=UTF-8',
      'application/json;charset=UTF-16', new MatchingRules()) != None$.MODULE$
  }

  def "matching headers - content type header - be false when other parameters are not equal"() {
    expect:
    HeaderMatcher.compareHeader('CONTENT-TYPE', 'application/json;declaration="<950118.AEB0@XIson.com>"',
      'application/json;charset=UTF-8', new MatchingRules()) != None$.MODULE$
  }

  def "matching headers - content type header - be true when the charset is missing from the expected header"() {
    expect:
    HeaderMatcher.compareHeader('CONTENT-TYPE', 'application/json',
      'application/json ; charset=UTF-8', new MatchingRules()) == None$.MODULE$
  }

  def "matching headers - content type header - delegate to any defined matcher"() {
    given:
    def matchers = new MatchingRules()
    matchers.addCategory('header').addRule('CONTENT-TYPE', new RegexMatcher('[a-z]+\\/[a-z]+'))

    expect:
    HeaderMatcher.compareHeader('CONTENT-TYPE', 'application/json',
      'application/json;charset=UTF-8', matchers) != None$.MODULE$
  }

  def "parse parameters - parse the parameters into a map"() {
    expect:
    JavaConversions.mapAsJavaMap(HeaderMatcher.parseParameters(JavaConversions.asScalaBuffer(['A=B']).toList())) ==
      [A: 'B']
    JavaConversions.mapAsJavaMap(HeaderMatcher.parseParameters(JavaConversions.asScalaBuffer(['A=B', 'C=D'])
      .toList())) == [A: 'B', C: 'D']
    JavaConversions.mapAsJavaMap(HeaderMatcher.parseParameters(JavaConversions.asScalaBuffer(['A= B', 'C =D '])
      .toList())) == [A: 'B', C: 'D']
  }

}
