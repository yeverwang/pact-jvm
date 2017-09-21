package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.model.matchingrules.MatchingRule

/**
 * Matcher to match using equality
 */
class EqualsMatcher extends Matcher {
  MatchingRule getMatcher() {
    au.com.dius.pact.model.matchingrules.EqualsMatcher.INSTANCE
  }
}
