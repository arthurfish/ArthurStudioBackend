package io.github.arthurfish

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.*
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime

typealias Icon = String

@Serializable
data class Vendor(
  val name: String,
  val apikey: String,
  val entrypointUrl: String,
  val icon: Icon? = null,
)

@Serializable
data class Model (
  val fullName: String,
  val ability: String,
  val vendor: Vendor? = null,
  val icon: Icon? = null,
)

@Serializable
data class Session(
  val preferedModel: Model? = null,
  val keywords: String = "default",
  val isBusy: Boolean = false,
  val icon: Icon? = null,
  )

@Serializable
data class Message (
  val sender: String,
  val mimeType: String,
  val content: String,
  val isHidden: Boolean = false,
  val isComplete: Boolean = true,
  val isUserInput: Boolean = true,
  val session: Session? = null,
)

@Serializable
data class RestEvent(
  val resourceUrl: String,
  val httpVerb: String,
  val content: String,
  val serialNumber: Long,
  val id: String,
  val createAt: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
)
/* Transmission Data Structure*/
@Serializable
data class TransmissionMessage (
  val type: String,
  val requestedSerialNumber: Long? = null,
  val currentSerialNumber: Long? = null,
  val event: RestEvent? = null,
)
