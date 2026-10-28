package com.waleedahmedja.score247.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.waleedahmedja.score247.R
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.waleedahmedja.score247.data.model.BallEvent
import com.waleedahmedja.score247.ui.theme.Amber
import com.waleedahmedja.score247.ui.theme.AmberTint
import com.waleedahmedja.score247.ui.theme.Ink
import com.waleedahmedja.score247.ui.theme.Ink100
import com.waleedahmedja.score247.ui.theme.Ink200
import com.waleedahmedja.score247.ui.theme.Ink300
import com.waleedahmedja.score247.ui.theme.Ink50
import com.waleedahmedja.score247.ui.theme.IsDarkTheme
import com.waleedahmedja.score247.ui.theme.Ink500
import com.waleedahmedja.score247.ui.theme.Ink700
import com.waleedahmedja.score247.ui.theme.OnPitch
import com.waleedahmedja.score247.ui.theme.Paper
import com.waleedahmedja.score247.ui.theme.Pitch
import com.waleedahmedja.score247.ui.theme.PitchL
import com.waleedahmedja.score247.ui.theme.PitchTint
import com.waleedahmedja.score247.ui.theme.Red
import com.waleedahmedja.score247.ui.theme.RedTint
import com.waleedahmedja.score247.ui.theme.ShapeButton
import com.waleedahmedja.score247.ui.theme.ShapeCard
import com.waleedahmedja.score247.ui.theme.ShapeChip
import com.waleedahmedja.score247.ui.theme.ShapePill
import com.waleedahmedja.score247.ui.theme.ShapeRunTile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

// ─────────────────────────────────────────────────────────────────────────────
// BUTTONS
// ─────────────────────────────────────────────────────────────────────────────

/** Primary call-to-action. Full width, 52dp tall (comfortable thumb target). */
@Composable
fun S247Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick   = onClick,
        enabled   = enabled,
        modifier  = modifier.fillMaxWidth().height(52.dp),
        shape     = ShapeButton,
        colors    = ButtonDefaults.buttonColors(
            containerColor         = Pitch,
            contentColor           = OnPitch,
            disabledContainerColor = Ink100,
            disabledContentColor   = Ink300,
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Outlined secondary action. */
@Composable
fun S247ButtonSecondary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick  = onClick,
        enabled  = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape    = ShapeButton,
        border   = BorderStroke(1.5.dp, if (enabled) Pitch else Ink200),
        colors   = ButtonDefaults.outlinedButtonColors(
            contentColor         = Pitch,
            disabledContentColor = Ink300,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Destructive action (wicket, delete). Width is up to the caller — usually `weight(1f)`. */
@Composable
fun S247ButtonDanger(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick   = onClick,
        enabled   = enabled,
        modifier  = modifier.height(52.dp),
        shape     = ShapeButton,
        colors    = ButtonDefaults.buttonColors(
            containerColor         = Red,
            // Light red button (dark theme) needs dark text to stay readable.
            contentColor           = if (IsDarkTheme) Color(0xFF2A0A0A) else Color.White,
            disabledContainerColor = Ink100,
            disabledContentColor   = Ink300,
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RUN TILE — the button the scorer taps hundreds of times per match.
// Big (64dp), springy on press, and gives a light haptic tick.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RunTile(
    runs: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val bg = when {
        !enabled  -> Ink100
        runs == 6 -> Pitch
        runs == 4 -> PitchTint
        runs == 0 -> Ink100
        else      -> Paper
    }
    val fg = when {
        !enabled  -> Ink300
        runs == 6 -> OnPitch
        runs == 4 -> Pitch
        else      -> Ink
    }
    val borderColor = when {
        !enabled  -> Ink100
        runs == 6 -> Pitch
        runs == 4 -> PitchL
        else      -> Ink200
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label         = "run_tile_scale",
    )
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .height(64.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(ShapeRunTile)
            .background(bg)
            .border(1.dp, borderColor, ShapeRunTile)
            .semantics(mergeDescendants = true) {
                contentDescription = if (runs == 0) "Dot ball" else "$runs run${if (runs == 1) "" else "s"}"
            }
            .clickable(
                enabled           = enabled,
                interactionSource = interaction,
                indication        = null,            // the press-scale is the feedback
                role              = Role.Button,
                onClick           = {
                    haptic.performHapticFeedback(
                        if (runs >= 4) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
                    )
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = if (runs == 0) "•" else "$runs",
            style = MaterialTheme.typography.headlineMedium,
            color = fg,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CONTAINERS & ROWS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun S247Card(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier       = modifier.fillMaxWidth(),
        shape          = ShapeCard,
        color          = Paper,
        border         = BorderStroke(1.dp, Ink200),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun S247Divider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = Ink200, thickness = 1.dp)
}

/** Small all-caps section caption. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style    = MaterialTheme.typography.labelSmall,
        color    = Ink300,
        modifier = modifier,
    )
}

/** A labelled switch. The WHOLE row is the touch target and is announced as one switch. */
@Composable
fun RuleRow(label: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = Ink)
            if (desc.isNotEmpty()) Text(desc, style = MaterialTheme.typography.bodySmall, color = Ink300)
        }
        Switch(
            checked         = checked,
            onCheckedChange = null,            // handled by toggleable above
            colors = SwitchDefaults.colors(
                checkedThumbColor   = OnPitch,
                checkedTrackColor   = Pitch,
                uncheckedThumbColor = Ink300,
                uncheckedTrackColor = Ink200,
                uncheckedBorderColor = Ink200,
            ),
        )
    }
}

/** − value + control with 48dp touch targets. */
@Composable
fun Stepper(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = Ink)
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepperButton(enabled = value > min, description = "Decrease $label", add = false) { onChange(value - 1) }
            Text(
                "$value",
                style     = MaterialTheme.typography.headlineMedium,
                color     = Ink,
                textAlign = TextAlign.Center,
                modifier  = Modifier.widthIn(min = 44.dp),
            )
            StepperButton(enabled = value < max, description = "Increase $label", add = true) { onChange(value + 1) }
        }
    }
}

@Composable
private fun StepperButton(enabled: Boolean, description: String, add: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(if (enabled) Ink100 else Ink50),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = if (add) Icons.Filled.Add else Icons.Filled.Remove,
                contentDescription = description,
                tint               = if (enabled) Ink else Ink300,
                modifier           = Modifier.size(20.dp),
            )
        }
    }
}

/** A selectable pill, used for dismissal types, extras types, etc. */
@Composable
fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = 44.dp)
            .clip(ShapeChip)
            .background(if (selected) PitchTint else Ink100)
            .border(1.5.dp, if (selected) Pitch else Color.Transparent, ShapeChip)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Pitch else Ink500,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BALL DOT — one delivery in the "This over" strip
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun BallDot(event: BallEvent, animate: Boolean = false) {
    val (label, bg, fg) = when (event) {
        is BallEvent.Runs -> Triple(
            if (event.runs == 0) "•" else event.runs.toString(),
            when (event.runs) { 6 -> Pitch; 4 -> PitchTint; 0 -> Ink100; else -> Paper },
            when (event.runs) { 6 -> OnPitch; 4 -> Pitch; else -> Ink },
        )
        is BallEvent.Wide    -> Triple(if (event.extras > 0) "Wd+${event.extras}" else "Wd", AmberTint, Amber)
        is BallEvent.NoBall  -> Triple(if (event.extras > 0) "Nb+${event.extras}" else "Nb", AmberTint, Amber)
        is BallEvent.Wicket  -> Triple("W", RedTint, Red)
        is BallEvent.Bye     -> Triple("B${event.runs}",  Ink100, Ink500)
        is BallEvent.LegBye  -> Triple("Lb${event.runs}", Ink100, Ink500)
    }
    val plain = event is BallEvent.Runs && event.runs in intArrayOf(1, 2, 3, 5)

    // Newest ball pops in; older ones render at full size immediately.
    val scale = remember { Animatable(if (animate) 0.3f else 1f) }
    LaunchedEffect(Unit) {
        if (animate) scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }

    Box(
        Modifier
            .size(44.dp)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (plain) Ink200 else Color.Transparent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = fg, textAlign = TextAlign.Center)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN SCAFFOLD
// ─────────────────────────────────────────────────────────────────────────────

/** Standard screen: top bar (with optional back arrow) on the app background. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun S247Screen(title: String, onBack: (() -> Unit)? = null, content: @Composable () -> Unit) {
    SystemBarIcons(statusDarkIcons = !IsDarkTheme, navDarkIcons = !IsDarkTheme)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.headlineMedium, color = Ink) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink50, titleContentColor = Ink),
            )
        },
        containerColor = Ink50,
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) { content() }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SYSTEM BAR ICONS — needed because the app draws edge-to-edge. Without this, status-bar
// icons stay dark on the dark scoreboard (unreadable) or light on a light screen.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SystemBarIcons(statusDarkIcons: Boolean, navDarkIcons: Boolean = statusDarkIcons) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(statusDarkIcons, navDarkIcons) {
        val window = view.context.findActivity()?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars     = statusDarkIcons
            controller.isAppearanceLightNavigationBars = navDarkIcons
        }
        onDispose { }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity       -> this
    is ContextWrapper -> baseContext.findActivity()
    else              -> null
}

// ─────────────────────────────────────────────────────────────────────────────
// TOAST — listens to a flow of messages and shows each for ~2 seconds
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ToastHost(messages: Flow<String>, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    var last    by remember { mutableStateOf("") }

    LaunchedEffect(messages) {
        // collectLatest: a new message replaces the current one and restarts the timer.
        messages.collectLatest { msg ->
            last = msg; visible = true
            delay(2200)
            visible = false
        }
    }

    AnimatedVisibility(
        visible  = visible,
        enter    = fadeIn() + slideInVertically { it / 2 },
        exit     = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        Surface(shape = ShapePill, color = Ink700, contentColor = Color.White, shadowElevation = 6.dp) {
            Text(
                last,
                style    = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// APP LOGO: the same picture as the launcher icon (res/drawable-nodpi/app_logo.png):
// green stumps over a scoreboard card, with an amber cricket ball.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AppLogo(size: Dp = 88.dp) {
    Image(
        painter = painterResource(R.drawable.app_logo),
        contentDescription = "Score247 logo",
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.22f)),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// EMPTY STATE: a friendly message shown when a list has nothing in it yet.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun EmptyState(emoji: String, title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = Ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Ink500, textAlign = TextAlign.Center)
    }
}
