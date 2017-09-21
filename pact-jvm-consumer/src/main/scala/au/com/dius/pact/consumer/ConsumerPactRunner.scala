package au.com.dius.pact.consumer

import java.net.SocketException

import au.com.dius.pact.model.{Pact, PactSpecVersion}

import scala.util.{Failure, Success, Try}

/**
  * @deprecated Moved to Kotlin implementation
  */
@Deprecated
object ConsumerPactRunner {
  
  def writeIfMatching(pact: Pact, results: PactSessionResults, pactVersion: PactSpecVersion): VerificationResult =
    writeIfMatching(pact, Success(results), pactVersion)
  
  def writeIfMatching(pact: Pact, tryResults: Try[PactSessionResults], pactVersion: PactSpecVersion): VerificationResult = {
    for (results <- tryResults if results.allMatched) {
      PactGenerator.merge(pact).writeAllToFile(pactVersion)
    }
    VerificationResult(tryResults)
  }
  
  def runAndWritePact[T](pact: Pact, pactVersion: PactSpecVersion = PactSpecVersion.V3)(userCode: => T, userVerification: ConsumerTestVerification[T]): VerificationResult = {
    val server = DefaultMockProvider.withDefaultConfig(pactVersion)
    new ConsumerPactRunner(server).runAndWritePact(pact, pactVersion)(userCode, userVerification)
  }
}

/**
  * @deprecated Moved to Kotlin implementation
  */
@Deprecated
class ConsumerPactRunner(server: MockProvider) {
  import ConsumerPactRunner._
  
  def runAndWritePact[T](pact: Pact, pactVersion: PactSpecVersion)(userCode: => T, userVerification: ConsumerTestVerification[T]): VerificationResult = {
    val tryResults = server.runAndClose(pact)(userCode)
    tryResults match {
      case Failure(e) =>
        if (e.isInstanceOf[SocketException]) PactError(new MockServerException("Failed to start mock server: " + e.getMessage, e))
        else if (server.session.remainingResults.allMatched) PactError(e)
        else PactMismatch(server.session.remainingResults, Some(e))
      case Success((codeResult, pactSessionResults)) =>
        userVerification(codeResult).fold(writeIfMatching(pact, pactSessionResults, pactVersion)) { error =>
          UserCodeFailed(error)
        }
    }
  }
  
  def runAndWritePact(pact: Pact, userCode: Runnable): VerificationResult =
    runAndWritePact(pact, server.config.getPactVersion)(userCode.run(), (u:Unit) => None)

  def run[T](userCode: => T, userVerification: ConsumerTestVerification[T]): VerificationResult = {
    val tryResults = server.run(userCode)
    tryResults match {
      case Failure(e) =>
        PactError(e)
      case Success(codeResult) =>
        userVerification(codeResult).fold[VerificationResult](PactVerified)(UserCodeFailed(_))
    }
  }

  def writePact(pact: Pact, pactVersion: PactSpecVersion): VerificationResult =
    if (server.session.remainingResults.allMatched)
      writeIfMatching(pact, server.session.remainingResults, pactVersion)
    else
      PactMismatch(server.session.remainingResults, None)
}
