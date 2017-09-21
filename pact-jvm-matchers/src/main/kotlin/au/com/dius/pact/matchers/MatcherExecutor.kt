package au.com.dius.pact.matchers

import au.com.dius.pact.model.matchingrules.DateMatcher
import au.com.dius.pact.model.matchingrules.IncludeMatcher
import au.com.dius.pact.model.matchingrules.MatchingRule
import au.com.dius.pact.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.model.matchingrules.MaxTypeMatcher
import au.com.dius.pact.model.matchingrules.MinTypeMatcher
import au.com.dius.pact.model.matchingrules.NullMatcher
import au.com.dius.pact.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.model.matchingrules.RegexMatcher
import au.com.dius.pact.model.matchingrules.RuleLogic
import au.com.dius.pact.model.matchingrules.TimeMatcher
import au.com.dius.pact.model.matchingrules.TimestampMatcher
import au.com.dius.pact.model.matchingrules.TypeMatcher
import mu.KotlinLogging
import org.apache.commons.lang3.time.DateUtils
import scala.xml.Elem
import java.math.BigDecimal
import java.math.BigInteger
import java.text.ParseException

private val logger = KotlinLogging.logger {}

fun valueOf(value: Any?): String {
  return when (value) {
    null -> "null"
    is String -> "'$value'"
    else -> value.toString()
  }
}

fun safeToString(value: Any?): String {
  return when (value) {
    null -> ""
    is Elem -> value.text()
    else -> value.toString()
  }
}

fun <Mismatch> matchInclude(includedValue: String, path: List<String>, expected: Any?, actual: Any?,
                            mismatchFactory: MismatchFactory<Mismatch>): List<Mismatch> {
  val matches = safeToString(actual).contains(includedValue)
  logger.debug { "comparing if ${valueOf(actual)} includes '$includedValue' at $path -> $matches" }
  return if (matches) {
    listOf()
  } else {
    listOf(mismatchFactory.create(expected, actual,
      "Expected ${valueOf(actual)} to include ${valueOf(includedValue)}", path))
  }
}

/**
 * Executor for matchers
 */
fun <Mismatch> domatch(matchers: MatchingRuleGroup, path: List<String>, expected: Any?, actual: Any?,
                       mismatchFn: MismatchFactory<Mismatch>): List<Mismatch> {
  val result = matchers.rules.map { matchingRule ->
    domatch(matchingRule, path, expected, actual, mismatchFn)
  }

  return if (matchers.ruleLogic == RuleLogic.AND) {
    result.flatten()
  } else {
    if (result.any { it.isEmpty() }) {
      emptyList()
    } else {
      result.flatten()
    }
  }
}

fun <Mismatch> domatch(matcher: MatchingRule, path: List<String>, expected: Any?, actual: Any?,
                       mismatchFn: MismatchFactory<Mismatch>): List<Mismatch> {
  return when (matcher) {
    is RegexMatcher -> matchRegex(matcher.regex, path, expected, actual, mismatchFn)
    is TypeMatcher -> matchType(path, expected, actual, mismatchFn)
    is NumberTypeMatcher -> matchNumber(matcher.numberType, path, expected, actual, mismatchFn)
    is DateMatcher -> matchDate(matcher.format, path, expected, actual, mismatchFn)
    is TimeMatcher -> matchTime(matcher.format, path, expected, actual, mismatchFn)
    is TimestampMatcher -> matchTimestamp(matcher.format, path, expected, actual, mismatchFn)
    is MinTypeMatcher -> matchMinType(matcher.min, path, expected, actual, mismatchFn)
    is MaxTypeMatcher -> matchMaxType(matcher.max, path, expected, actual, mismatchFn)
    is IncludeMatcher -> matchInclude(matcher.value, path, expected, actual, mismatchFn)
    is NullMatcher -> matchNull(path, actual, mismatchFn)
    else -> matchEquality(path, expected, actual, mismatchFn)
  }
}

fun <Mismatch> matchEquality(path: List<String>, expected: Any?, actual: Any?,
                             mismatchFactory: MismatchFactory<Mismatch>): List<Mismatch> {
  val matches = actual == null && expected == null || actual != null && actual == expected
  logger.debug { "comparing ${valueOf(actual)} to ${valueOf(expected)} at $path -> $matches" }
  return if (matches) {
    emptyList()
  } else {
    listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to equal ${valueOf(actual)}", path))
  }
}

fun <Mismatch> matchRegex(regex: String, path: List<String>, expected: Any?, actual: Any?,
                          mismatchFactory: MismatchFactory<Mismatch>): List<Mismatch> {
  val matches = safeToString(actual).matches(Regex(regex))
  logger.debug { "comparing ${valueOf(actual)} with regexp $regex at $path -> $matches" }
  return if (matches
    || expected is List<*> && actual is List<*>
    || expected is scala.collection.immutable.List<*> && actual is scala.collection.immutable.List<*>
    || expected is Map<*, *> && actual is Map<*, *>
    || expected is scala.collection.Map<*, *> && actual is scala.collection.Map<*, *>) {
    emptyList()
  } else {
    listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to match '$regex'", path))
  }
}

fun <Mismatch> matchType(path: List<String>, expected: Any?, actual: Any?,
                         mismatchFactory: MismatchFactory<Mismatch>): List<Mismatch> {
  logger.debug { "comparing type of ${valueOf(actual)} to ${valueOf(expected)} at $path" }
  return if (expected is String && actual is String
    || expected is Number && actual is Number
    || expected is Boolean && actual is Boolean
    || expected is List<*> && actual is List<*>
    || expected is scala.collection.immutable.List<*> && actual is scala.collection.immutable.List<*>
    || expected is Map<*, *> && actual is Map<*, *>
    || expected is scala.collection.Map<*, *> && actual is scala.collection.Map<*, *>
    || expected is Elem && actual is Elem && actual.label() == expected.label()) {
    emptyList()
  } else if (expected == null) {
    if (actual == null) {
      emptyList()
    } else {
      listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to be null", path))
    }
  } else {
    listOf(mismatchFactory.create(expected, actual,
      "Expected ${valueOf(actual)} to be the same type as ${valueOf(expected)}", path))
  }
}

fun <Mismatch> matchNumber(numberType: NumberTypeMatcher.NumberType, path: List<String>, expected: Any?, actual: Any?,
                           mismatchFactory: MismatchFactory<Mismatch>): List<Mismatch> {
  if (expected == null && actual != null) {
    return listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to be null", path))
  }
  when (numberType) {
    NumberTypeMatcher.NumberType.NUMBER -> {
      logger.debug { "comparing type of ${valueOf(actual)} to a number at $path" }
      if (actual !is Number) {
        return listOf(mismatchFactory.create(expected, actual,
          "Expected ${valueOf(actual)} to be a number", path))
      }
    }
    NumberTypeMatcher.NumberType.INTEGER -> {
      logger.debug { "comparing type of ${valueOf(actual)} to an integer at $path" }
      if (actual !is Int && actual !is Long && actual !is BigInteger) {
        return listOf(mismatchFactory.create(expected, actual,
          "Expected ${valueOf(actual)} to be an integer", path))
      }
    }
    NumberTypeMatcher.NumberType.DECIMAL -> {
      logger.debug { "comparing type of ${valueOf(actual)} to a decimal at $path" }
      if (actual !is Float && actual !is Double && actual !is BigDecimal) {
        return listOf(mismatchFactory.create(expected, actual,
          "Expected ${valueOf(actual)} to be a decimal number",
          path))
      }
    }
  }
  return emptyList()
}

fun <Mismatch> matchDate(pattern: String, path: List<String>, expected: Any?, actual: Any?,
                         mismatchFactory: MismatchFactory<Mismatch>): List<Mismatch> {
  logger.debug { "comparing ${valueOf(actual)} to date pattern $pattern at $path" }
  return try {
    DateUtils.parseDate(safeToString(actual), pattern)
    emptyList()
  } catch (e: ParseException) {
    listOf(mismatchFactory.create(expected, actual,
      "Expected ${valueOf(actual)} to match a date of '$pattern': " +
        "${e.message}", path))
  }
}

fun <Mismatch> matchTime(pattern: String, path: List<String>, expected: Any?, actual: Any?,
                         mismatchFactory: MismatchFactory<Mismatch>): List<Mismatch> {
  logger.debug { "comparing ${valueOf(actual)} to time pattern $pattern at $path" }
  return try {
    DateUtils.parseDate(safeToString(actual), pattern)
    emptyList()
  } catch (e: ParseException) {
    listOf(mismatchFactory.create(expected, actual,
      "Expected ${valueOf(actual)} to match a time of '$pattern': " +
        "${e.message}", path))
  }
}

fun <Mismatch> matchTimestamp(pattern: String, path: List<String>, expected: Any?, actual: Any?,
                              mismatchFactory: MismatchFactory<Mismatch>): List<Mismatch> {
  logger.debug { "comparing ${valueOf(actual)} to timestamp pattern $pattern at $path" }
  return try {
    DateUtils.parseDate(safeToString(actual), pattern)
    emptyList()
  } catch (e: ParseException) {
    listOf(mismatchFactory.create(expected, actual,
      "Expected ${valueOf(actual)} to match a timestamp of '$pattern': " +
        "${e.message}", path))
  }
}

fun <Mismatch> matchMinType(min: Int, path: List<String>, expected: Any?, actual: Any?,
                            mismatchFactory: MismatchFactory<Mismatch>): List<Mismatch> {
  logger.debug { "comparing ${valueOf(actual)} with minimum $min at $path" }
  return if (actual is List<*>) {
    if (actual.size < min) {
      listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have minimum $min", path))
    } else {
      emptyList()
    }
  } else if (actual is scala.collection.immutable.List<*>) {
    if (actual.size() < min) {
      listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have minimum $min", path))
    } else {
      emptyList()
    }
  } else if (actual is Elem) {
    if (actual.child().size() < min) {
      listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have minimum $min", path))
    } else {
      emptyList()
    }
  } else {
    matchType(path, expected, actual, mismatchFactory)
  }
}

fun <Mismatch> matchMaxType(max: Int, path: List<String>, expected: Any?, actual: Any?,
                            mismatchFactory: MismatchFactory<Mismatch>): List<Mismatch> {
  logger.debug { "comparing ${valueOf(actual)} with maximum $max at $path" }
  return if (actual is List<*>) {
    if (actual.size > max) {
      listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have maximum $max", path))
    } else {
      emptyList()
    }
  } else if (actual is scala.collection.immutable.List<*>) {
    if (actual.size() > max) {
      listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have maximum $max", path))
    } else {
      emptyList()
    }
  } else if (actual is Elem) {
    if (actual.child().size() > max) {
      listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have maximum $max", path))
    } else {
      emptyList()
    }
  } else {
    matchType(path, expected, actual, mismatchFactory)
  }
}

fun <Mismatch> matchNull(path: List<String>, actual: Any?, mismatchFactory: MismatchFactory<Mismatch>): List<Mismatch> {
  val matches = actual == null
  logger.debug { "comparing ${valueOf(actual)} to null at $path -> $matches" }
  return if (matches) {
    emptyList()
  } else {
    listOf(mismatchFactory.create(null, actual, "Expected ${valueOf(actual)} to be null", path))
  }
}
