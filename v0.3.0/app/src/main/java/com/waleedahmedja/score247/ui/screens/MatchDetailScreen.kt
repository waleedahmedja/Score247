package com.waleedahmedja.score247.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waleedahmedja.score247.ui.components.ShareScorecardActions
import com.waleedahmedja.score247.ui.components.S247Screen
import com.waleedahmedja.score247.ui.theme.Ink
import com.waleedahmedja.score247.ui.theme.Ink300
import com.waleedahmedja.score247.ui.theme.Ink500
import com.waleedahmedja.score247.ui.theme.Paper
import com.waleedahmedja.score247.ui.theme.Red
import com.waleedahmedja.score247.ui.theme.ShapeButton
import com.waleedahmedja.score247.ui.theme.ShapeDialog
import com.waleedahmedja.score247.util.formatMatchDate
import com.waleedahmedja.score247.viewmodel.MatchViewModel

/** One saved match: its full scorecard (read-only), with Share and Delete. */
@Composable
fun MatchDetailScreen(matchId: Long, vm: MatchViewModel, onBack: () -> Unit) {
    val history by vm.history.collectAsStateWithLifecycle()
    val record = history.firstOrNull { it.id == matchId }
    var confirmDelete by remember { mutableStateOf(false) }

    // The match is gone (just deleted) → leave this screen instead of showing an empty page.
    LaunchedEffect(record == null) { if (record == null) onBack() }
    if (record == null) return

    S247Screen(formatMatchDate(record.completedAt), onBack = onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            ScorecardBody(record.state)
            Spacer(Modifier.height(4.dp))
            ShareScorecardActions(record.state, record.completedAt)
            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Delete this match", color = Red, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor   = Paper,
            shape            = ShapeDialog,
            title = { Text("Delete this match?", style = MaterialTheme.typography.headlineMedium, color = Ink) },
            text  = { Text("It will also be removed from player stats.", style = MaterialTheme.typography.bodyMedium, color = Ink500) },
            confirmButton = {
                Button(
                    onClick = { confirmDelete = false; vm.deleteMatch(record.id) },
                    colors  = ButtonDefaults.buttonColors(containerColor = Red),
                    shape   = ShapeButton,
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = Ink300) } },
        )
    }
}
