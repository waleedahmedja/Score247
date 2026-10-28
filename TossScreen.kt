package com.waleedahmedja.score247.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waleedahmedja.score247.data.model.TossChoice
import com.waleedahmedja.score247.ui.components.S247Button
import com.waleedahmedja.score247.ui.components.S247Card
import com.waleedahmedja.score247.ui.components.S247Screen
import com.waleedahmedja.score247.ui.theme.Ink
import com.waleedahmedja.score247.ui.theme.Ink100
import com.waleedahmedja.score247.ui.theme.Ink200
import com.waleedahmedja.score247.ui.theme.Ink300
import com.waleedahmedja.score247.ui.theme.Pitch
import com.waleedahmedja.score247.ui.theme.ShapeButton
import com.waleedahmedja.score247.viewmodel.MatchViewModel
import kotlin.random.Random

/**
 * Coin toss. Team A is "heads", Team B is "tails". The winner is decided FIRST and the
 * coin is animated to land on the matching face, so what you see is always the result.
 */
@Composable
fun TossScreen(vm: MatchViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()

    var flipping by remember { mutableStateOf(false) }
    // Saveable so rotating the phone after the flip doesn't throw the result away.
    var winner   by rememberSaveable { mutableStateOf<String?>(null) }
    var choice   by rememberSaveable { mutableStateOf<TossChoice?>(null) }
    val rotation = remember { Animatable(0f) }

    // 5 full turns (1800°) lands on the front face; +180° lands on the back.
    LaunchedEffect(flipping) {
        if (flipping) {
            val headsWins = Random.nextBoolean()
            rotation.snapTo(0f)
            rotation.animateTo(
                targetValue   = if (headsWins) 1800f else 1980f,
                animationSpec = tween(1500, easing = FastOutSlowInEasing),
            )
            winner   = if (headsWins) state.teamA else state.teamB
            flipping = false
        }
    }

    val won = winner
    val picked = choice

    S247Screen("Toss", onBack = onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text  = if (won != null) "$won won the toss!" else "Flip the coin",
                style = MaterialTheme.typography.headlineLarge,
                color = if (won != null) Pitch else Ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Heads: ${state.teamA}  •  Tails: ${state.teamB}",
                style = MaterialTheme.typography.bodySmall, color = Ink300, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(36.dp))

            Coin(
                rotation    = rotation.value,
                headsLetter = state.teamA.take(1).uppercase(),
                tailsLetter = state.teamB.take(1).uppercase(),
                enabled     = won == null && !flipping,
                onClick     = { flipping = true },
            )
            if (won == null && !flipping) {
                Spacer(Modifier.height(12.dp))
                Text("Tap the coin", style = MaterialTheme.typography.labelMedium, color = Ink300)
            }
            Spacer(Modifier.height(36.dp))

            if (won != null && picked == null) {
                S247Card {
                    Text("$won chooses to…", style = MaterialTheme.typography.headlineMedium, color = Ink,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ChoiceTile("Bat", "Go in first", Modifier.weight(1f)) { choice = TossChoice.BAT }
                        ChoiceTile("Bowl", "Field first", Modifier.weight(1f)) { choice = TossChoice.BOWL }
                    }
                }
            }

            if (won != null && picked != null) {
                S247Card {
                    Text(
                        "$won will ${if (picked == TossChoice.BAT) "bat" else "bowl"} first",
                        style = MaterialTheme.typography.headlineMedium, color = Ink,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(20.dp))
                    S247Button(text = "Confirm & Continue", onClick = { vm.performToss(won, picked); onDone() })
                    Spacer(Modifier.height(10.dp))
                    TextButton(
                        onClick  = { choice = null; winner = null },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Re-toss", color = Ink300) }
                }
            }
        }
    }
}

/** A gold coin that spins around its vertical axis; the face shown follows the angle. */
@Composable
private fun Coin(
    rotation: Float,
    headsLetter: String,
    tailsLetter: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val showingTails = (rotation % 360f) in 90f..270f

    Box(
        Modifier
            .size(160.dp)
            .graphicsLayer {
                rotationY      = rotation
                cameraDistance = 14f * density   // keeps the 3-D flip from looking squashed
            }
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFFF8DE7E), Color(0xFFD9A21B))))
            .border(4.dp, Color(0xFFB8860B), CircleShape)
            .semantics { contentDescription = "Coin. Tap to flip." }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // The back face is mirrored by the 3-D rotation; flip the content back so it reads correctly.
        // The small HEADS/TAILS label matters when both team names start with the same letter.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { rotationY = if (showingTails) 180f else 0f },
        ) {
            Text(
                text = if (showingTails) tailsLetter else headsLetter,
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF5C4300),
            )
            Text(
                text = if (showingTails) "TAILS" else "HEADS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5C4300),
            )
        }
    }
}

@Composable
private fun ChoiceTile(title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(92.dp)
            .clip(ShapeButton)
            .background(Ink100)
            .border(1.dp, Ink200, ShapeButton)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Ink300)
        }
    }
}
