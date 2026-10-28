package com.waleedahmedja.score247.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.waleedahmedja.score247.data.model.MatchState
import com.waleedahmedja.score247.ui.theme.Ink
import com.waleedahmedja.score247.ui.theme.Ink300
import com.waleedahmedja.score247.ui.theme.OnPitch
import com.waleedahmedja.score247.ui.theme.Paper
import com.waleedahmedja.score247.ui.theme.Pitch
import com.waleedahmedja.score247.ui.theme.ShapeButton
import com.waleedahmedja.score247.ui.theme.ShapeDialog
import com.waleedahmedja.score247.util.ShareHelper
import com.waleedahmedja.score247.util.ShareSummaryBuilder
import com.waleedahmedja.score247.util.formatMatchDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ─────────────────────────────────────────────────────────────────────────────
// ShareActions.kt: the "Share as image" and "Share as text" buttons.
//
// Tapping "Share as image" paints the story-sized picture in the background, shows a preview
// so the user can check it, and only then opens the share sheet. Painting a big bitmap takes a
// moment, so it runs off the main thread and the button shows "Creating picture…" meanwhile.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ShareScorecardActions(state: MatchState, dateMillis: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var busy by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<Pair<Bitmap, File>?>(null) }

    Column {
        S247Button(
            text = if (busy) "Creating picture…" else "Share as image",
            enabled = !busy,
            onClick = {
                busy = true
                scope.launch {
                    try {
                        preview = withContext(Dispatchers.Default) {
                            ShareHelper.createImageFile(context, state, formatMatchDate(dateMillis))
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Couldn't create the picture", Toast.LENGTH_SHORT).show()
                    } finally {
                        busy = false
                    }
                }
            },
        )
        Spacer(Modifier.height(8.dp))
        S247ButtonSecondary(text = "Share as text", onClick = { ShareHelper.shareText(context, state) })
    }

    preview?.let { (bitmap, file) ->
        AlertDialog(
            onDismissRequest = { preview = null },
            containerColor = Paper,
            shape = ShapeDialog,
            title = { Text("Preview", style = MaterialTheme.typography.headlineMedium, color = Ink) },
            text = {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Scorecard picture that will be shared",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        ShareHelper.shareImage(context, file, ShareSummaryBuilder.caption(state))
                        preview = null
                    },
                    shape = ShapeButton,
                    colors = ButtonDefaults.buttonColors(containerColor = Pitch, contentColor = OnPitch),
                ) { Text("Share") }
            },
            dismissButton = { TextButton(onClick = { preview = null }) { Text("Cancel", color = Ink300) } },
        )
    }
}
