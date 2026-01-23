package br.com.opensheets.companion.domain.parser

import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedNotification(
    val merchantName: String? = null,
    val amount: Double? = null,
    val date: Date? = null,
    val cardLastDigits: String? = null,
    val transactionType: String? = null // "Despesa" or "Receita"
)

/**
 * Parser for financial notifications from banking apps.
 * Uses a layered approach: specific bank parsers first, then generic fallback.
 */
@Singleton
class NotificationParser @Inject constructor() {

    // Known bank package names and their parsers
    private val bankParsers = mapOf(
        "com.nu.production" to ::parseNubank,
        "br.com.intermedium" to ::parseInter,
        "com.itau" to ::parseItau,
        "com.bradesco" to ::parseBradesco,
        "br.com.bb.android" to ::parseBB,
        "com.santander.app" to ::parseSantander,
        "com.c6bank.app" to ::parseC6
    )

    fun parse(packageName: String, title: String?, text: String): ParsedNotification {
        // Try specific bank parser first
        val specificParser = bankParsers[packageName]
        if (specificParser != null) {
            val result = specificParser(title, text)
            if (result.amount != null) {
                return result
            }
        }

        // Fallback to generic parser
        return parseGeneric(title, text)
    }

    // ==================== GENERIC PARSER ====================

    private fun parseGeneric(title: String?, text: String): ParsedNotification {
        val fullText = listOfNotNull(title, text).joinToString(" ")

        return ParsedNotification(
            amount = extractAmount(fullText),
            merchantName = extractMerchant(fullText),
            cardLastDigits = extractCardDigits(fullText),
            transactionType = inferTransactionType(fullText)
        )
    }

    private fun extractAmount(text: String): Double? {
        val patterns = listOf(
            Regex("""R\$\s*([\d.]+,\d{2})"""),
            Regex("""R\$\s*([\d]+,\d{2})"""),
            Regex("""RS\s*([\d.]+,\d{2})""", RegexOption.IGNORE_CASE),
            Regex("""([\d.]+,\d{2})\s*reais""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val value = match.groupValues[1]
                    .replace(".", "")
                    .replace(",", ".")
                return value.toDoubleOrNull()
            }
        }
        return null
    }

    private fun extractMerchant(text: String): String? {
        val patterns = listOf(
            Regex("""(?:em|no|na)\s+([A-Z][A-Z0-9\s*]+)""", RegexOption.IGNORE_CASE),
            Regex("""-\s*([A-Z][A-Z0-9\s]+)\s*-"""),
            Regex("""(?:compra|pagamento).*?(?:em|no|na)\s+(.+?)(?:\.|,|$)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1].trim().take(50)
            }
        }
        return null
    }

    private fun extractCardDigits(text: String): String? {
        val patterns = listOf(
            Regex("""final\s*(\d{4})""", RegexOption.IGNORE_CASE),
            Regex("""[•*]+\s*(\d{4})"""),
            Regex("""cartão\s*(\d{4})""", RegexOption.IGNORE_CASE),
            Regex("""(\d{4})\s*$""")
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    private fun inferTransactionType(text: String): String {
        val lowerText = text.lowercase()

        val expenseKeywords = listOf(
            "compra", "débito", "pagamento", "saque", "transferência enviada",
            "pix enviado", "boleto", "fatura", "cobrança"
        )

        val incomeKeywords = listOf(
            "recebido", "recebeu", "depósito", "transferência recebida",
            "pix recebido", "crédito", "estorno", "cashback"
        )

        if (incomeKeywords.any { lowerText.contains(it) }) {
            return "Receita"
        }

        if (expenseKeywords.any { lowerText.contains(it) }) {
            return "Despesa"
        }

        return "Despesa" // Default to expense
    }

    // ==================== BANK-SPECIFIC PARSERS ====================

    private fun parseNubank(title: String?, text: String): ParsedNotification {
        val fullText = listOfNotNull(title, text).joinToString(" ")

        // Padrão: "Compra de R$ 10,00 APROVADA em ESTABELECIMENTO para cartão final 1234"
        val creditPattern = Regex(
            """[Cc]ompra de R\$\s*([\d.,]+)\s*(?:APROVADA|aprovada)\s*em\s+(.+?)\s+para.*final\s*(\d{4})"""
        )

        val creditMatch = creditPattern.find(fullText)
        if (creditMatch != null) {
            return ParsedNotification(
                amount = creditMatch.groupValues[1].replace(".", "").replace(",", ".").toDoubleOrNull(),
                merchantName = creditMatch.groupValues[2].trim(),
                cardLastDigits = creditMatch.groupValues[3],
                transactionType = "Despesa"
            )
        }

        // Padrão débito: "Compra no débito de R$ 10,00 em ESTABELECIMENTO"
        val debitPattern = Regex(
            """[Cc]ompra no débito de R\$\s*([\d.,]+)\s*em\s+(.+)"""
        )

        val debitMatch = debitPattern.find(fullText)
        if (debitMatch != null) {
            return ParsedNotification(
                amount = debitMatch.groupValues[1].replace(".", "").replace(",", ".").toDoubleOrNull(),
                merchantName = debitMatch.groupValues[2].trim(),
                transactionType = "Despesa"
            )
        }

        return parseGeneric(title, text)
    }

    private fun parseInter(title: String?, text: String): ParsedNotification {
        val fullText = listOfNotNull(title, text).joinToString(" ")

        // Padrão: "Você gastou R$ 10,00 no ESTABELECIMENTO. Cartão •••• 1234"
        val pattern = Regex(
            """[Vv]ocê gastou R\$\s*([\d.,]+)\s*(?:no|na|em)\s+(.+?)\.\s*[Cc]artão\s*[•*]+\s*(\d{4})"""
        )

        val match = pattern.find(fullText)
        if (match != null) {
            return ParsedNotification(
                amount = match.groupValues[1].replace(".", "").replace(",", ".").toDoubleOrNull(),
                merchantName = match.groupValues[2].trim(),
                cardLastDigits = match.groupValues[3],
                transactionType = "Despesa"
            )
        }

        return parseGeneric(title, text)
    }

    private fun parseItau(title: String?, text: String): ParsedNotification {
        val fullText = listOfNotNull(title, text).joinToString(" ")

        // Padrão: "Compra aprovada no cartão final 1234 - R$ 10,00 - ESTABELECIMENTO"
        val pattern = Regex(
            """[Cc]ompra aprovada.*final\s*(\d{4})\s*-\s*R\$\s*([\d.,]+)\s*-\s*(.+)"""
        )

        val match = pattern.find(fullText)
        if (match != null) {
            return ParsedNotification(
                amount = match.groupValues[2].replace(".", "").replace(",", ".").toDoubleOrNull(),
                merchantName = match.groupValues[3].trim(),
                cardLastDigits = match.groupValues[1],
                transactionType = "Despesa"
            )
        }

        return parseGeneric(title, text)
    }

    private fun parseBradesco(title: String?, text: String): ParsedNotification {
        val fullText = listOfNotNull(title, text).joinToString(" ")

        // Padrão: "COMPRA CARTAO FINAL 1234 VALOR RS 10,00 ESTABELECIMENTO AUTORIZADA"
        val pattern = Regex(
            """COMPRA.*FINAL\s*(\d{4}).*(?:VALOR|RS)\s*(?:RS)?\s*([\d.,]+)\s*(.+?)\s*AUTORIZADA""",
            RegexOption.IGNORE_CASE
        )

        val match = pattern.find(fullText)
        if (match != null) {
            return ParsedNotification(
                amount = match.groupValues[2].replace(".", "").replace(",", ".").toDoubleOrNull(),
                merchantName = match.groupValues[3].trim(),
                cardLastDigits = match.groupValues[1],
                transactionType = "Despesa"
            )
        }

        return parseGeneric(title, text)
    }

    private fun parseBB(title: String?, text: String): ParsedNotification {
        return parseGeneric(title, text)
    }

    private fun parseSantander(title: String?, text: String): ParsedNotification {
        return parseGeneric(title, text)
    }

    private fun parseC6(title: String?, text: String): ParsedNotification {
        return parseGeneric(title, text)
    }
}
