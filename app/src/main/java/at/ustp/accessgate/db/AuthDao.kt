package at.ustp.accessgate.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthDao {


    @Query("SELECT * FROM auth_enrollments")
    fun getAllEnrollments(): kotlinx.coroutines.flow.Flow<List<AuthEnrollmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveEnrollment(entity: AuthEnrollmentEntity)

    @Query("SELECT * FROM auth_enrollments WHERE methodId = :methodId LIMIT 1")
    suspend fun getEnrollment(methodId: String): AuthEnrollmentEntity?

    @Query("DELETE FROM auth_enrollments WHERE methodId = :methodId")
    suspend fun deleteEnrollment(methodId: String)
}