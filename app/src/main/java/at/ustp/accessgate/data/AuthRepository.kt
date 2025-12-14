package at.ustp.accessgate.data

import at.ustp.accessgate.db.AuthDao
import at.ustp.accessgate.db.AuthEnrollmentEntity

class AuthRepository(private val dao: AuthDao) {

    suspend fun saveTapEnrollment(intervals: List<Long>) {
        val payload = intervals.joinToString(",")
        dao.saveEnrollment(AuthEnrollmentEntity("tap_jingle", payload))
    }

    suspend fun loadTapEnrollment(): List<Long>? {
        val entity = dao.getEnrollment("tap_jingle") ?: return null
        return entity.payload.split(",").map { it.toLong() }
    }

    suspend fun clearTapEnrollment() {
        dao.deleteEnrollment("tap_jingle")
    }
}