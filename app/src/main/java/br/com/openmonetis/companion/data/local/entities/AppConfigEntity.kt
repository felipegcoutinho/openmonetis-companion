package br.com.openmonetis.companion.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_configs")
data class AppConfigEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,

    @ColumnInfo(name = "keywords")
    val keywords: String = "[]", // JSON array of keywords

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
