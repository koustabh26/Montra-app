package com.montra.app.data

/** Name of the catch-all categories used when a category with transactions is deleted. */
const val FALLBACK_EXPENSE = "Miscellaneous"
const val FALLBACK_INCOME = "Other Income"

val CATEGORY_COLORS: List<Long> = listOf(
    0xFF00E5FF, 0xFF8B5CF6, 0xFFFF3D9A, 0xFF39FFB0, 0xFFFFC53D, 0xFFFF6B4A,
    0xFF4D9BFF, 0xFFB388FF, 0xFF2DD4BF, 0xFFFB7185, 0xFFA3E635, 0xFFF59E0B
)

private fun pick(i: Int): Long = CATEGORY_COLORS[i % CATEGORY_COLORS.size]

object DefaultCategories {
    private val expense = listOf(
        "Food & Dining" to "🍽️",
        "Groceries" to "🛒",
        "Rent & Housing" to "🏠",
        "Utilities" to "💡",
        "Mobile & Internet" to "📱",
        "Transport" to "🚌",
        "Fuel" to "⛽",
        "Vehicle Maintenance" to "🔧",
        "Health & Medical" to "🏥",
        "Insurance" to "🛡️",
        "Education" to "🎓",
        "Shopping" to "🛍️",
        "Clothing" to "👕",
        "Entertainment" to "🎬",
        "Travel" to "✈️",
        "Subscriptions" to "📺",
        "Personal Care" to "💇",
        "Fitness" to "🏋️",
        "Gifts & Donations" to "🎁",
        "Kids & Family" to "👪",
        "Pets" to "🐾",
        "EMI & Loans" to "🏦",
        "Taxes" to "🧾",
        "Investments" to "📈",
        "Home Maintenance" to "🛠️",
        "Festivals & Events" to "🪔",
        "Bills & Fees" to "📄",
        FALLBACK_EXPENSE to "🧩"
    )

    private val income = listOf(
        "Salary" to "💼",
        "Freelance" to "💻",
        "Business" to "🏪",
        "Interest" to "🏦",
        "Dividends" to "📈",
        "Rental Income" to "🏘️",
        "Bonus" to "🎉",
        "Gifts Received" to "🎁",
        "Refunds" to "↩️",
        FALLBACK_INCOME to "💰"
    )

    fun build(): List<CategoryEntity> {
        val list = ArrayList<CategoryEntity>()
        expense.forEachIndexed { i, (name, emoji) ->
            list += CategoryEntity(
                name = name, emoji = emoji, color = pick(i),
                type = TxType.EXPENSE, isDefault = true, sortOrder = i
            )
        }
        income.forEachIndexed { i, (name, emoji) ->
            list += CategoryEntity(
                name = name, emoji = emoji, color = pick(i + 3),
                type = TxType.INCOME, isDefault = true, sortOrder = i
            )
        }
        return list
    }
}
