package com.waleedahmedja.score247.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.FileProvider
import com.waleedahmedja.score247.R
import com.waleedahmedja.score247.data.model.MatchState
import java.io.File

// ─────────────────────────────────────────────────────────────────────────────
// ShareHelper.kt: saves the scorecard picture and opens Android's share sheet.
//
// Android apps cannot hand another app (WhatsApp, Instagram...) a file directly. Instead the
// picture is saved in this app's private cache folder and shared through a "FileProvider",
// which gives the other app a temporary, read-only permission for just that one file.
// The FileProvider itself is declared in AndroidManifest.xml (see docs/SETUP_SHARING.md).
// ─────────────────────────────────────────────────────────────────────────────
object ShareHelper {

    private const val FOLDER = "shared"

    /** Paints the scorecard picture and writes it to a PNG file. Call off the main thread. */
    fun createImageFile(context: Context, state: MatchState, dateText: String): Pair<Bitmap, File> {
        // The same icon picture the app shows on its home screen.
        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.app_logo)
        val bitmap = ScorecardImage.render(state, dateText, logo)
        val dir = File(context.cacheDir, FOLDER).apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }            // keep only the newest picture
        val file = File(dir, "score247_scorecard_${System.currentTimeMillis()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bitmap to file
    }

    /**
     * Opens the share sheet with the picture. The caption (with the GitHub link) travels as
     * the message text: WhatsApp shows it under the picture. Instagram ignores captions on
     * stories, which is why the picture also carries a QR code.
     */
    fun shareImage(context: Context, file: File, caption: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            clipData = ClipData.newRawUri("Scorecard", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share scorecard"))
    }

    /** Plain-text share (the original behaviour), now with the GitHub link at the end. */
    fun shareText(context: Context, state: MatchState) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareScorecard(state) + "\n" + AppLinks.REPO_URL)
        }
        context.startActivity(Intent.createChooser(send, "Share scorecard"))
    }
}
