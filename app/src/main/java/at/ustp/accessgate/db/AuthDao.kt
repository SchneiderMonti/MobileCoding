package at.ustp.accessgate.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthDao {

    @Query("SELECT * FROM auth_entries ORDER BY updatedAt DESC")
    fun getAllEntries(): Flow<List<AuthEntryEntity>>

    @Query("SELECT * FROM auth_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): AuthEntryEntity?

    @Insert
    suspend fun insertEntry(entity: AuthEntryEntity): Long

    @Update
    suspend fun updateEntry(entity: AuthEntryEntity)

    @Query("DELETE FROM auth_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)
}