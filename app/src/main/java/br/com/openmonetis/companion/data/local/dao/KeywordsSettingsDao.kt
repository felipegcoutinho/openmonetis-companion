package br.com.openmonetis.companion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.openmonetis.companion.data.local.entities.KeywordsSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KeywordsSettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: KeywordsSettingsEntity)

    @Query("SELECT * FROM keywords_settings WHERE id = 1")
    suspend fun get(): KeywordsSettingsEntity?

    @Query("SELECT * FROM keywords_settings WHERE id = 1")
    fun getFlow(): Flow<KeywordsSettingsEntity?>

    @Query("DELETE FROM keywords_settings")
    suspend fun delete()
}
