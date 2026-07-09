package com.financialassistant.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.financialassistant.data.entities.RecipientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipientDao {
    @Query("SELECT * FROM recipients ORDER BY name ASC")
    fun getAllRecipients(): Flow<List<RecipientEntity>>

    @Query("SELECT * FROM recipients WHERE name = :name LIMIT 1")
    suspend fun getRecipientByName(name: String): RecipientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipient(recipient: RecipientEntity): Long

    @Update
    suspend fun updateRecipient(recipient: RecipientEntity)
}
