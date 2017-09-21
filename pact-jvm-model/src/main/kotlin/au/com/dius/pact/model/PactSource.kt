package au.com.dius.pact.model

import java.io.File
import java.util.function.Supplier

/**
 * Represents the source of a Pact
 */
sealed class PactSource {
  open fun description() = toString()
}

/**
 * A source of a pact that comes from some URL
 */
sealed class UrlPactSource : PactSource() {
  abstract val url: String
}

data class DirectorySource @JvmOverloads constructor(val dir: File,
                                                     val pacts: MutableMap<File, Pact> = mutableMapOf()) : PactSource()

data class PactBrokerSource @JvmOverloads constructor(val host: String,
                                                      val port: String,
                                                      val pacts: MutableMap<Consumer, List<Pact>> = mutableMapOf())
  : PactSource()

data class FileSource @JvmOverloads constructor(val file: File, val pact: Pact? = null) : PactSource() {
  override fun description() = "File $file"
}

data class UrlSource @JvmOverloads constructor(override val url: String, val pact: Pact? = null) : UrlPactSource() {
  override fun description() = "URL $url"
}

data class UrlsSource @JvmOverloads constructor(val url: List<String>,
                                                val pacts: MutableMap<String, Pact> = mutableMapOf()) : PactSource()

data class BrokerUrlSource @JvmOverloads constructor(override val url: String,
                                                     val pactBrokerUrl: String,
                                                     val attributes: Map<String, Map<String, Any>> = mapOf(),
                                                     val options: Map<String, Any> = mapOf()) : UrlPactSource() {
  override fun description() = "Pact Broker $url"
}

object InputStreamPactSource : PactSource()

object ReaderPactSource : PactSource()

object UnknownPactSource : PactSource()

@Suppress("ClassNaming")
data class S3PactSource(override val url: String) : UrlPactSource() {
  override fun description() = "S3 Bucket $url"
}

data class ClosurePactSource(val closure: Supplier<Any>) : PactSource()
