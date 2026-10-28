package com.waleedahmedja.score247.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.waleedahmedja.score247.data.model.BallEvent
import com.waleedahmedja.score247.data.model.MatchRules
import com.waleedahmedja.score247.ui.theme.Amber
import com.waleedahmedja.score247.ui.theme.AmberTint
import com.waleedahmedja.score247.ui.theme.Ink
import com.waleedahmedja.score247.ui.theme.Ink100
import com.waleedahmedja.score247.ui.theme.Ink200
import com.waleedahmedja.score247.ui.theme.Ink500
import com.waleedahmedja.score247.ui.theme.Paper
import com.waleedahmedja.score247.ui.theme.Pitch
import com.waleedahmedja.score247.ui.theme.PitchL
import com.waleedahmedja.score247.ui.theme.PitchTint
import com.waleedahmedja.score247.ui.theme.Red
import com.waleedahmedja.score247.ui.theme.RedTint
import com.waleedahmedja.score247.ui.theme.ShapeCard

/** Colours and text for one banner; built from the palette inside composition. */
private class BannerStyle(val bg: Color, val border: Color, val fg: Color, val message: String)

@Composable
private fun bannerStyle(event: BallEvent, rules: MatchRules): BannerStyle = when (event) {
    is BallEvent.Runs -> when (event.runs) {
        6    -> BannerStyle(PitchTint, PitchL, Pitch, "SIX!  Maximum")
        4    -> BannerStyle(PitchTint, PitchL, Pitch, "FOUR!  Boundary")
        0    -> BannerStyle(Ink100, Ink200, Ink500, "Dot ball")
        else -> BannerStyle(Paper, Ink200, Ink, "${event.runs} run${if (event.runs == 1) "" else "s"}")
    }
    is BallEvent.Wicket -> BannerStyle(
        RedTint, Red.copy(alpha = 0.4f), Red,
        "WICKET  •  ${event.kind.label}" + if (event.runsOnBall > 0) "  (+${event.runsOnBall})" else "",
    )
    is BallEvent.Wide -> BannerStyle(
        AmberTint, Amber.copy(alpha = 0.4f), Amber,
        "Wide  •  +${(if (rules.wideGivesRun) 1 else 0) + event.extras}",
    )
    is BallEvent.NoBall -> BannerStyle(
        AmberTint, Amber.copy(alpha = 0.4f), Amber,
        "No ball  •  +${(if (rules.noBallGivesRun) 1 else 0) + event.extras}" +
                if (rules.freeHitOnNoBall) "  •  free hit next" else "",
    )
    is BallEvent.Bye    -> BannerStyle(Ink100, Ink200, Ink500, "${event.runs} bye${if (event.runs == 1) "" else "s"}")
    is BallEvent.LegBye -> BannerStyle(Ink100, Ink200, Ink500, "${event.runs} leg bye${if (event.runs == 1) "" else "s"}")
}

/**
 * Slides in a coloured strip describing the delivery that was just scored.
 * Keyed on [ballCount] as well as the event, so two identical balls in a row both animate.
 */
@Composable
fun LastBallBanner(event: BallEvent?, ballCount: Int, rules: MatchRules, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState    = ballCount to event,
        modifier       = modifier,
        transitionSpec = {
            (slideInVertically { -it / 2 } + fadeIn(tween(180))) togetherWith
                    (slideOutVertically { it / 3 } + fadeOut(tween(100)))
        },
        label = "last_ball_banner",
    ) { (_, ev) ->
        if (ev == null) {
            Spacer(Modifier.height(0.dp))
        } else {
            val style = bannerStyle(ev, rules)
            Surface(
                shape    = ShapeCard,
                color    = style.bg,
                border   = BorderStroke(1.dp, style.border),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(style.message, style = MaterialTheme.typography.labelLarge, color = style.fg)
                }
            }
        }
    }
}
