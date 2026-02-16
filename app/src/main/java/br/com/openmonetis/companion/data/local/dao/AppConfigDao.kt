package br.com.openmonetis.companion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.openmonetis.companion.data.local.entities.AppConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: AppConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(configs: List<AppConfigEntity>)

    @Update
    suspend fun update(config: AppConfigEntity)

    @Query("SELECT * FROM app_configs ORDER BY display_name ASC")
    fun getAllFlow(): Flow<List<AppConfigEntity>>

    @Query("SELECT * FROM app_configs ORDER BY display_name ASC")
    suspend fun getAll(): List<AppConfigEntity>

    @Query("SELECT * FROM app_configs WHERE is_enabled = 1 ORDER BY display_name ASC")
    suspend fun getEnabled(): List<AppConfigEntity>

    @Query("SELECT * FROM app_configs WHERE package_name = :packageName")
    suspend fun getByPackageName(packageName: String): AppConfigEntity?

    @Query("UPDATE app_configs SET is_enabled = :enabled WHERE package_name = :packageName")
    suspend fun setEnabled(packageName: String, enabled: Boolean)

    @Query("DELETE FROM app_configs WHERE package_name = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM app_configs")
    suspend fun deleteAll()
}
