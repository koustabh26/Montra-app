package com.montra.app.data

import androidx.room.withTransaction

class Repository(private val db: AppDatabase) {
    private val categoryDao = db.categoryDao()
    private val txDao = db.transactionDao()
    private val budgetDao = db.budgetDao()
    private val goalDao = db.goalDao()

    val categories = categoryDao.observeAll()
    val transactions = txDao.observeAll()
    val budgets = budgetDao.observeAll()
    val goals = goalDao.observeAll()

    suspend fun seedIfEmpty() {
        if (categoryDao.count() == 0) categoryDao.insertAll(DefaultCategories.build())
    }

    // Transactions
    suspend fun saveTransaction(t: TransactionEntity) {
        if (t.id == 0L) txDao.insert(t) else txDao.update(t)
    }

    suspend fun deleteTransaction(t: TransactionEntity) = txDao.delete(t)

    /** Puts a deleted transaction back with its original id (used by Undo). */
    suspend fun restoreTransaction(t: TransactionEntity) {
        txDao.insert(t)
    }

    // Categories
    suspend fun saveCategory(c: CategoryEntity) {
        if (c.id == 0L) categoryDao.insert(c) else categoryDao.update(c)
    }

    /** Returns false when the category can't be deleted (it is the fallback for its type). */
    suspend fun deleteCategory(c: CategoryEntity): Boolean = db.withTransaction {
        val fallbackName = if (c.type == TxType.EXPENSE) FALLBACK_EXPENSE else FALLBACK_INCOME
        if (c.name == fallbackName) return@withTransaction false
        if (txDao.countForCategory(c.id) > 0) {
            val fallback = categoryDao.getAll().firstOrNull { it.type == c.type && it.name == fallbackName }
                ?: CategoryEntity(
                    name = fallbackName,
                    emoji = if (c.type == TxType.EXPENSE) "🧩" else "💰",
                    color = CATEGORY_COLORS[0],
                    type = c.type,
                    isDefault = true
                ).let { it.copy(id = categoryDao.insert(it)) }
            txDao.reassign(c.id, fallback.id)
        }
        budgetDao.deleteForCategory(c.id)
        categoryDao.delete(c)
        true
    }

    // Budgets
    suspend fun saveBudget(b: BudgetEntity) {
        budgetDao.upsert(b)
    }

    suspend fun deleteBudget(b: BudgetEntity) = budgetDao.delete(b)

    // Goals
    suspend fun saveGoal(g: GoalEntity) {
        goalDao.upsert(g)
    }

    suspend fun deleteGoal(g: GoalEntity) = goalDao.delete(g)

    // Backup / restore / reset
    suspend fun snapshot(): BackupData = BackupData(
        categories = categoryDao.getAll(),
        transactions = txDao.getAll(),
        budgets = budgetDao.getAll(),
        goals = goalDao.getAll()
    )

    suspend fun restore(data: BackupData) = db.withTransaction {
        txDao.deleteAll()
        budgetDao.deleteAll()
        goalDao.deleteAll()
        categoryDao.deleteAll()
        categoryDao.insertAll(data.categories)
        txDao.insertAll(data.transactions)
        budgetDao.insertAll(data.budgets)
        goalDao.insertAll(data.goals)
        if (categoryDao.count() == 0) categoryDao.insertAll(DefaultCategories.build())
    }

    suspend fun resetAll() = db.withTransaction {
        txDao.deleteAll()
        budgetDao.deleteAll()
        goalDao.deleteAll()
        categoryDao.deleteAll()
        categoryDao.insertAll(DefaultCategories.build())
    }
}
