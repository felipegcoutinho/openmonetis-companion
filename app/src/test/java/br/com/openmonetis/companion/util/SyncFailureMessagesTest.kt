package br.com.openmonetis.companion.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncFailureMessagesTest {
    @Test
    fun timestampRejectionHasAnActionableMessage() {
        assertEquals(
            "Horário da notificação fora do intervalo aceito",
            SyncFailureMessages.forItem("inbox_notification_timestamp_invalid")
        )
    }

    @Test
    fun idempotencyConflictHasADistinctMessage() {
        assertEquals(
            "Identificador já usado para outra notificação",
            SyncFailureMessages.forItem("inbox_idempotency_conflict")
        )
    }

    @Test
    fun unknownServerContentIsNeverReflectedInTheMessage() {
        assertEquals("Falha ao enviar lançamento", SyncFailureMessages.forItem(null))
        assertEquals(
            "Falha ao enviar lançamento",
            SyncFailureMessages.forItem("private-content https://example.invalid opm_private")
        )
    }
}
