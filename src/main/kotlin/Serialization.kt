package io.github.arthurfish

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import java.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.jetbrains.exposed.sql.*

fun Application.configureSerialization() {
  install(ContentNegotiation) {
    json(Json {
      prettyPrint = true
      isLenient = true
    })
  }
}
