package br.com.openmonetis.companion.util

object SyncFailureMessages {
    fun forItem(code: String?): String = when (code) {
        "inbox_notification_timestamp_invalid" -> "Horário da notificação fora do intervalo aceito"
        "inbox_idempotency_conflict" -> "Identificador já usado para outra notificação"
        // Do not persist arbitrary server messages, which may contain private data.
        else -> "Falha ao enviar lançamento"
    }
}
