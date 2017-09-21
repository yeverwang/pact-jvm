package specification

import au.com.dius.pact.model.PactSpecVersion
import au.com.dius.pact.model.RequestMatching
import groovy.util.logging.Slf4j
import spock.lang.Unroll

@Slf4j
class RequestSpecificationV2Spec extends BaseRequestSpec {

  @Unroll
  def '#type/#name #test #matchDesc'() {
    expect:
    RequestMatching.requestMismatches(expected, actual).isEmpty() == match

    where:
    [type, name, test, match, matchDesc, expected, actual] << loadTestCases('/v2/request/', PactSpecVersion.V2)
  }

}
