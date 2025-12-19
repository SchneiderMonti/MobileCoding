package at.ustp.accessgate.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auth_entries")
data class AuthEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val payload: String,
    val createdAt: Long,
    val updatedAt: Long
)