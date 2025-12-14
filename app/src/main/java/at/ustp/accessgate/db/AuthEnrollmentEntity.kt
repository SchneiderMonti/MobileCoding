package at.ustp.accessgate.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auth_enrollments")
data class AuthEnrollmentEntity(
    @PrimaryKey val methodId: String, // e.g. "tap_jingle"
    val payload: String               // e.g. "120,300,110,450"
)