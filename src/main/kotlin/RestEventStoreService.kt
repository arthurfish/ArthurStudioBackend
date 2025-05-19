package io.github.arthurfish

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.*
import kotlin.math.max

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

import kotlin.uuid.*

object IncrementOnly{
  private var _serverSideMaxSerialNumber: Long = 0
  var serverSideMaxSerialNumber: Long
    get() = this._serverSideMaxSerialNumber
  set(value) {
    this._serverSideMaxSerialNumber = max(value, this._serverSideMaxSerialNumber)
  }
}

class RestEventStoreService(database: Database) {
  @OptIn(ExperimentalUuidApi::class)
  fun initDatabase(){
    transaction {
      try{
        SchemaUtils.drop(RestEventTable)
      }catch(e: Exception){ }

      SchemaUtils.create(RestEventTable)

      var counter: Long = 1
      RestEventTable.insert {
        it[id] = Uuid.random().toString()
        it[resourceUrl] = "/vendor"
        it[httpVerb] = "POST"
        it[content] = Json.encodeToString(Vendor("volcengine", "6c57e60f-570e-4af3-afb3-8cd5dc29c9af", "https://ark.cn-beijing.volces.com/api/v3/"))
        it[serialNumber] = counter as Long
      }
      counter++

      RestEventTable.insert {
        it[id] = Uuid.random().toString()
        it[resourceUrl] = "/model"
        it[httpVerb] = "POST"
        it[content] = Json.encodeToString(Model("deepseek-v3", "text-generate"))
        it[serialNumber] = counter as Long
      }
      counter++

      RestEventTable.insert{
        it[id] = Uuid.random().toString()
        it[resourceUrl] = "/session"
        it[httpVerb] = "POST"
        it[content] = Json.encodeToString(Session(keywords = "what, the, fuck"))
        it[serialNumber] = counter as Long
      }
      counter++

      RestEventTable.insert {
        it[id] = Uuid.random().toString()
        it[resourceUrl] = "/message"
        it[httpVerb] = "POST"
        it[content] = Json.encodeToString(Message("user", "text/plain", "*?#".repeat(10)))
        it[serialNumber] = counter as Long
      }
      counter++

      IncrementOnly.serverSideMaxSerialNumber = counter as Long
    }
  }

  object RestEventTable: Table("rest_event") {
    val id = varchar("id", 36)
    val createAt = datetime("create_at").defaultExpression(CurrentDateTime)
    val resourceUrl = varchar("resource_url", 1024)
    val httpVerb = varchar("http_verb", 16)
    val content = text("content")
    val serialNumber = long("serial_number")

    override val primaryKey: PrimaryKey = PrimaryKey(id)
  }
  private suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }

  suspend fun storeEvent(event: RestEvent) = dbQuery {
    RestEventTable.insert{
      it[resourceUrl] = event.resourceUrl
      it[httpVerb] = event.httpVerb
      it[content] = event.content
      it[serialNumber] = event.serialNumber
      it[id] = event.id
      it[createAt] = event.createAt
    }
  }

  suspend fun getEventBySerialNumber(serialNumber: Long): RestEvent? = dbQuery {
    RestEventTable.selectAll()
      .where{RestEventTable.serialNumber.eq(serialNumber)}
      .map {RestEvent(it[RestEventTable.resourceUrl],
        it[RestEventTable.httpVerb],
        it[RestEventTable.content],
        it[RestEventTable.serialNumber],
        it[RestEventTable.id],
        it[RestEventTable.createAt])}
      .singleOrNull()
  }

}

