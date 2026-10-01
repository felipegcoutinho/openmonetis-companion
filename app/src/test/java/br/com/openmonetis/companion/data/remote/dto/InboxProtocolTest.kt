package br.com.openmonetis.companion.data.remote.dto

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class InboxProtocolTest {
    @Test
    fun batchJsonDeclaresUtcTimestampProtocolToAvoidLegacyThreeHourShift() {
        val timestamp = "2026-09-30T22:00:00.000Z"
        val request = InboxRequest(
            sourceApp = "test.app",
            sourceAppName = null,
            originalTitle = null,
            originalText = "Notificação de teste",
            notificationTimestamp = timestamp,
            parsedName = null,
            parsedAmount = null,
            clientId = "test-id"
        )
        val json = JsonParser.parseString(Gson().toJson(InboxBatchRequest(listOf(request))))
        val item = json.asJsonObject.getAsJsonArray("items")[0].asJsonObject

        assertEquals(2, item.get("timestampFormatVersion").asInt)
        assertEquals(Instant.parse(timestamp), Instant.parse(item.get("notificationTimestamp").asString))
        assertEquals("test-id", item.get("clientId").asString)
    }
}
