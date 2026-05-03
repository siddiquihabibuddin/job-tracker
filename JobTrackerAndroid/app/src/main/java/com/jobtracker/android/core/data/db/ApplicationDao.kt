package com.jobtracker.android.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ApplicationDao {

    @Query("SELECT * FROM applications ORDER BY appliedAt DESC, createdAt DESC")
    fun observeAll(): Flow<List<ApplicationEntity>>

    @Query("SELECT * FROM applications WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ApplicationEntity?>

    @Query("SELECT * FROM applications WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ApplicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ApplicationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ApplicationEntity)

    @Query("DELETE FROM applications WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM applications")
    suspend fun clear()
}
