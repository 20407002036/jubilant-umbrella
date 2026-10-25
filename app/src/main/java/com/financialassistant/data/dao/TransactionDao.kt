package com.financialassistant.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.financialassistant.data.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE receiptNumber = :receiptNumber LIMIT 1")
    suspend fun getTransactionByReceipt(receiptNumber: String): TransactionEntity?

    @Query("""
        SELECT * FROM transactions 
        WHERE recipient LIKE '%' || :query || '%' 
           OR receiptNumber LIKE '%' || :query || '%' 
           OR category LIKE '%' || :query || '%' 
           OR provider LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY timestamp DESC")
    fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE recipient = :recipient ORDER BY timestamp DESC")
    fun getTransactionsByRecipient(recipient: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE simSlot = :simSlot ORDER BY timestamp DESC")
    fun getBySim(simSlot: Int): Flow<List<TransactionEntity>>

    @Query("UPDATE transactions SET simSlot = :simSlot WHERE id = :id")
    suspend fun updateSimSlot(id: Long, simSlot: Int)

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'RECEIVED' AND timestamp >= :startTime")
    fun getIncomingSumSince(startTime: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type != 'RECEIVED' AND timestamp >= :startTime")
    fun getOutgoingSumSince(startTime: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
