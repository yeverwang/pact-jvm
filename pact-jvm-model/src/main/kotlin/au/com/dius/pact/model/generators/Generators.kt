package au.com.dius.pact.model.generators

import au.com.dius.pact.model.ContentType
import au.com.dius.pact.model.InvalidPactException
import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.PactSpecVersion
import au.com.dius.pact.model.PathToken
import au.com.dius.pact.model.parsePath
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import mu.KLogging
import org.apache.commons.collections4.IteratorUtils

enum class Category {
  METHOD, PATH, HEADER, QUERY, BODY, STATUS
}

interface ContentTypeHandler {
  fun processBody(value: String, fn: (QueryResult) -> Unit): OptionalBody
  fun applyKey(body: QueryResult, key: String, generator: Generator)
}

val contentTypeHandlers: MutableMap<String, ContentTypeHandler> = mutableMapOf(
  "application/json" to JsonContentTypeHandler
)

fun setupDefaultContentTypeHandlers() {
  contentTypeHandlers.clear()
  contentTypeHandlers["application/json"] = JsonContentTypeHandler
}

data class QueryResult(var value: Any, val key: Any? = null, val parent: Any? = null)

object JsonContentTypeHandler : ContentTypeHandler {
  override fun processBody(value: String, fn: (QueryResult) -> Unit): OptionalBody {
    val bodyJson = QueryResult(JsonSlurper().parseText(value))
    fn.invoke(bodyJson)
    return OptionalBody.body(JsonOutput.toJson(bodyJson.value))
  }

  override fun applyKey(body: QueryResult, key: String, generator: Generator) {
    val pathExp = parsePath(key)
    queryObjectGraph(pathExp.iterator(), body) { (value, valueKey, parent) ->
      @Suppress("UNCHECKED_CAST")
      when (parent) {
        is MutableMap<*, *> -> (parent as MutableMap<String, Any>)[valueKey.toString()] = generator.generate(value)
        is MutableList<*> -> (parent as MutableList<Any>)[valueKey as Int] = generator.generate(value)
        else -> body.value = generator.generate(value)
      }
    }
  }

  private fun queryObjectGraph(pathExp: Iterator<PathToken>, body: QueryResult, fn: (QueryResult) -> Unit) {
    var bodyCursor = body
    while (pathExp.hasNext()) {
      val token = pathExp.next()
      when (token) {
        is PathToken.Field -> if (bodyCursor.value is Map<*, *> &&
          (bodyCursor.value as Map<*, *>).containsKey(token.name)) {
          val map = bodyCursor.value as Map<*, *>
          bodyCursor = QueryResult(map[token.name]!!, token.name, bodyCursor.value)
        } else {
          return
        }
        is PathToken.Index -> if (bodyCursor.value is List<*> && (bodyCursor.value as List<*>).size > token.index) {
          val list = bodyCursor.value as List<*>
          bodyCursor = QueryResult(list[token.index]!!, token.index, bodyCursor.value)
        } else {
          return
        }
        is PathToken.Star -> if (bodyCursor.value is MutableMap<*, *>) {
          val map = bodyCursor.value as MutableMap<*, *>
          val pathIterator = IteratorUtils.toList(pathExp)
          HashMap(map).forEach { (key, value) ->
            queryObjectGraph(pathIterator.iterator(), QueryResult(value!!, key, map), fn)
          }
          return
        } else {
          return
        }
        is PathToken.StarIndex -> if (bodyCursor.value is List<*>) {
          val list = bodyCursor.value as List<*>
          val pathIterator = IteratorUtils.toList(pathExp)
          list.forEachIndexed { index, item ->
            queryObjectGraph(pathIterator.iterator(), QueryResult(item!!, index, list), fn)
          }
          return
        } else {
          return
        }
      }
    }

    fn(bodyCursor)
  }

}

data class Generators(val categories: MutableMap<Category, MutableMap<String, Generator>> = HashMap()) {

  companion object : KLogging() {

    @JvmStatic fun fromMap(map: Map<String, Map<String, Any>>?): Generators {
      val generators = Generators()

      map?.forEach { (key, generatorMap) ->
        try {
          val category = Category.valueOf(key.toUpperCase())
          when (category) {
            Category.STATUS, Category.PATH, Category.METHOD -> if (generatorMap.containsKey("type")) {
              val generator = lookupGenerator(generatorMap)
              if (generator != null) {
                generators.addGenerator(category, generator = generator)
              } else {
                logger.warn { "Ignoring invalid generator config '$generatorMap'" }
              }
            } else {
              logger.warn { "Ignoring invalid generator config '$generatorMap'" }
            }
            else -> generatorMap.forEach { (generatorKey, generatorValue) ->
              if (generatorValue is Map<*, *> && generatorValue.containsKey("type")) {
                @Suppress("UNCHECKED_CAST")
                val generator = lookupGenerator(generatorValue as Map<String, Any>)
                if (generator != null) {
                  generators.addGenerator(category, generatorKey, generator)
                } else {
                  logger.warn { "Ignoring invalid generator config '$generatorMap'" }
                }
              } else {
                logger.warn { "Ignoring invalid generator config '$generatorKey -> $generatorValue'" }
              }
            }
          }
        } catch (e: IllegalArgumentException) {
          logger.warn(e) { "Ignoring generator with invalid category '$key'" }
        }
      }

      return generators
    }
  }

  @JvmOverloads
  fun addGenerator(category: Category, key: String? = "", generator: Generator): Generators {
    if (categories.containsKey(category) && categories[category] != null) {
      categories[category]?.put(key ?: "", generator)
    } else {
      categories[category] = mutableMapOf((key ?: "") to generator)
    }
    return this
  }

  @JvmOverloads
  fun addGenerators(generators: Generators, keyPrefix: String = ""): Generators {
    generators.categories.forEach { (category, map) ->
      map.forEach { (key, generator) ->
        addGenerator(category, keyPrefix + key, generator)
      }
    }
    return this
  }

  fun addCategory(category: Category): Generators {
    if (!categories.containsKey(category)) {
      categories[category] = mutableMapOf()
    }
    return this
  }

  fun applyGenerator(category: Category, closure: (String, Generator?) -> Unit) {
    if (categories.containsKey(category) && categories[category] != null) {
      val categoryValues = categories[category]
      if (categoryValues != null) {
        for ((key, value) in categoryValues) {
          closure.invoke(key, value)
        }
      }
    }
  }

  fun applyBodyGenerators(body: OptionalBody, contentType: ContentType): OptionalBody {
    return when (body.state) {
      OptionalBody.State.EMPTY, OptionalBody.State.MISSING, OptionalBody.State.NULL -> body
      OptionalBody.State.PRESENT -> when {
        contentType.isJson() -> processBody(body.value!!, "application/json")
        contentType.isXml() -> processBody(body.value!!, "application/xml")
        else -> body
      }
    }
  }

  private fun processBody(value: String, contentType: String): OptionalBody {
    val handler = contentTypeHandlers[contentType]
    return handler?.processBody(value) { body: QueryResult ->
      applyGenerator(Category.BODY) { key: String, generator: Generator? ->
        if (generator != null) {
          handler.applyKey(body, key, generator)
        }
      }
    } ?: OptionalBody.body(value)
  }

  /**
   * If there are no generators
   */
  fun isEmpty() = categories.isEmpty()

  /**
   * If there are generators
   */
  fun isNotEmpty() = categories.isNotEmpty()

  fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    if (pactSpecVersion < PactSpecVersion.V3) {
      throw InvalidPactException("Generators are only supported with pact specification version 3+")
    }
    return categories.entries.associate { (key, value) ->
      when (key) {
        Category.METHOD, Category.PATH, Category.STATUS -> key.name.toLowerCase() to value[""]!!.toMap(pactSpecVersion)
        else -> key.name.toLowerCase() to value.entries.associate { (genKey, generator) ->
          genKey to generator.toMap(pactSpecVersion)
        }
      }
    }
  }

  fun applyRootPrefix(prefix: String) {
    categories.keys.forEach { category ->
      categories[category] = categories[category]!!.mapKeys { entry -> prefix + entry.key }.toMutableMap()
    }
  }
}
