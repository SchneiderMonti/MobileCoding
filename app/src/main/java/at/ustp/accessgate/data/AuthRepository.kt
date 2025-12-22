package at.ustp.accessgate.data

import at.ustp.accessgate.db.AuthDao
import at.ustp.accessgate.db.AuthEntryEntity
import kotlinx.coroutines.flow.Flow

class AuthRepository(private val dao: AuthDao) {

    val entries: Flow<List<AuthEntryEntity>> = dao.getAllEntries()

    suspend fun createEntry(name: String, type: String, payload: String): Long {
        val now = System.currentTimeMillis()
        return dao.insertEntry(
            AuthEntryEntity(
                name = name,
                type = type,
                payload = payload,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    fun observeEntryById(id: Long): Flow<AuthEntryEntity?> = dao.observeEntryById(id)

    suspend fun getEntryById(id: Long): AuthEntryEntity? = dao.getEntryById(id)


    /*
    suspend fun renameEntry(id: Long, newName: String) {
        val existing = dao.getEntryById(id) ?: return
        dao.updateEntry(existing.copy(name = newName, updatedAt = System.currentTimeMillis()))
    }

    suspend fun updatePayload(id: Long, newPayload: String) {
        val existing = dao.getEntryById(id) ?: return
        dao.updateEntry(existing.copy(payload = newPayload, updatedAt = System.currentTimeMillis()))
    }

     */

    suspend fun deleteEntryById(id: Long) {
        dao.deleteEntryById(id)
    }
}
