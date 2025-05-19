package io.github.arthurfish

import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.jetbrains.exposed.sql.*

fun Application.configureSockets(restEventStoreService: RestEventStoreService) {
  install(WebSockets) {
    pingPeriod = 15.seconds
    timeout = 15.seconds
    maxFrameSize = Long.MAX_VALUE
    masking = false
    contentConverter = KotlinxWebsocketSerializationConverter(Json)
  }
  routing {
    webSocket("/ws") { // websocketSession
      while (true) {
        val transmissionMessage = receiveDeserialized<TransmissionMessage>() ?: continue
        println(transmissionMessage)
        if (transmissionMessage.type.lowercase() == "request" && transmissionMessage.requestedSerialNumber != null) {
          val retrievedEvent = restEventStoreService.getEventBySerialNumber(transmissionMessage.requestedSerialNumber)
          if(retrievedEvent != null){
            sendSerialized(
              TransmissionMessage(
                "offer",
                event = retrievedEvent,
                currentSerialNumber = IncrementOnly.serverSideMaxSerialNumber
              )
            )
          }
        } else if (transmissionMessage.type.lowercase() == "offer" && transmissionMessage.event != null) {
          restEventStoreService.storeEvent(transmissionMessage.event)
        }
        //Fix Inconsistency
        val yourSerial = transmissionMessage.currentSerialNumber
        val mySerial = IncrementOnly.serverSideMaxSerialNumber
        if (yourSerial != null && yourSerial < mySerial) {
          GlobalScope.launch {
            println("Fixing Inconsistency... $yourSerial $mySerial")
            ((yourSerial + 1)..mySerial).forEach {
              val retrievedEvent = restEventStoreService.getEventBySerialNumber(it)
              if (retrievedEvent != null) {
                sendSerialized(
                  TransmissionMessage(
                    "offer",
                    event = retrievedEvent,
                    currentSerialNumber = IncrementOnly.serverSideMaxSerialNumber
                  )
                )
              }
            }
          }
        }
      }
    }
  }
}
