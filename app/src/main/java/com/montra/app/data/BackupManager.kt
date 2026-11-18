package com.montra.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class BackupData(
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
    val goals: List<GoalEntity>
)

class BackupFormatException(message: String) : Exception(message)

object BackupManager {
    private const val APP = "Montra"
    private const val LEGACY_APP = "CHUTHIYA"   // backups made before the rename
    private const val VERSION = 1

    fun toJson(data: BackupData): String {
        val root = JSONObject()
        root.put("app", APP)
        root.put("version", VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        root.put("categories", JSONArray().also { arr ->
            data.categories.forEach {
                arr.put(JSONObject().apply {
                    put("id", it.id); put("name", it.name); put("emoji", it.emoji)
                    put("color", it.color); put("type", it.type)
                    put("isDefault", it.isDefault); put("sortOrder", it.sortOrder)
                })
            }
        })
        root.put("transactions", JSONArray().also { arr ->
            data.transactions.forEach {
                arr.put(JSONObject().apply {
                    put("id", it.id); put("amountPaise", it.amountPaise); put("type", it.type)
                    put("categoryId", it.categoryId); put("note", it.note); put("date", it.date)
                    put("paymentMethod", it.paymentMethod); put("createdAt", it.createdAt)
                })
            }
        })
        root.put("budgets", JSONArray().also { arr ->
            data.budgets.forEach {
                arr.put(JSONObject().apply {
                    put("id", it.id); put("categoryId", it.categoryId); put("limitPaise", it.limitPaise)
                })
            }
        })
        root.put("goals", JSONArray().also { arr ->
            data.goals.forEach {
                arr.put(JSONObject().apply {
                    put("id", it.id); put("name", it.name); put("emoji", it.emoji)
                    put("color", it.color); put("targetPaise", it.targetPaise)
                    put("savedPaise", it.savedPaise)
                    if (it.deadline != null) put("deadline", it.deadline)
                    put("createdAt", it.createdAt)
                })
            }
        })
        return root.toString(2)
    }

    fun parse(json: String): BackupData {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw BackupFormatException("This file is not a valid Montra backup.")
        }
        if (root.optString("app") != APP && root.optString("app") != LEGACY_APP) {
            throw BackupFormatException("This file is not a Montra backup.")
        }
        if (root.optInt("version", -1) != VERSION) throw BackupFormatException("Unsupported backup version.")

        try {
            val cats = root.getJSONArray("categories").map { o ->
                CategoryEntity(
                    id = o.getLong("id"), name = o.getString("name"), emoji = o.getString("emoji"),
                    color = o.getLong("color"), type = o.getString("type"),
                    isDefault = o.optBoolean("isDefault", false), sortOrder = o.optInt("sortOrder", 0)
                )
            }
            val catIds = cats.map { it.id }.toSet()
            val txs = root.getJSONArray("transactions").map { o ->
                TransactionEntity(
                    id = o.getLong("id"), amountPaise = o.getLong("amountPaise"), type = o.getString("type"),
                    categoryId = o.getLong("categoryId"), note = o.optString("note", ""),
                    date = o.getLong("date"), paymentMethod = o.optString("paymentMethod", "Cash"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            }
            val budgets = root.getJSONArray("budgets").map { o ->
                BudgetEntity(id = o.getLong("id"), categoryId = o.getLong("categoryId"), limitPaise = o.getLong("limitPaise"))
            }
            val goals = root.getJSONArray("goals").map { o ->
                GoalEntity(
                    id = o.getLong("id"), name = o.getString("name"), emoji = o.getString("emoji"),
                    color = o.getLong("color"), targetPaise = o.getLong("targetPaise"),
                    savedPaise = o.optLong("savedPaise", 0),
                    deadline = if (o.has("deadline") && !o.isNull("deadline")) o.getLong("deadline") else null,
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            }
            if (txs.any { it.categoryId !in catIds }) {
                throw BackupFormatException("Backup is inconsistent: a transaction points to a missing category.")
            }
            return BackupData(cats, txs, budgets.filter { it.categoryId == 0L || it.categoryId in catIds }, goals)
        } catch (e: BackupFormatException) {
            throw e
        } catch (e: Exception) {
            throw BackupFormatException("The backup file is damaged or incomplete.")
        }
    }

    private fun <T> JSONArray.map(block: (JSONObject) -> T): List<T> =
        (0 until length()).map { block(getJSONObject(it)) }

    fun toCsv(data: BackupData): String {
        val names = data.categories.associateBy { it.id }
        val sb = StringBuilder("Date,Type,Category,Amount (INR),Payment Method,Note\n")
        data.transactions.sortedBy { it.date }.forEach { t ->
            val cat = names[t.categoryId]?.name ?: "Unknown"
            val amount = String.format(java.util.Locale.ROOT, "%d.%02d", t.amountPaise / 100, t.amountPaise % 100)
            sb.append(LocalDate.ofEpochDay(t.date)).append(',')
                .append(t.type).append(',')
                .append(csv(cat)).append(',')
                .append(amount).append(',')
                .append(csv(t.paymentMethod)).append(',')
                .append(csv(t.note)).append('\n')
        }
        return sb.toString()
    }

    /** Quotes the field and neutralises spreadsheet formula injection. */
    private fun csv(s: String): String {
        var v = s
        if (v.isNotEmpty() && v[0] in charArrayOf('=', '+', '-', '@')) v = "'$v"
        return "\"" + v.replace("\"", "\"\"") + "\""
    }
}
