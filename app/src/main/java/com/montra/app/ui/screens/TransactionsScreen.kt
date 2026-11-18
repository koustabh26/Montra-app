@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.montra.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.montra.app.AppViewModel
import com.montra.app.data.CategoryEntity
import com.montra.app.data.PAYMENT_METHODS
import com.montra.app.data.TransactionEntity
import com.montra.app.data.TxType
import com.montra.app.domain.FinanceCalc
import com.montra.app.domain.Period
import com.montra.app.domain.PeriodMode
import com.montra.app.ui.components.PeriodSwitcher
import com.montra.app.ui.components.label
import java.time.YearMonth
import com.montra.app.ui.components.AmountText
import com.montra.app.ui.components.CategoryBadge
import com.montra.app.ui.components.CategoryPicker
import com.montra.app.ui.components.ChoiceChips
import com.montra.app.ui.components.ChuDatePicker
import com.montra.app.ui.components.ChuField
import com.montra.app.ui.components.EmptyState
import com.montra.app.ui.components.GhostButton
import com.montra.app.ui.components.GlassCard
import com.montra.app.ui.components.Muted
import com.montra.app.ui.components.MonthSwitcher
import com.montra.app.ui.components.NeonButton
import com.montra.app.ui.theme.CColors
import com.montra.app.util.Money
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)

fun dayLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> date.format(dayFormat)
}

@Composable
fun TransactionRow(t: TransactionEntity, category: CategoryEntity?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryBadge(category)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                category?.name ?: "Unknown",
                color = CColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val sub = if (t.note.isBlank()) t.paymentMethod else "${t.note} · ${t.paymentMethod}"
            Text(sub, color = CColors.TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        AmountText(t.amountPaise, positive = t.type == TxType.INCOME)
    }
}

@Composable
fun TransactionsScreen(vm: AppViewModel, onEdit: (TransactionEntity) -> Unit, modifier: Modifier = Modifier) {
    val txs by vm.transactions.collectAsStateWithLifecycle()
    val cats by vm.categories.collectAsStateWithLifecycle()
    val period by vm.period.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }

    val catMap = remember(cats) { cats.associateBy { it.id } }
    val periodTx = remember(txs, period) { FinanceCalc.inPeriod(txs, period) }
    val summary = remember(periodTx) { FinanceCalc.summary(periodTx) }

    val filtered = remember(periodTx, query, filter, catMap) {
        val q = query.trim().lowercase()
        periodTx.filter { t ->
            val typeOk = when (filter) {
                "Expenses" -> t.type == TxType.EXPENSE
                "Income" -> t.type == TxType.INCOME
                else -> true
            }
            val textOk = q.isEmpty() ||
                t.note.lowercase().contains(q) ||
                (catMap[t.categoryId]?.name?.lowercase()?.contains(q) == true) ||
                t.paymentMethod.lowercase().contains(q)
            typeOk && textOk
        }
    }
    // Newest day first; every day becomes its own card.
    val days = remember(filtered) { filtered.groupBy { it.date }.toSortedMap(compareByDescending<Long> { it }) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PeriodSwitcher(
                period, today,
                onMode = { vm.setMode(it) },
                onPrev = { vm.shiftPeriod(-1) },
                onNext = { vm.shiftPeriod(1) },
                onToday = { vm.periodToToday() }
            )
        }
        item {
            GlassCard(Modifier.fillMaxWidth(), glow = CColors.Cyan) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Muted("Income")
                        AmountText(summary.income, positive = true, size = 16)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Muted("Transactions")
                        Text("${periodTx.size}", color = CColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Muted("Expenses")
                        AmountText(summary.expense, positive = false, size = 16)
                    }
                }
            }
        }
        item {
            ChuField(query, { query = it }, "Search note, category or payment method")
        }
        item {
            ChoiceChips(listOf("All", "Expenses", "Income"), filter, { filter = it })
        }
        if (days.isEmpty()) {
            item {
                val none = periodTx.isEmpty()
                EmptyState(
                    "🧾",
                    if (none) "Nothing recorded ${periodWord(period)}" else "Nothing matches your filters",
                    if (none) "Tap + to add an expense or income." else "Try a different search or filter."
                )
            }
        } else {
            var lastMonth: YearMonth? = null
            days.forEach { (epochDay, list) ->
                val date = LocalDate.ofEpochDay(epochDay)
                if (period.mode == PeriodMode.YEAR) {
                    val ym = YearMonth.from(date)
                    if (ym != lastMonth) {
                        lastMonth = ym
                        val monthSummary = FinanceCalc.summary(filtered.filter { YearMonth.from(LocalDate.ofEpochDay(it.date)) == ym })
                        item(key = "m$ym") { MonthHeader(ym, monthSummary.income, monthSummary.expense) }
                    }
                }
                item(key = "d$epochDay") { DayCard(date, list, catMap, today, onEdit) }
            }
        }
    }
}

private fun periodWord(p: Period): String = when (p.mode) {
    PeriodMode.DAY -> "on this day"
    PeriodMode.MONTH -> "this month"
    PeriodMode.YEAR -> "this year"
}

@Composable
private fun MonthHeader(month: YearMonth, income: Long, expense: Long) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(month.label(), color = CColors.Cyan, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
        Text(
            "Spent ${Money.formatWhole(expense)}  ·  Earned ${Money.formatWhole(income)}",
            color = CColors.TextSecondary, fontSize = 12.sp
        )
    }
}

/** One calendar day: the date, that day's totals, then its transactions. */
@Composable
private fun DayCard(
    date: LocalDate,
    list: List<TransactionEntity>,
    catMap: Map<Long, CategoryEntity>,
    today: LocalDate,
    onEdit: (TransactionEntity) -> Unit
) {
    val spent = list.filter { it.type == TxType.EXPENSE }.sumOf { it.amountPaise }
    val earned = list.filter { it.type == TxType.INCOME }.sumOf { it.amountPaise }
    GlassCard(Modifier.fillMaxWidth(), glow = if (date == today) CColors.Cyan else CColors.Violet) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                dayLabel(date, today),
                color = CColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Column(horizontalAlignment = Alignment.End) {
                if (spent > 0) Text("−" + Money.format(spent), color = CColors.Magenta, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (earned > 0) Text("+" + Money.format(earned), color = CColors.Lime, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(4.dp))
        list.forEach { t -> TransactionRow(t, catMap[t.categoryId]) { onEdit(t) } }
    }
}

/** Bottom sheet to add ([initial] == null) or edit a transaction. */
@Composable
fun TransactionSheet(
    initial: TransactionEntity?,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
    defaultDate: LocalDate = LocalDate.now()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var type by remember { mutableStateOf(initial?.type ?: TxType.EXPENSE) }
    var amountText by remember { mutableStateOf(initial?.let { Money.toInput(it.amountPaise) } ?: "") }
    var categoryId by remember { mutableStateOf(initial?.categoryId) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var date by remember { mutableStateOf(initial?.let { LocalDate.ofEpochDay(it.date) } ?: defaultDate) }
    var method by remember { mutableStateOf(initial?.paymentMethod ?: "UPI") }
    var showDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var triedSave by remember { mutableStateOf(false) }

    val typeCategories = remember(categories, type) { categories.filter { it.type == type } }
    val amountPaise = Money.parsePaise(amountText)
    val amountError = if (triedSave && amountPaise == null) "Enter an amount greater than 0 (max 2 decimals)" else null
    val categoryMissing = triedSave && typeCategories.none { it.id == categoryId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CColors.Surface
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (initial == null) "New transaction" else "Edit transaction",
                color = CColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold
            )
            ChoiceChips(
                options = listOf("Expense", "Income"),
                selected = if (type == TxType.EXPENSE) "Expense" else "Income",
                onSelect = {
                    val newType = if (it == "Expense") TxType.EXPENSE else TxType.INCOME
                    if (newType != type) {
                        type = newType
                        categoryId = null
                    }
                }
            )
            ChuField(amountText, { amountText = it }, "Amount", numeric = true, prefix = "₹", error = amountError)

            Text("Category", color = CColors.TextSecondary, fontSize = 13.sp)
            CategoryPicker(typeCategories, categoryId, { categoryId = it.id })
            if (categoryMissing) Text("Pick a category", color = CColors.Red, fontSize = 12.sp)

            ChuField(note, { if (it.length <= 120) note = it }, "Note (optional)")

            Text("Payment method", color = CColors.TextSecondary, fontSize = 13.sp)
            ChoiceChips(PAYMENT_METHODS, method, { method = it })

            GhostButton(
                "📅  ${dayLabel(date)}",
                onClick = { showDate = true },
                modifier = Modifier.fillMaxWidth(),
                color = CColors.Cyan
            )

            Spacer(Modifier.height(4.dp))
            NeonButton(
                text = if (initial == null) "Save transaction" else "Save changes",
                onClick = {
                    triedSave = true
                    val cid = categoryId
                    if (amountPaise != null && cid != null && typeCategories.any { it.id == cid }) {
                        onSave(
                            TransactionEntity(
                                id = initial?.id ?: 0L,
                                amountPaise = amountPaise,
                                type = type,
                                categoryId = cid,
                                note = note.trim(),
                                date = date.toEpochDay(),
                                paymentMethod = method,
                                createdAt = initial?.createdAt ?: System.currentTimeMillis()
                            )
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (initial != null) {
                if (!confirmDelete) {
                    GhostButton("Delete", { confirmDelete = true }, Modifier.fillMaxWidth(), color = CColors.Red)
                } else {
                    GhostButton(
                        "Tap again to confirm delete",
                        { onDelete(initial); onDismiss() },
                        Modifier.fillMaxWidth(),
                        color = CColors.Red
                    )
                }
            }
        }
    }

    if (showDate) {
        ChuDatePicker(date, onDismiss = { showDate = false }, onPicked = { date = it })
    }
}
