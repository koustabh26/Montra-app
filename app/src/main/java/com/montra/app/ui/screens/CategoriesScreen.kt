package com.montra.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import com.montra.app.ui.components.QuickAmountChips
import com.montra.app.ui.components.GhostButton
import com.montra.app.ui.components.FormSheet
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.montra.app.AppViewModel
import com.montra.app.data.CATEGORY_COLORS
import com.montra.app.data.CategoryEntity
import com.montra.app.data.FALLBACK_EXPENSE
import com.montra.app.data.FALLBACK_INCOME
import com.montra.app.data.TxType
import com.montra.app.ui.components.CategoryBadge
import com.montra.app.ui.components.ChoiceChips
import com.montra.app.ui.components.ChuField
import com.montra.app.ui.components.ColorPicker
import com.montra.app.ui.components.EmptyState
import com.montra.app.ui.components.GlassCard
import com.montra.app.ui.components.Muted
import com.montra.app.ui.components.NeonButton
import com.montra.app.ui.theme.CColors

@Composable
fun CategoriesScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val cats by vm.categories.collectAsStateWithLifecycle()
    val txs by vm.transactions.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf("Expense") }
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

    val type = if (tab == "Expense") TxType.EXPENSE else TxType.INCOME
    val usage = remember(txs) { txs.groupingBy { it.categoryId }.eachCount() }
    val list = remember(cats, type) { cats.filter { it.type == type } }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Categories", color = CColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Muted("${cats.size} in total · tap one to edit")
                }
                NeonButton("+ New", onClick = { creating = true })
            }
        }
        item { ChoiceChips(listOf("Expense", "Income"), tab, { tab = it }) }
        if (list.isEmpty()) {
            item { EmptyState("🗂️", "No categories", "Create one to start organising.") }
        } else {
            items(list, key = { it.id }) { c ->
                GlassCard(Modifier.fillMaxWidth(), glow = Color(c.color), onClick = { editing = c }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryBadge(c)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, color = CColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            val n = usage[c.id] ?: 0
                            Muted(if (n == 0) "Unused" else "$n transaction${if (n == 1) "" else "s"}")
                        }
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        CategorySheet(
            existing = editing,
            type = editing?.type ?: type,
            takenNames = cats.filter { it.type == (editing?.type ?: type) && it.id != editing?.id }
                .map { it.name.lowercase() }.toSet(),
            usageCount = editing?.let { usage[it.id] ?: 0 } ?: 0,
            onDismiss = { creating = false; editing = null },
            onSave = { vm.saveCategory(it); creating = false; editing = null },
            onDelete = { vm.deleteCategory(it); creating = false; editing = null }
        )
    }
}

@Composable
private fun CategorySheet(
    existing: CategoryEntity?,
    type: String,
    takenNames: Set<String>,
    usageCount: Int,
    onDismiss: () -> Unit,
    onSave: (CategoryEntity) -> Unit,
    onDelete: (CategoryEntity) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var emoji by remember { mutableStateOf(existing?.emoji ?: "🏷️") }
    var color by remember { mutableStateOf(existing?.color ?: CATEGORY_COLORS[0]) }
    var confirmDelete by remember { mutableStateOf(false) }
    var tried by remember { mutableStateOf(false) }

    val trimmed = name.trim()
    val nameError = when {
        !tried -> null
        trimmed.isEmpty() -> "Enter a name"
        trimmed.lowercase() in takenNames -> "A category with this name already exists"
        else -> null
    }
    val isFallback = existing != null && existing.name in setOf(FALLBACK_EXPENSE, FALLBACK_INCOME)
    val fallbackName = if (type == TxType.EXPENSE) FALLBACK_EXPENSE else FALLBACK_INCOME

    FormSheet(
        (if (existing == null) "New " else "Edit ") + (if (type == TxType.EXPENSE) "expense" else "income") + " category",
        onDismiss
    ) {
        ChuField(name, { if (it.length <= 30) name = it }, "Name", error = nameError)
        ChuField(emoji, { if (it.length <= 8) emoji = it }, "Emoji")
        Text("Colour", color = CColors.TextSecondary, fontSize = 13.sp)
        ColorPicker(CATEGORY_COLORS, color) { color = it }

        NeonButton(
            "Save category",
            onClick = {
                tried = true
                if (trimmed.isNotEmpty() && trimmed.lowercase() !in takenNames) {
                    onSave(
                        CategoryEntity(
                            id = existing?.id ?: 0L,
                            name = trimmed,
                            emoji = emoji.trim().ifEmpty { "🏷️" },
                            color = color,
                            type = type,
                            isDefault = existing?.isDefault ?: false,
                            sortOrder = existing?.sortOrder ?: 1000
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        if (existing != null && !isFallback) {
            if (confirmDelete && usageCount > 0) {
                Text(
                    "$usageCount transaction${if (usageCount == 1) "" else "s"} will move to \"$fallbackName\".",
                    color = CColors.Amber, fontSize = 13.sp
                )
            }
            GhostButton(
                if (confirmDelete) "Tap again to confirm delete" else "Delete category",
                onClick = { if (confirmDelete) onDelete(existing) else confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
                color = CColors.Red
            )
        }
    }
}
