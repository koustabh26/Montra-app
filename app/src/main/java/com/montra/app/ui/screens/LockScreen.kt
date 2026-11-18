package com.montra.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.montra.app.AppViewModel
import com.montra.app.security.LockoutPolicy
import com.montra.app.security.PinPolicy
import com.montra.app.ui.components.NeonButton
import com.montra.app.ui.theme.CColors
import kotlinx.coroutines.delay
import java.util.Locale

private fun countdown(seconds: Int): String =
    if (seconds >= 60) String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60) else "${seconds}s"

@Composable
fun LockScreen(vm: AppViewModel) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val lockout = vm.lockoutSecondsLeft
    val strong = vm.pinIsStrong          // false only for a 4-digit PIN left by an older version
    val maxLen = PinPolicy.LENGTH

    // Tick the countdown once a second while locked out.
    LaunchedEffect(lockout) {
        if (lockout > 0) {
            delay(1000)
            vm.refreshLockout()
        }
    }

    fun submit(code: String) {
        if (lockout > 0 || code.length < 4) return
        if (!vm.tryUnlock(code)) {
            error = true
            pin = ""
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CColors.Bg, Color(0xFF0B0E22))))
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("🔒", fontSize = 40.sp)
            Text("Montra", color = CColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)

            val message = when {
                lockout > 0 -> "Too many wrong attempts. Try again in ${countdown(lockout)}"
                error && vm.attemptsLeft in 1 until LockoutPolicy.FREE_ATTEMPTS ->
                    "Incorrect PIN · ${vm.attemptsLeft} attempt${if (vm.attemptsLeft == 1) "" else "s"} left before a timed lockout"
                error && vm.attemptsLeft == 0 -> "Incorrect PIN · each wrong try now adds a longer wait"
                error -> "Incorrect PIN"
                else -> "Enter your $maxLen-digit PIN"
            }
            Text(
                message,
                color = if (error || lockout > 0) CColors.Red else CColors.TextSecondary,
                fontSize = 14.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(maxLen) { i ->
                    val filled = i < pin.length
                    Box(
                        Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (filled) CColors.Cyan else Color.White.copy(alpha = 0.10f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫")).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { key ->
                            if (key.isEmpty()) {
                                Spacer(Modifier.size(68.dp))
                            } else {
                                Key(key, enabled = lockout == 0) {
                                    error = false
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (key == "⌫") {
                                        pin = pin.dropLast(1)
                                    } else if (pin.length < maxLen) {
                                        val next = pin + key
                                        pin = next
                                        // A 6-digit PIN unlocks automatically on the last digit.
                                        if (strong && next.length == maxLen) submit(next)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!strong) {
                // Old 4-digit PIN: needs an explicit button because its length is not fixed.
                Spacer(Modifier.height(4.dp))
                NeonButton("Unlock", onClick = { submit(pin) }, enabled = pin.length >= 4 && lockout == 0)
            }
        }
    }
}

@Composable
private fun Key(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(68.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .shadow(8.dp, CircleShape, ambientColor = CColors.Violet, spotColor = CColors.Violet)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(CColors.CardTop, CColors.CardBottom)))
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = CColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    }
}
