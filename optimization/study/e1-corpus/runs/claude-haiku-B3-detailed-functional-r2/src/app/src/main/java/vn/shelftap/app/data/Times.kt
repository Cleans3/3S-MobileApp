package vn.shelftap.app.data

import java.time.Instant
import java.time.OffsetDateTime

/** Parses an ISO-8601 timestamp from the API. Returns null for null or malformed input. */
fun parseInstant(iso: String?): Instant? =
    iso?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() }
