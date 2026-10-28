package com.waleedahmedja.score247.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waleedahmedja.score247.data.model.BallEvent
import com.waleedahmedja.score247.data.model.DismissalKind
import com.waleedahmedja.score247.data.model.Innings
import com.waleedahmedja.score247.data.model.MatchRules
import com.waleedahmedja.score247.data.model.Player
import com.waleedahmedja.score247.engine.ScoringEngine
import com.waleedahmedja.score247.ui.components.BallDot
import com.waleedahmedja.score247.ui.components.OptionChip
import com.waleedahmedja.score247.ui.components.RunTile
import com.waleedahmedja.score247.ui.components.S247ButtonDanger
import com.waleedahmedja.score247.ui.components.S247Card
import com.waleedahmedja.score247.ui.components.S247Divider
import com.waleedahmedja.score247.ui.components.SectionLabel
import com.waleedahmedja.score247.ui.components.SystemBarIcons
import com.waleedahmedja.score247.ui.components.ToastHost
import com.waleedahmedja.score247.ui.theme.Amber
import com.waleedahmedja.score247.ui.theme.AmberTint
import com.waleedahmedja.score247.ui.theme.HeaderAccent
import com.waleedahmedja.score247.ui.theme.HeaderAccentBg
import com.waleedahmedja.score247.ui.theme.HeaderBg
import com.waleedahmedja.score247.ui.theme.HeaderChip
import com.waleedahmedja.score247.ui.theme.HeaderDim
import com.waleedahmedja.score247.ui.theme.HeaderMuted
import com.waleedahmedja.score247.ui.theme.Ink
import com.waleedahmedja.score247.ui.theme.Ink100
import com.waleedahmedja.score247.ui.theme.Ink200
import com.waleedahmedja.score247.ui.theme.Ink300
import com.waleedahmedja.score247.ui.theme.Ink50
import com.waleedahmedja.score247.ui.theme.Ink500
import com.waleedahmedja.score247.ui.theme.IsDarkTheme
import com.waleedahmedja.score247.ui.theme.OnPitch
import com.waleedahmedja.score247.ui.theme.Paper
import com.waleedahmedja.score247.ui.theme.Pitch
import com.waleedahmedja.score247.ui.theme.Red
import com.waleedahmedja.score247.ui.theme.ShapeButton
import com.waleedahmedja.score247.ui.theme.ShapeCard
import com.waleedahmedja.score247.ui.theme.ShapeDialog
import com.waleedahmedja.score247.ui.theme.ShapeIconSm
import com.waleedahmedja.score247.ui.theme.ShapePill
import com.waleedahmedja.score247.util.noDecimal
import com.waleedahmedja.score247.util.oneDecimal
import com.waleedahmedja.score247.util.requiredRate
import com.waleedahmedja.score247.viewmodel.MatchViewModel

// ─────────────────────────────────────────────────────────────────────────────
// SCORING SCREEN — the one screen used during play
//   header   : big animated score, run rates, target / free-hit chips
//   list     : last-ball banner · batters · bowler · run pad · this over
//   dialogs  : extras · wicket (how out) · next batter · bowler
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ScoringScreen(inningsNum: Int, vm: MatchViewModel, onComplete: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val innings = state.inningsFor(inningsNum)
    val rules   = state.rules
    val haptic  = LocalHapticFeedback.current

    // The scoreboard header is dark in both themes, so status-bar icons must be light.
    SystemBarIcons(statusDarkIcons = false, navDarkIcons = !IsDarkTheme)

    // Matches last a long time and the scorer only taps now and then: stop the screen from
    // going to sleep while this screen is showing, and put it back to normal when leaving.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    var showExtras by remember { mutableStateOf(false) }
    var showWicket by remember { mutableStateOf(false) }
    var showBowler by remember { mutableStateOf(false) }

    // Leave the screen the moment this innings is complete (also covers restore-after-restart).
    LaunchedEffect(innings?.complete) {
        if (innings?.complete == true) onComplete()
    }

    // After the 6th legal ball the scorer is asked who bowls next.
    val balls    = innings?.balls ?: 0
    val awaiting = innings?.awaitingNextBatter ?: false
    LaunchedEffect(balls, awaiting) {
        if (innings != null && balls > 0 && balls % 6 == 0 && !innings.complete && !awaiting) showBowler = true
    }

    if (innings == null) {
        Box(Modifier.fillMaxSize().background(Ink50), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Pitch, strokeWidth = 2.dp)
        }
        return
    }

    val striker    = innings.batters.getOrNull(innings.strikerIndex)
    val nonStriker = innings.batters.getOrNull(innings.nonStrikerIndex)
    val bowler     = innings.bowlers.getOrNull(innings.bowlerIndex)
    val target     = state.targetFor(inningsNum)
    val ballsLeft  = rules.totalOvers * 6 - innings.balls
    val blocked    = innings.awaitingNextBatter          // must pick the next batter first
    val freeHit    = ScoringEngine.isFreeHit(innings, rules)
    val over       = remember(innings.events, rules) { ScoringEngine.currentOver(innings, rules) }
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    fun score(event: BallEvent) = vm.scoreBall(inningsNum, event)

    Box(Modifier.fillMaxSize().background(Ink50)) {
        Column(Modifier.fillMaxSize()) {

            ScoreHeader(innings, rules, inningsNum, target, ballsLeft, freeHit)

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp + bottomInset),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // What just happened
                item(key = "last_event") {
                    LastBallBanner(innings.events.lastOrNull(), innings.events.size, rules)
                }

                // Batters
                item(key = "batters") {
                    S247Card {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Batting", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = Ink)
                            StatCell("R", 36.dp, Ink300); StatCell("B", 36.dp, Ink300); StatCell("SR", 44.dp, Ink300)
                        }
                        Spacer(Modifier.height(8.dp))
                        striker?.let { BatterRow(it, onStrike = true) }
                        // A lone last batter occupies both ends — show them once.
                        if (nonStriker != null && innings.nonStrikerIndex != innings.strikerIndex) {
                            Spacer(Modifier.height(6.dp))
                            BatterRow(nonStriker, onStrike = false)
                        }
                    }
                }

                // Bowler
                item(key = "bowler") {
                    S247Card {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Bowling", style = MaterialTheme.typography.titleLarge, color = Ink)
                                if (bowler != null) {
                                    Text(
                                        "${bowler.name}  •  ${bowler.oversBowled} ov  •  ${bowler.wicketsTaken}/${bowler.runsConceded}" +
                                                if (bowler.ballsBowled > 0) "  •  Econ ${bowler.economy.oneDecimal()}" else "",
                                        style = MaterialTheme.typography.bodySmall, color = Ink500,
                                    )
                                }
                            }
                            TextButton(onClick = { showBowler = true }) {
                                Text("Change", color = Pitch, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                // Scoring is locked until the next batter is chosen
                if (blocked) {
                    item(key = "blocked") {
                        Surface(
                            shape = ShapeCard, color = AmberTint,
                            border = BorderStroke(1.dp, Amber.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Select next batter", style = MaterialTheme.typography.labelLarge, color = Ink)
                                Text("Choose who comes to the crease to carry on scoring.", style = MaterialTheme.typography.bodySmall, color = Ink500)
                            }
                        }
                    }
                }

                // The run pad — big tiles, thumb-friendly
                item(key = "pad") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (0..3).forEach { r ->
                                RunTile(r, enabled = !blocked, modifier = Modifier.weight(1f),
                                    onClick = { score(BallEvent.Runs(r, innings.strikerIndex, innings.bowlerIndex)) })
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (4..6).forEach { r ->
                                RunTile(r, enabled = !blocked, modifier = Modifier.weight(1f),
                                    onClick = { score(BallEvent.Runs(r, innings.strikerIndex, innings.bowlerIndex)) })
                            }
                            S247ButtonDanger(
                                text     = "Wicket",
                                enabled  = !blocked,
                                modifier = Modifier.weight(1f).height(64.dp),
                                onClick  = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showWicket = true },
                            )
                        }
                        // Wides and no-balls are the most common extras, so they get one-tap buttons
                        // (no extra runs). "More…" opens the dialog for byes, leg-byes and wides or
                        // no-balls that also had runs.
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExtraButton("Wide", !blocked, Modifier.weight(1f)) {
                                score(BallEvent.Wide(0, innings.bowlerIndex))
                            }
                            ExtraButton("No ball", !blocked, Modifier.weight(1f)) {
                                score(BallEvent.NoBall(0, innings.strikerIndex, innings.bowlerIndex))
                            }
                            ExtraButton("More…", !blocked, Modifier.weight(1f)) { showExtras = true }
                        }
                        OutlinedButton(
                            onClick  = { vm.undoLastBall(inningsNum) },
                            enabled  = innings.events.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape    = ShapeButton,
                            border   = BorderStroke(1.5.dp, if (innings.events.isNotEmpty()) Amber else Ink200),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null,
                                tint = if (innings.events.isNotEmpty()) Amber else Ink300, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Undo last ball", color = if (innings.events.isNotEmpty()) Amber else Ink300,
                                style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                // This over
                if (over.events.isNotEmpty()) {
                    item(key = "over") {
                        S247Card {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(if (over.complete) "Over complete" else "This over", style = MaterialTheme.typography.titleLarge, color = Ink)
                                SectionLabel("Over ${over.overIndex + 1}")
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val last = over.events.lastIndex
                                over.events.forEachIndexed { idx, ev -> BallDot(ev, animate = idx == last) }
                            }
                        }
                    }
                }
            }
        }

        ToastHost(vm.toast, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp))
    }

    // ── DIALOGS ───────────────────────────────────────────────────────────────

    if (showExtras) {
        ExtrasDialog(
            rules     = rules,
            onDismiss = { showExtras = false },
            onConfirm = { type, r ->
                showExtras = false
                val b = innings.bowlerIndex
                score(when (type) {
                    ExtraType.WIDE    -> BallEvent.Wide(r, b)
                    ExtraType.NO_BALL -> BallEvent.NoBall(r, innings.strikerIndex, b)
                    ExtraType.BYE     -> BallEvent.Bye(r, b)
                    ExtraType.LEG_BYE -> BallEvent.LegBye(r, b)
                })
            },
        )
    }

    if (showWicket && striker != null) {
        WicketDialog(
            striker    = striker,
            nonStriker = if (innings.nonStrikerIndex != innings.strikerIndex) nonStriker else null,
            freeHit    = freeHit,
            onDismiss  = { showWicket = false },
            onConfirm  = { kind, asNonStriker, runs ->
                showWicket = false
                val out = if (asNonStriker) innings.nonStrikerIndex else innings.strikerIndex
                score(BallEvent.Wicket(batter = out, bowler = innings.bowlerIndex, runsOnBall = runs, kind = kind))
            },
        )
    }

    // Next-batter picker: can't be dismissed — scoring is locked until a choice is made
    // (or the wicket is undone, in case it was a mis-tap).
    if (blocked && !innings.complete) {
        val candidates = ScoringEngine.nextBatterCandidates(innings)
        AlertDialog(
            onDismissRequest = {},
            containerColor   = Paper, shape = ShapeDialog,
            title = {
                Column {
                    Text("Who bats next?", style = MaterialTheme.typography.headlineMedium, color = Ink)
                    Text("${innings.battingTeam}  •  ${innings.wickets} down", style = MaterialTheme.typography.bodySmall, color = Ink300)
                }
            },
            text = {
                LazyColumn(Modifier.heightIn(max = 340.dp)) {
                    items(candidates, key = { it }) { index ->
                        val player = innings.batters[index]
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.selectNextBatter(inningsNum, index) }.padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(32.dp).background(Ink100, ShapeIconSm), contentAlignment = Alignment.Center) {
                                Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = Ink500)
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(player.name, style = MaterialTheme.typography.bodyLarge, color = Ink, modifier = Modifier.weight(1f))
                        }
                        if (index != candidates.last()) S247Divider()
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { vm.undoLastBall(inningsNum) }) { Text("Undo wicket", color = Amber) }
            },
        )
    }

    if (showBowler) {
        AlertDialog(
            onDismissRequest = { showBowler = false },
            containerColor   = Paper, shape = ShapeDialog,
            title = {
                Column {
                    Text("Select bowler", style = MaterialTheme.typography.headlineMedium, color = Ink)
                    Text(innings.bowlingTeam, style = MaterialTheme.typography.bodySmall, color = Ink300)
                }
            },
            text = {
                LazyColumn(Modifier.heightIn(max = 340.dp)) {
                    items(innings.bowlers.indices.toList(), key = { it }) { idx ->
                        val player = innings.bowlers[idx]
                        val sel = idx == innings.bowlerIndex
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.setBowler(inningsNum, idx); showBowler = false }.padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(if (sel) Pitch else Ink200))
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(player.name, style = MaterialTheme.typography.bodyLarge,
                                    color = if (sel) Pitch else Ink,
                                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                                if (player.ballsBowled > 0) {
                                    Text("${player.oversBowled} ov  •  ${player.wicketsTaken}/${player.runsConceded}  •  Econ ${player.economy.oneDecimal()}",
                                        style = MaterialTheme.typography.labelSmall, color = if (sel) Pitch else Ink300)
                                }
                            }
                        }
                        if (idx != innings.bowlers.lastIndex) S247Divider()
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showBowler = false }) { Text("Close", color = Ink300) } },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCORE HEADER
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ScoreHeader(
    innings: Innings, rules: MatchRules, inningsNum: Int,
    target: Int?, ballsLeft: Int, freeHit: Boolean,
) {
    // The big number counts up to the new total instead of jumping.
    val animatedRuns by animateIntAsState(
        targetValue   = innings.runs,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label         = "score_anim",
    )
    val runsNeeded = target?.let { it - innings.runs }
    val rrr = if (runsNeeded != null) requiredRate(runsNeeded, ballsLeft) else null

    Column(
        Modifier
            .fillMaxWidth()
            .background(HeaderBg)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 18.dp),
    ) {
        Text(
            "${innings.battingTeam}  •  INNINGS $inningsNum".uppercase(),
            style = MaterialTheme.typography.labelSmall, color = HeaderMuted,
        )
        Spacer(Modifier.height(2.dp))
        Row(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                contentDescription = "${innings.runs} for ${innings.wickets}, ${innings.oversDisplay} of ${rules.totalOvers} overs"
            },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$animatedRuns", style = MaterialTheme.typography.displayLarge, color = Color.White)
                Text("/${innings.wickets}", style = MaterialTheme.typography.displayMedium, color = HeaderDim,
                    modifier = Modifier.padding(bottom = 10.dp, start = 4.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(innings.oversDisplay, style = MaterialTheme.typography.displaySmall, color = Color.White)
                Text("/ ${rules.totalOvers} ov", style = MaterialTheme.typography.labelMedium, color = HeaderDim)
            }
        }

        // Chase progress bar (2nd innings)
        if (target != null && target > 0) {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(4.dp).clip(ShapePill).background(HeaderChip)) {
                Box(
                    Modifier
                        .fillMaxWidth((innings.runs.toFloat() / target).coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(ShapePill)
                        .background(HeaderAccent)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoChip("CRR ${innings.runRate.oneDecimal()}")
            if (runsNeeded != null && runsNeeded > 0 && ballsLeft > 0) {
                InfoChip(
                    "Need $runsNeeded off $ballsLeft" + (rrr?.let { "  •  RRR ${it.oneDecimal()}" } ?: ""),
                    accent = true,
                )
            } else {
                InfoChip("Extras ${innings.extras}")
            }
            if (freeHit) InfoChip("FREE HIT", accent = true)
        }
    }
}

@Composable
private fun InfoChip(text: String, accent: Boolean = false) {
    Box(
        Modifier.clip(ShapePill)
            .background(if (accent) HeaderAccentBg else HeaderChip)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = if (accent) HeaderAccent else HeaderMuted)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BATTER ROW
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StatCell(text: String, width: Dp, color: Color, bold: Boolean = false) {
    Text(
        text, modifier = Modifier.width(width), textAlign = TextAlign.End,
        style = MaterialTheme.typography.labelMedium, color = color,
        fontWeight = if (bold) FontWeight.Bold else null,
    )
}

@Composable
private fun BatterRow(player: Player, onStrike: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // "●" marks who is facing the next ball.
        Text(
            if (onStrike) "●" else "",
            color = Pitch, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(16.dp).semantics { contentDescription = if (onStrike) "On strike" else "" },
        )
        Text(
            player.name, modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (onStrike) FontWeight.Bold else FontWeight.Medium,
            color = Ink, maxLines = 1,
        )
        StatCell("${player.runsScored}", 36.dp, Ink, bold = true)
        StatCell("${player.ballsFaced}", 36.dp, Ink500)
        StatCell(if (player.ballsFaced == 0) "—" else player.strikeRate.noDecimal(), 44.dp, Ink500)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EXTRAS DIALOG — pick the type, then the runs; the button shows the exact total added
// ─────────────────────────────────────────────────────────────────────────────
private enum class ExtraType(val label: String, val hint: String) {
    WIDE("Wide", "Extra runs run on top of the wide"),
    NO_BALL("No ball", "Runs the batter scored off it"),
    BYE("Bye", "Byes run"),
    LEG_BYE("Leg bye", "Leg byes run"),
}

@Composable
private fun ExtrasDialog(rules: MatchRules, onDismiss: () -> Unit, onConfirm: (ExtraType, Int) -> Unit) {
    var type by remember { mutableStateOf(ExtraType.WIDE) }
    var picked by remember { mutableIntStateOf(0) }

    val options = when (type) {
        ExtraType.WIDE    -> 0..4
        ExtraType.NO_BALL -> 0..6
        else              -> 1..4       // a bye of 0 would just be a dot ball
    }
    val runs  = picked.coerceIn(options.first, options.last)
    val total = when (type) {
        ExtraType.WIDE    -> (if (rules.wideGivesRun) 1 else 0) + runs
        ExtraType.NO_BALL -> (if (rules.noBallGivesRun) 1 else 0) + runs
        else              -> runs
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Paper, shape = ShapeDialog,
        title = { Text("Extras", style = MaterialTheme.typography.headlineMedium, color = Ink) },
        text  = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExtraType.values().toList().chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { t -> OptionChip(t.label, selected = t == type, onClick = { type = t }, modifier = Modifier.weight(1f)) }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(type.hint, style = MaterialTheme.typography.bodySmall, color = Ink500)
                options.toList().chunked(4).forEach { rowRuns ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowRuns.forEach { r -> OptionChip("$r", selected = r == runs, onClick = { picked = r }, modifier = Modifier.weight(1f)) }
                        repeat(4 - rowRuns.size) { Spacer(Modifier.weight(1f)) }   // keep columns aligned
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(type, runs) },
                shape   = ShapeButton,
                colors  = ButtonDefaults.buttonColors(containerColor = Pitch, contentColor = OnPitch),
            ) { Text(if (total == 0) "Add" else "Add $total run${if (total == 1) "" else "s"}") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Ink300) } },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// WICKET DIALOG — how out (and, for a run-out, who and how many runs were completed)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WicketDialog(
    striker: Player,
    nonStriker: Player?,
    freeHit: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (kind: DismissalKind, nonStrikerOut: Boolean, runs: Int) -> Unit,
) {
    // On a free hit only a run-out is possible.
    val kinds = if (freeHit) listOf(DismissalKind.RUN_OUT) else DismissalKind.values().toList()
    var kind by remember { mutableStateOf(kinds.first()) }
    var nonStrikerOut by remember { mutableStateOf(false) }
    var runs by remember { mutableIntStateOf(0) }
    val isRunOut = kind == DismissalKind.RUN_OUT
    val outName  = if (isRunOut && nonStrikerOut && nonStriker != null) nonStriker.name else striker.name

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Paper, shape = ShapeDialog,
        title = {
            Column {
                Text("Wicket", style = MaterialTheme.typography.headlineMedium, color = Red)
                Text("$outName is out", style = MaterialTheme.typography.bodySmall, color = Ink500)
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (freeHit) Text("Free hit — only a run-out counts.", style = MaterialTheme.typography.bodySmall, color = Amber)
                SectionLabel("How out")
                kinds.chunked(3).forEach { group ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        group.forEach { k -> OptionChip(k.label, selected = k == kind, onClick = { kind = k }, modifier = Modifier.weight(1f)) }
                        repeat(3 - group.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                if (isRunOut) {
                    if (nonStriker != null) {
                        SectionLabel("Who is out")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OptionChip(striker.name, selected = !nonStrikerOut, onClick = { nonStrikerOut = false }, modifier = Modifier.weight(1f))
                            OptionChip(nonStriker.name, selected = nonStrikerOut, onClick = { nonStrikerOut = true }, modifier = Modifier.weight(1f))
                        }
                    }
                    SectionLabel("Runs completed")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (0..3).forEach { r -> OptionChip("$r", selected = r == runs, onClick = { runs = r }, modifier = Modifier.weight(1f)) }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(kind, isRunOut && nonStrikerOut, if (isRunOut) runs else 0) },
                shape   = ShapeButton,
                colors  = ButtonDefaults.buttonColors(containerColor = Red, contentColor = if (IsDarkTheme) Color(0xFF2A0A0A) else Color.White),
            ) { Text("Confirm out") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Ink300) } },
    )
}

/** A small outlined button for the extras row under the run pad. */
@Composable
private fun ExtraButton(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current   // same light tick the run tiles give
    OutlinedButton(
        onClick  = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() },
        enabled  = enabled,
        modifier = modifier.height(52.dp),
        shape    = ShapeButton,
        border   = BorderStroke(1.5.dp, if (enabled) Ink300 else Ink200),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
    ) { Text(text, color = if (enabled) Ink else Ink300, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
}
