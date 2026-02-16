package br.com.openmonetis.companion.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to store global keywords settings for notification capture.
 * Uses a singleton pattern (id = 1) since there's only one global config.
 * Keywords are stored as comma-separated strings for simplicity.
 */
@Entity(tableName = "keywords_settings")
data class KeywordsSettingsEntity(
    @PrimaryKey
    val id: Int = 1,

    @ColumnInfo(name = "trigger_keywords")
    val triggerKeywords: String = DEFAULT_TRIGGER_KEYWORDS
) {
    companion object {
        const val DEFAULT_TRIGGER_KEYWORDS = "compra,R$,pix,transferência,débito,crédito,saque,pagamento,boleto,fatura"

        fun fromKeywordList(triggerList: List<String>): KeywordsSettingsEntity {
            return KeywordsSettingsEntity(
                triggerKeywords = triggerList.joinToString(",")
            )
        }
    }

    fun getTriggerKeywordsList(): List<String> = triggerKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

