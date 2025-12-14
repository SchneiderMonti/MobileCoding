package at.ustp.accessgate.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AuthDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveEnrollment(entity: AuthEnrollmentEntity)

    @Query("SELECT * FROM auth_enrollments WHERE methodId = :methodId LIMIT 1")
    suspend fun getEnrollment(methodId: String): AuthEnrollmentEntity?

    @Query("DELETE FROM auth_enrollments WHERE methodId = :methodId")
    suspend fun deleteEnrollment(methodId: String)
}