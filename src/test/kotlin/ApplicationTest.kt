package io.github.arthurfish

import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ApplicationTest {

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(WebSockets){
                contentConverter = KotlinxWebsocketSerializationConverter(Json)
            }
        }
        client.webSocket("/ws") {
            for (i in 1L..4L) {

                sendSerialized(TransmissionMessage("offer", currentSerialNumber = 4, event = RestEvent("/message", "POST", "TEST FUCK", serialNumber = 4L+i, id= Uuid.random().toString())))
            }
        }
    }

}
