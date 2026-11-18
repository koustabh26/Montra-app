package com.montra.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object TxType {
    const val EXPENSE = "EXPENSE"
    const val INCOME = "INCOME"
}

val PAYMENT_METHODS = listOf("Cash", "UPI", "Credit Card", "Debit Card", "Net Banking", "Wallet")

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val color: Long,          // ARGB, e.g. 0xFF00E5FF
    val type: String,         // TxType
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)

/** Amounts are stored in paise (1 INR = 100 paise) so arithmetic is exact. date = LocalDate epoch day. */
@Entity(
    tableName = "transactions",
    indices = [Index("categoryId"), Index("date")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountPaise: Long,
    val type: String,
    val categoryId: Long,
    val note: String = "",
    val date: Long,
    val paymentMethod: String = "Cash",
    val createdAt: Long = System.currentTimeMillis()
)

/** categoryId == 0 means the overall monthly budget. One budget per category, recurring monthly. */
@Entity(
    tableName = "budgets",
    indices = [Index(value = ["categoryId"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val limitPaise: Long
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val color: Long,
    val targetPaise: Long,
    val savedPaise: Long = 0,
    val deadline: Long? = null,   // epoch day, optional
    val createdAt: Long = System.currentTimeMillis()
)
