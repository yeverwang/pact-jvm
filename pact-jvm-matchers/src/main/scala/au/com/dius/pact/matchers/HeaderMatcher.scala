package au.com.dius.pact.matchers

import au.com.dius.pact.model.matchingrules.MatchingRules
import au.com.dius.pact.model.{HeaderMismatch, HeaderMismatchFactory}
import com.typesafe.scalalogging.StrictLogging

object HeaderMatcher extends StrictLogging {

  def matchContentType(expected: String, actual: String) = {
    logger.debug(s"Comparing content type header: '$actual' to '$expected'")

    val expectedValues = expected.split(';').map(_.trim)
    val actualValues = actual.split(';').map(_.trim)
    val expectedContentType = expectedValues.head
    val actualContentType = actualValues.head
    val expectedParameters = parseParameters(expectedValues.toList.tail)
    val actualParameters = parseParameters(actualValues.toList.tail)
    val headerMismatch = Some(HeaderMismatch("Content-Type", expected, actual,
      Some(s"Expected header 'Content-Type' to have value '$expected' but was '$actual'")))

    if (expectedContentType == actualContentType) {
      expectedParameters.map(entry => {
        if (actualParameters.contains(entry._1)) {
          if (entry._2 == actualParameters(entry._1)) None
          else headerMismatch
        } else headerMismatch
      }).find(_.isDefined).getOrElse(None)
    }
    else headerMismatch
  }

  def parseParameters(values: List[String]): Map[String, String] = {
    values.map(_.split('=').map(_.trim)).foldLeft(Map[String, String]()) {
      (m, v) => m + (v(0) -> v(1))
    }
  }

  def compareHeader(headerKey: String, expected: String, actual: String, matchers: MatchingRules) = {
    logger.debug(s"Comparing header '$headerKey': '$actual' to '$expected'")

    def stripWhiteSpaceAfterCommas(in: String): String = in.replaceAll(",[ ]*", ",")

    if (Matchers.matcherDefined("header", Seq(headerKey), matchers)) {
      Matchers.domatch[HeaderMismatch](matchers, "header", Seq(headerKey), expected, actual, HeaderMismatchFactory).headOption
    }
    else if (headerKey.equalsIgnoreCase("Content-Type")) matchContentType(expected, actual)
    else if (stripWhiteSpaceAfterCommas(expected) == stripWhiteSpaceAfterCommas(actual)) None
    else Some(HeaderMismatch(headerKey, expected, actual,
      Some(s"Expected header '$headerKey' to have value '$expected' but was '$actual'")))
  }
}
