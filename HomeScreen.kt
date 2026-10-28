// HomeScreen.kt: the first screen the user sees.
//
// What the user sees: the app logo and name, and ONE card that changes with the state of the match:
//  - No match yet           -> a "Start New Match" button.
//  - A match is under way   -> a "Match in progress" card with a live score and a "Resume" button.
//  - The last match is done -> its result, a "View Scorecard" button and a "New Match" button.
// Below the card, a "History & stats" link appears once at least one match has been finished.
//
// Words used here:
//  - Composable: a function marked @Composable that DRAWS part of the screen.
//  - State: a value the screen is watching. When it changes, the screen redraws itself.
//  - Lambda / callback (onNewMatch: () -> Unit): a button action handed in from outside. This
//    screen does not know where "New match" leads; AppNavigation decides that.
package com.waleedahmedja.score247.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waleedahmedja.score247.data.model.MatchPhase
import com.waleedahmedja.score247.data.model.MatchState
import com.waleedahmedja.score247.ui.components.AppLogo
import com.waleedahmedja.score247.ui.components.S247Button
import com.waleedahmedja.score247.ui.components.S247ButtonSecondary
import com.waleedahmedja.score247.ui.components.S247Card
import com.waleedahmedja.score247.ui.components.SystemBarIcons
import com.waleedahmedja.score247.ui.theme.AmberTint
import com.waleedahmedja.score247.ui.theme.Amber
import com.waleedahmedja.score247.ui.theme.Ink
import com.waleedahmedja.score247.ui.theme.Ink300
import com.waleedahmedja.score247.ui.theme.Ink50
import com.waleedahmedja.score247.ui.theme.Ink500
import com.waleedahmedja.score247.ui.theme.IsDarkTheme
import com.waleedahmedja.score247.ui.theme.Paper
import com.waleedahmedja.score247.ui.theme.Pitch
import com.waleedahmedja.score247.ui.theme.PitchTint
import com.waleedahmedja.score247.ui.theme.Red
import com.waleedahmedja.score247.ui.theme.ShapeButton
import com.waleedahmedja.score247.ui.theme.ShapeDialog
import com.waleedahmedja.score247.ui.theme.ShapeIconMd
import com.waleedahmedja.score247.ui.theme.ShapePill
import com.waleedahmedja.score247.viewmodel.MatchViewModel

/** Which of the three cards the home screen shows, worked out once from the match phase. */
private enum class HomeCard { FRESH, IN_PROGRESS, DONE }

// vm = the ViewModel (the shared holder of match data); the on... parameters are the actions
// that move to other screens.
@Composable
fun HomeScreen(
    vm: MatchViewModel,
    onNewMatch: () -> Unit,
    onResume:   () -> Unit,
    onSummary:  () -> Unit,
    onHistory:  () -> Unit,
    onAbout:    () -> Unit,
) {
    // "collectAsStateWithLifecycle" turns the ViewModel's live data into screen state, and stops
    // listening while the app is in the background. "by" lets us use `state` as a plain value.
    val state by vm.state.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    // Whether the "Start new match?" warning dialog is open. "remember" keeps the value between
    // redraws; mutableStateOf makes the screen redraw when it changes.
    var showConfirm by remember { mutableStateOf(false) }

    // Setup phase = nothing started; COMPLETED = a finished match; anything else = in progress.
    val card = when (state.phase) {
        MatchPhase.SETUP     -> HomeCard.FRESH
        MatchPhase.COMPLETED -> HomeCard.DONE
        else                 -> HomeCard.IN_PROGRESS
    }

    // The app draws behind the clock/battery bar, so we choose dark or light icons for that bar
    // depending on whether the screen behind it is light or dark.
    SystemBarIcons(statusDarkIcons = !IsDarkTheme, navDarkIcons = !IsDarkTheme)

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink50)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        // Hero + card scroll together on short screens; the footer stays pinned.
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(32.dp))
            AppLogo()
            Spacer(Modifier.height(24.dp))
            Text("Score247", style = MaterialTheme.typography.displaySmall, color = Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text("Street cricket. Your rules.", style = MaterialTheme.typography.bodyLarge, color = Ink300, textAlign = TextAlign.Center)
            Spacer(Modifier.height(44.dp))

            // Fades smoothly from one card to another when the match state changes.
            AnimatedContent(
                targetState    = card,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label          = "home_card",
            ) { current ->
                when (current) {
                    // Match under way: show the resume card. "New" asks for confirmation first.
                    HomeCard.IN_PROGRESS -> InProgressCard(
                        state    = state,
                        onResume = onResume,
                        onNew    = { showConfirm = true },
                    )
                    HomeCard.DONE -> S247Card {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(ShapeIconMd).background(AmberTint), contentAlignment = Alignment.Center) {
                                Text("🏆", fontSize = 18.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Last match", style = MaterialTheme.typography.titleLarge, color = Ink)
                                Text(state.result, style = MaterialTheme.typography.bodySmall, color = Pitch, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        S247Button(text = "View Scorecard", onClick = onSummary)
                        Spacer(Modifier.height(10.dp))
                        // Starting fresh from a finished match: reset first so nothing stale carries over.
                        S247ButtonSecondary(text = "New Match", onClick = { vm.newMatch(); onNewMatch() })
                    }
                    // Nothing to resume: a single big button.
                    HomeCard.FRESH -> S247Button(text = "Start New Match", onClick = onNewMatch)
                }
            }
            // Only shown once at least one match has been finished and saved.
            if (history.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onHistory) {
                    Text("History & stats (${history.size})", color = Pitch, style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // Footer: opens the About and legal screen (privacy policy, terms, licence, GitHub link).
        TextButton(onClick = onAbout, modifier = Modifier.fillMaxWidth()) {
            Text("About and legal  •  Built by waleedahmedja", style = MaterialTheme.typography.labelSmall, color = Ink300)
        }
    }

    // The warning dialog. Starting a new match throws away the match in progress, so we ask first.
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor   = Paper,
            shape            = ShapeDialog,
            title  = { Text("Start new match?", style = MaterialTheme.typography.headlineMedium, color = Ink) },
            text   = { Text("This will permanently delete the current match.", style = MaterialTheme.typography.bodyMedium, color = Ink500) },
            confirmButton = {
                Button(
                    onClick = { showConfirm = false; vm.newMatch(); onNewMatch() },
                    colors  = ButtonDefaults.buttonColors(containerColor = Red),
                    shape   = ShapeButton,
                ) { Text("Yes, start fresh") }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancel", color = Ink300) } },
        )
    }
}

/** The card shown while a match is under way: who is playing, the stage, the live score. */
@Composable
private fun InProgressCard(state: MatchState, onResume: () -> Unit, onNew: () -> Unit) {
    // A short, friendly name for the stage the match has reached.
    val phaseLabel = when (state.phase) {
        MatchPhase.TOSS            -> "Toss"
        MatchPhase.SETUP_INNINGS_1 -> "Choosing players"
        MatchPhase.INNINGS_1       -> "1st innings"
        MatchPhase.INNINGS_BREAK,
        MatchPhase.SETUP_INNINGS_2 -> "Innings break"
        MatchPhase.INNINGS_2       -> "2nd innings"
        else                       -> ""
    }
    // Live score line, e.g. "Team A  34/2 (3.4 ov)", when an innings is under way.
    val live = (if (state.phase == MatchPhase.INNINGS_2) state.innings2 else state.innings1)
        ?.let { "${it.battingTeam}  ${it.runs}/${it.wickets} (${it.oversDisplay} ov)" }

    S247Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(ShapeIconMd).background(PitchTint), contentAlignment = Alignment.Center) {
                Text("▶", fontSize = 16.sp, color = Pitch)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Match in progress", style = MaterialTheme.typography.titleLarge, color = Ink)
                Text("${state.teamA} vs ${state.teamB}", style = MaterialTheme.typography.bodySmall, color = Ink300)
            }
        }
        Spacer(Modifier.height(16.dp))
        if (phaseLabel.isNotEmpty()) {
            Surface(shape = ShapePill, color = PitchTint) {
                Text(phaseLabel, style = MaterialTheme.typography.labelSmall, color = Pitch,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }
        }
        if (live != null) {
            Spacer(Modifier.height(10.dp))
            Text(live, style = MaterialTheme.typography.titleMedium, color = Amber)
        }
        Spacer(Modifier.height(18.dp))
        S247Button(text = "Resume Match", onClick = onResume)
        Spacer(Modifier.height(10.dp))
        S247ButtonSecondary(text = "Start New Match", onClick = onNew)
    }
}
