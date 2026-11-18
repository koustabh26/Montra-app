@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.montra.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.montra.app.data.CategoryEntity
import com.montra.app.domain.Period
import com.montra.app.domain.PeriodMode
import com.montra.app.ui.theme.CColors
import com.montra.app.util.Money
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------- 3D-style surfaces ----------

/** A raised glass card: vertical depth gradient, lit top edge, and a coloured glow shadow. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glow: Color = CColors.Violet,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.975f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "cardScale"
    )
    var m = modifier
        .scale(scale)
        .shadow(14.dp, shape, ambientColor = glow.copy(alpha = 0.35f), spotColor = glow.copy(alpha = 0.6f))
        .clip(shape)
        .background(Brush.verticalGradient(listOf(CColors.CardTop, CColors.CardBottom)))
        .border(
            1.dp,
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.03f), glow.copy(alpha = 0.40f))),
            shape
        )
    if (onClick != null) {
        m = m.clickable(interactionSource = source, indication = null, onClick = onClick)
    }
    Column(m.padding(16.dp), content = content)
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(CColors.Cyan, CColors.Violet),
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(16.dp)
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, tween(90), label = "btnScale")
    Box(
        modifier = modifier
            .scale(scale)
            .shadow(if (pressed) 4.dp else 12.dp, shape, ambientColor = colors.first(), spotColor = colors.last())
            .clip(shape)
            .background(Brush.horizontalGradient(if (enabled) colors else listOf(Color(0xFF3A3F5C), Color(0xFF2A2E48))))
            .border(1.dp, Color.White.copy(alpha = 0.25f), shape)
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (enabled) Color(0xFF02060D) else CColors.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = CColors.TextSecondary) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

// ---------- Text helpers ----------

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = CColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier, size: Int = 12) {
    Text(text, color = CColors.TextSecondary, fontSize = size.sp, modifier = modifier)
}

@Composable
fun EmptyState(emoji: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 44.sp)
        Spacer(Modifier.height(8.dp))
        Text(title, color = CColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = CColors.TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

// ---------- Category visuals ----------

@Composable
fun CategoryBadge(category: CategoryEntity?, size: Int = 42) {
    val color = Color(category?.color ?: 0xFF9AA0C3)
    Box(
        Modifier
            .size(size.dp)
            .shadow(8.dp, CircleShape, ambientColor = color, spotColor = color)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0.18f))))
            .border(1.dp, color.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(category?.emoji ?: "❔", fontSize = (size * 0.45).sp)
    }
}

@Composable
fun NeonProgress(progress: Float, color: Color, modifier: Modifier = Modifier, height: Int = 10) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.7f), color)))
        )
    }
}

// ---------- Form widgets ----------

@Composable
fun ChuField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    singleLine: Boolean = true,
    prefix: String? = null,
    error: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onChange(if (numeric) sanitizeAmount(raw) else raw) },
        label = { Text(label) },
        singleLine = singleLine,
        isError = error != null,
        supportingText = error?.let { msg -> { Text(msg) } },
        prefix = prefix?.let { p -> { Text(p) } },
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Keeps only a valid money string: digits, at most one decimal point, 2 decimals, 10 integer digits.
 * A comma is treated as the decimal point because some keyboards emit it instead of ".".
 */
fun sanitizeAmount(raw: String): String {
    val sb = StringBuilder()
    var seenDot = false
    var decimals = 0
    var integers = 0
    for (ch in raw) {
        when {
            ch in '0'..'9' -> {
                if (seenDot) {
                    if (decimals < 2) { sb.append(ch); decimals++ }
                } else if (integers < 10) {
                    sb.append(ch); integers++
                }
            }
            (ch == '.' || ch == ',') && !seenDot -> { sb.append('.'); seenDot = true }
        }
    }
    return sb.toString()
}

/**
 * Bottom sheet for forms. Scrolls, and lifts above the keyboard, so fields are never hidden
 * (this is what the old AlertDialog forms got wrong).
 */
@Composable
fun FormSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = CColors.Surface) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(title, color = CColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

/** One-tap amount suggestions (whole rupees). */
@Composable
fun QuickAmountChips(rupees: List<Long>, onPick: (Long) -> Unit) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        rupees.forEach { r ->
            FilterChip(selected = false, onClick = { onPick(r) }, label = { Text(Money.formatWhole(r * 100)) })
        }
    }
}

@Composable
fun ChoiceChips(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            FilterChip(
                selected = opt == selected,
                onClick = { onSelect(opt) },
                label = { Text(opt) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CColors.Cyan.copy(alpha = 0.22f),
                    selectedLabelColor = CColors.Cyan
                )
            )
        }
    }
}

@Composable
fun CategoryPicker(
    categories: List<CategoryEntity>,
    selectedId: Long?,
    onSelect: (CategoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { c ->
            FilterChip(
                selected = c.id == selectedId,
                onClick = { onSelect(c) },
                label = { Text("${c.emoji} ${c.name}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(c.color).copy(alpha = 0.28f),
                    selectedLabelColor = CColors.TextPrimary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = c.id == selectedId,
                    borderColor = Color.White.copy(alpha = 0.18f),
                    selectedBorderColor = Color(c.color)
                )
            )
        }
    }
}

@Composable
fun ColorPicker(colors: List<Long>, selected: Long, onSelect: (Long) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { c ->
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(c))
                    .border(if (c == selected) BorderStroke(3.dp, Color.White) else BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), CircleShape)
                    .clickable { onSelect(c) }
            )
        }
    }
}

/** Date picker dialog working in LocalDate (the picker itself speaks UTC-midnight millis). */
@Composable
fun ChuDatePicker(initial: LocalDate, onDismiss: () -> Unit, onPicked: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val ms = state.selectedDateMillis
                if (ms != null) onPicked(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate())
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) { DatePicker(state = state) }
}

// ---------- Month switcher ----------

private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

fun YearMonth.label(): String = format(monthFormat)

@Composable
fun MonthSwitcher(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f)).clickable(onClick = onPrev),
            contentAlignment = Alignment.Center
        ) { Text("‹", color = CColors.TextPrimary, fontSize = 22.sp) }
        Text(
            month.label(),
            color = CColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).clickable(onClick = onToday)
        )
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f)).clickable(onClick = onNext),
            contentAlignment = Alignment.Center
        ) { Text("›", color = CColors.TextPrimary, fontSize = 22.sp) }
    }
}

/** Day | Month | Year selector with previous / next arrows. Tapping the date jumps back to today. */
@Composable
fun PeriodSwitcher(
    period: Period,
    today: LocalDate,
    onMode: (PeriodMode) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ChoiceChips(
            options = PeriodMode.entries.map { it.label },
            selected = period.mode.label,
            onSelect = { picked -> PeriodMode.entries.firstOrNull { it.label == picked }?.let(onMode) }
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f)).clickable(onClick = onPrev),
                contentAlignment = Alignment.Center
            ) { Text("‹", color = CColors.TextPrimary, fontSize = 22.sp) }
            Column(
                Modifier.weight(1f).clickable(onClick = onToday),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    period.label(today),
                    color = CColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
                if (!period.contains(today)) {
                    Text("Tap to jump to today", color = CColors.Cyan, fontSize = 11.sp)
                }
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f)).clickable(onClick = onNext),
                contentAlignment = Alignment.Center
            ) { Text("›", color = CColors.TextPrimary, fontSize = 22.sp) }
        }
    }
}

// ---------- Charts ----------

data class Slice(val label: String, val value: Long, val color: Color)

@Composable
fun DonutChart(slices: List<Slice>, centerTop: String, centerBottom: String, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.value }.coerceAtLeast(1L)
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val progress by animateFloatAsState(if (started) 1f else 0f, tween(900), label = "donut")
    Box(modifier.size(180.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 26.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            // Soft base ring gives the donut a recessed 3D track.
            drawArc(Color.White.copy(alpha = 0.06f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            var start = -90f
            slices.forEach { s ->
                val sweep = 360f * s.value / total * progress
                val gap = if (slices.size > 1) 2f else 0f
                drawArc(
                    color = s.color,
                    startAngle = start,
                    sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Butt)
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerTop, color = CColors.TextSecondary, fontSize = 11.sp)
            Text(centerBottom, color = CColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
    }
}

@Composable
fun RingProgress(progress: Float, color: Color, label: String, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(800), label = "ring")
    Box(modifier.size(72.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 9.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(Color.White.copy(alpha = 0.08f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(color, -90f, 360f * animated, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(label, color = CColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

/** Grouped income/expense columns, one group per month. */
@Composable
fun TrendBars(
    labels: List<String>,
    income: List<Long>,
    expense: List<Long>,
    modifier: Modifier = Modifier
) {
    val max = (income + expense).maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val chartHeight = 140.dp
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        labels.forEachIndexed { i, label ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Row(Modifier.height(chartHeight), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Bar(income[i].toFloat() / max, chartHeight, CColors.Lime)
                    Bar(expense[i].toFloat() / max, chartHeight, CColors.Magenta)
                }
                Spacer(Modifier.height(6.dp))
                Text(label, color = CColors.TextSecondary, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun Bar(fraction: Float, chartHeight: androidx.compose.ui.unit.Dp, color: Color) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700), label = "bar")
    Box(
        Modifier
            .width(12.dp)
            .height(maxOf(2.dp, chartHeight * animated))
            .shadow(6.dp, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp), ambientColor = color, spotColor = color)
            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            .background(Brush.verticalGradient(listOf(color, color.copy(alpha = 0.35f))))
    )
}

@Composable
fun LineChart(values: List<Long>, color: Color, modifier: Modifier = Modifier) {
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Canvas(modifier.fillMaxWidth().height(120.dp)) {
        if (values.size < 2) return@Canvas
        val stepX = size.width / (values.size - 1)
        fun y(v: Long) = size.height - (v.toFloat() / max) * (size.height - 8.dp.toPx()) - 4.dp.toPx()
        val line = Path()
        values.forEachIndexed { i, v ->
            val x = stepX * i
            if (i == 0) line.moveTo(x, y(v)) else line.lineTo(x, y(v))
        }
        val area = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)))
        drawPath(line, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
fun LegendRow(color: Color, label: String, value: String, share: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(label, color = CColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(share, color = CColors.TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.width(10.dp))
        Text(value, color = CColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun AmountText(paise: Long, positive: Boolean?, size: Int = 15) {
    val color = when (positive) {
        true -> CColors.Lime
        false -> CColors.Magenta
        null -> CColors.TextPrimary
    }
    val prefix = when (positive) {
        true -> "+"
        false -> "−"
        null -> ""
    }
    Text(prefix + Money.format(paise), color = color, fontWeight = FontWeight.Bold, fontSize = size.sp)
}
