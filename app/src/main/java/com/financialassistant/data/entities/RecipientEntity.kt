package com.financialassistant.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recipients",
    indices = [Index(value = ["name"], unique = true)]
)
data class RecipientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val totalTransactionsCount: Int = 0,
    val totalAmountSpent: Double = 0.0, // Cumulative value for analysis
    val defaultCategory: String? = null // Auto-suggested category
)
