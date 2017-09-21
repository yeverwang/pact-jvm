package au.com.dius.pact.provider.junit

import au.com.dius.pact.model.Pact
import au.com.dius.pact.model.ProviderState
import au.com.dius.pact.model.Request
import au.com.dius.pact.model.RequestResponseInteraction
import au.com.dius.pact.model.RequestResponsePact
import au.com.dius.pact.model.Response
import au.com.dius.pact.provider.junit.loader.PactFilter
import au.com.dius.pact.provider.junit.loader.PactFolder
import au.com.dius.pact.provider.junit.target.Target
import au.com.dius.pact.provider.junit.target.TestTarget
import org.junit.runners.model.InitializationError
import spock.lang.Specification

class FilteredPactRunnerSpec extends Specification {

  private List<Pact> pacts
  private au.com.dius.pact.model.Consumer consumer, consumer2
  private au.com.dius.pact.model.Provider provider
  private List<RequestResponseInteraction> interactions, interactions2

  @Provider('myAwesomeService')
  @PactFolder('pacts')
  @PactFilter('State 1')
  @IgnoreNoPactsToVerify
  class TestClass {
    @TestTarget
    Target target
  }

  @Provider('myAwesomeService')
  @PactFolder('pacts')
  @PactFilter('')
  class TestClassEmptyFilter {
    @TestTarget
    Target target
  }

  @Provider('myAwesomeService')
  @PactFolder('pacts')
  @PactFilter(['', '', ''])
  class TestClassEmptyFilters {
    @TestTarget
    Target target
  }

  @Provider('myAwesomeService')
  @PactFolder('pacts')
  @PactFilter('')
  class TestClassNoFilterAnnotations {
    @TestTarget
    Target target
  }

  @Provider('myAwesomeService')
  @PactFolder('pacts')
  @PactFilter(['State 1', 'State 3'])
  @IgnoreNoPactsToVerify
  class TestMultipleStatesClass {
    @TestTarget
    Target target
  }

  @Provider('myAwesomeService')
  @PactFolder('pacts')
  @PactFilter('State \\d+')
  @IgnoreNoPactsToVerify
  class TestRegexClass {
    @TestTarget
    Target target
  }

  @Provider('myAwesomeService')
  @PactFolder('pacts')
  @PactFilter(['State 6'])
  class TestFilterOutAllPactsClass {
    @TestTarget
    Target target
  }

  @Provider('myAwesomeService')
  @PactFolder('pacts')
  @PactFilter(['State 6'])
  @IgnoreNoPactsToVerify
  class TestFilterOutAllPactsIgnoreNoPactsToVerifyClass {
    @TestTarget
    Target target
  }

  def setup() {
    consumer = new au.com.dius.pact.model.Consumer('Consumer 1')
    consumer2 = new au.com.dius.pact.model.Consumer('Consumer 2')
    provider = new au.com.dius.pact.model.Provider('myAwesomeService')
    interactions = [
      new RequestResponseInteraction('Req 1', [
        new ProviderState('State 1')
      ], new Request(), new Response()),
      new RequestResponseInteraction('Req 2', [
        new ProviderState('State 1'),
        new ProviderState('State 2')
      ], new Request(), new Response())
    ]
    interactions2 = [
      new RequestResponseInteraction('Req 3', [
        new ProviderState('State 3')
      ], new Request(), new Response()),
      new RequestResponseInteraction('Req 4', [
        new ProviderState('State X')
      ], new Request(), new Response())
    ]
    pacts = [
      new RequestResponsePact(provider, consumer, interactions),
      new RequestResponsePact(provider, consumer2, interactions2)
    ]
  }

  def 'handles a test class with no filter annotations'() {
    given:
    FilteredPactRunner filteredPactRunner = new FilteredPactRunner(TestClassNoFilterAnnotations)

    when:
    def result = filteredPactRunner.filterPacts(pacts)

    then:
    result.is pacts
  }

  def 'handles a test class with an empty filter annotation'() {
    given:
    FilteredPactRunner filteredPactRunner = new FilteredPactRunner(TestClassEmptyFilter)
    FilteredPactRunner filteredPactRunner2 = new FilteredPactRunner(TestClassEmptyFilters)

    when:
    def result = filteredPactRunner.filterPacts(pacts)
    def result2 = filteredPactRunner2.filterPacts(pacts)

    then:
    result.is pacts
    result2.is pacts
  }

  def 'filters the interactions by provider state'() {
    given:
    FilteredPactRunner filteredPactRunner = new FilteredPactRunner(TestClass)

    when:
    def result = filteredPactRunner.filterPacts(pacts)

    then:
    result.size() == 1
    result*.interactions*.description.flatten() == ['Req 1', 'Req 2']
  }

  def 'filters the interactions correctly when given multiple provider states'() {
    given:
    FilteredPactRunner filteredPactRunner = new FilteredPactRunner(TestMultipleStatesClass)

    when:
    def result = filteredPactRunner.filterPacts(pacts)

    then:
    result.size() == 2
    result*.interactions*.description.flatten() == ['Req 1', 'Req 2', 'Req 3']
  }

  def 'filters the interactions correctly when given a regex'() {
    given:
    FilteredPactRunner filteredPactRunner = new FilteredPactRunner(TestRegexClass)

    when:
    def result = filteredPactRunner.filterPacts(pacts)

    then:
    result.size() == 2
    result*.interactions*.description.flatten() == ['Req 1', 'Req 2', 'Req 3']
  }

  @SuppressWarnings('UnusedObject')
  def 'Throws an initialisation error if all pacts are filtered out'() {
    when:
    new FilteredPactRunner(TestFilterOutAllPactsClass)

    then:
    thrown(InitializationError)
  }

  @SuppressWarnings('UnusedObject')
  def 'Does not throw an initialisation error if all pacts are filtered out but @IgnoreNoPactsToVerify is present'() {
    when:
    new FilteredPactRunner(TestFilterOutAllPactsIgnoreNoPactsToVerifyClass)

    then:
    notThrown(InitializationError)
  }

}
