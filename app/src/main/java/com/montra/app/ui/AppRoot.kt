@file:OptIn(ExperimentalMaterial3Api::class)

package com.montra.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.montra.app.AppViewModel
import com.montra.app.data.TransactionEntity
import com.montra.app.domain.PeriodMode
import com.montra.app.ui.screens.SetPinDialog
import java.time.LocalDate
import com.montra.app.ui.screens.AnalyticsScreen
import com.montra.app.ui.screens.BudgetsScreen
import com.montra.app.ui.screens.CategoriesScreen
import com.montra.app.ui.screens.DashboardScreen
import com.montra.app.ui.screens.GoalsScreen
import com.montra.app.ui.screens.LockScreen
import com.montra.app.ui.screens.SettingsScreen
import com.montra.app.ui.screens.TransactionSheet
import com.montra.app.ui.screens.TransactionsScreen
import com.montra.app.ui.theme.CColors

private enum class Tab(val label: String, val icon: String) {
    Dashboard("Home", "🏠"),
    Transactions("History", "🧾"),
    Budgets("Budgets", "🎯"),
    Analytics("Insights", "📊"),
    More("More", "⋯")
}

/** Screens reached from the "More" tab. */
private enum class MoreSection(val title: String) {
    Menu("More"), Categories("Categories"), Goals("Savings goals"), Settings("Settings & Backup")
}

@Composable
fun AppRoot(vm: AppViewModel, onBackup: () -> Unit, onRestore: () -> Unit, onExportCsv: () -> Unit) {
    if (vm.locked) {
        LockScreen(vm)
        return
    }

    val categories by vm.categories.collectAsStateWithLifecycle()
    val period by vm.period.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.Dashboard.name) }
    var more by rememberSaveable { mutableStateOf(MoreSection.Menu.name) }
    var sheetOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TransactionEntity?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.messages.collect { m ->
            val result = snackbar.showSnackbar(
                message = m.text,
                actionLabel = m.actionLabel,
                duration = if (m.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) m.onAction?.invoke()
        }
    }

    val current = Tab.valueOf(tab)
    val moreSection = MoreSection.valueOf(more)

    // System back: leave a More sub-screen first, then return to Home, and only then exit the app.
    BackHandler(enabled = current != Tab.Dashboard) {
        if (current == Tab.More && moreSection != MoreSection.Menu) {
            more = MoreSection.Menu.name
        } else {
            tab = Tab.Dashboard.name
        }
    }

    fun openEditor(t: TransactionEntity?) {
        editing = t
        sheetOpen = true
    }

    Scaffold(
        containerColor = CColors.Bg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (current == Tab.More && moreSection != MoreSection.Menu) {
                    Text(
                        "‹",
                        color = CColors.Cyan,
                        fontSize = 30.sp,
                        modifier = Modifier.clip(CircleShape).clickable { more = MoreSection.Menu.name }.padding(horizontal = 10.dp)
                    )
                }
                Text(
                    if (current == Tab.More) moreSection.title else "Montra",
                    color = CColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        bottomBar = {
            // NavigationBar already pads for the system navigation bar; adding it again left a gap.
            NavigationBar(containerColor = CColors.Surface) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = t == current,
                        onClick = {
                            tab = t.name
                            if (t == Tab.More) more = MoreSection.Menu.name
                        },
                        icon = { Text(t.icon, fontSize = 20.sp) },
                        label = { Text(t.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = CColors.Cyan,
                            unselectedTextColor = CColors.TextSecondary,
                            indicatorColor = CColors.Cyan.copy(alpha = 0.18f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (current == Tab.Dashboard || current == Tab.Transactions) {
                Box(
                    Modifier
                        .size(60.dp)
                        .shadow(16.dp, CircleShape, ambientColor = CColors.Cyan, spotColor = CColors.Violet)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(CColors.Cyan, CColors.Violet)))
                        .clickable { openEditor(null) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = Color(0xFF02060D), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        val body = Modifier.fillMaxSize().padding(padding)
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(CColors.Bg, Color(0xFF0A0C1B))))
        ) {
            when (current) {
                Tab.Dashboard -> DashboardScreen(
                    vm, onEdit = { openEditor(it) },
                    onSeeAll = { tab = Tab.Transactions.name },
                    onOpenBudgets = { tab = Tab.Budgets.name },
                    modifier = body
                )
                Tab.Transactions -> TransactionsScreen(vm, onEdit = { openEditor(it) }, modifier = body)
                Tab.Budgets -> BudgetsScreen(vm, modifier = body)
                Tab.Analytics -> AnalyticsScreen(vm, modifier = body)
                Tab.More -> when (moreSection) {
                    MoreSection.Menu -> MoreMenu(onOpen = { more = it.name }, modifier = body)
                    MoreSection.Categories -> CategoriesScreen(vm, modifier = body)
                    MoreSection.Goals -> GoalsScreen(vm, modifier = body)
                    MoreSection.Settings -> SettingsScreen(vm, onBackup, onRestore, onExportCsv, modifier = body)
                }
            }
        }
    }

    if (sheetOpen) {
        TransactionSheet(
            initial = editing,
            categories = categories,
            onDismiss = { sheetOpen = false; editing = null },
            onSave = { vm.saveTransaction(it) },
            onDelete = { vm.deleteTransaction(it) },
            // While looking at a single day, new entries default to that day.
            defaultDate = if (period.mode == PeriodMode.DAY) period.anchor else LocalDate.now()
        )
    }

    // A PIN saved by an older version was only 4 digits: require a 6-digit one before continuing.
    if (vm.pinNeedsUpgrade) {
        SetPinDialog(
            onDismiss = {},
            onConfirm = { vm.enablePin(it) },
            title = "Upgrade to a 6-digit PIN",
            forced = true
        )
    }
}

@Composable
private fun MoreMenu(onOpen: (MoreSection) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf(
        Triple(MoreSection.Categories, "🗂️", "Organise your income and expense categories"),
        Triple(MoreSection.Goals, "🏆", "Save towards the things that matter"),
        Triple(MoreSection.Settings, "⚙️", "App lock, backup, restore and export")
    )
    Column(modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
        items.forEach { (section, icon, subtitle) ->
            com.montra.app.ui.components.GlassCard(
                Modifier.fillMaxWidth(), glow = CColors.Violet, onClick = { onOpen(section) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 28.sp)
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(section.title, color = CColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(subtitle, color = CColors.TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
