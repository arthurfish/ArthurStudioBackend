package io.github.arthurfish

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.*
import kotlin.math.max

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object IncrementOnly{
  private var _serverSideMaxSerialNumber: Long = 0
  var serverSideMaxSerialNumber: Long
    get() = this._serverSideMaxSerialNumber
  set(value) {
    this._serverSideMaxSerialNumber = max(value, this._serverSideMaxSerialNumber)
  }
}

class RestEventStoreService(database: Database) {
  fun initDatabase(){
    transaction {
      try{
        SchemaUtils.drop(RestEventTable)
      }catch(e: Exception){ }

      SchemaUtils.create(RestEventTable)
      RestEventTable.insert {
        it[resourceUrl] = "/vendor"
        it[httpVerb] = "POST"
        it[content] = Json.encodeToString(Vendor("volcengine", "6c57e60f-570e-4af3-afb3-8cd5dc29c9af", "https://ark.cn-beijing.volces.com/api/v3/"))
      }

      RestEventTable.insert {
        it[resourceUrl] = "/model"
        it[httpVerb] = "POST"
        it[content] = Json.encodeToString(Model("deepseek-v3", "text-generate"))
      }

      RestEventTable.insert{
        it[resourceUrl] = "/session"
        it[httpVerb] = "POST"
        it[content] = Json.encodeToString(Session(keywords = "what, the, fuck"))
      }

      RestEventTable.insert {
        it[resourceUrl] = "/message"
        it[httpVerb] = "POST"
        it[content] = Json.encodeToString(Message("user", "text/plain", "*?#".repeat(10)))
      }
      IncrementOnly.serverSideMaxSerialNumber = RestEventTable.selectAll().count()
    }
  }

  object RestEventTable: Table("rest_event") {
    val id = varchar("id", 18)
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

