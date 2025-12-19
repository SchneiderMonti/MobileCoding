package at.ustp.accessgate.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auth_enrollments")
data class AuthEnrollmentEntity(
    @PrimaryKey val methodId: String,
    val payload: String
)