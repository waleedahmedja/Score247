package com.waleedahmedja.score247.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.waleedahmedja.score247.data.model.MatchState

// ─────────────────────────────────────────────────────────────────────────────
// ScorecardImage.kt: paints the scorecard as a picture for sharing.
//
// The picture is 1080 x 1920 pixels, the size of an Instagram / WhatsApp story, so it fills
// a phone screen. It is drawn with Android's plain Canvas (a blank sheet you paint on with
// "draw this text here, draw that rounded box there"), NOT with Compose. That keeps it
// independent of the live screens and always the same size on every phone.
//
// What gets printed comes from ShareSummary.kt. This file only decides where and how.
// The QR code in the footer opens the project's GitHub page when scanned: that is how a
// viewer who only sees the picture (for example on an Instagram story) can find the app.
// ─────────────────────────────────────────────────────────────────────────────
object ScorecardImage {

    const val WIDTH = 1080
    const val HEIGHT = 1920
    private const val MARGIN = 72f

    // Colours of the picture (always dark, like the in-app scoreboard).
    private val BG_TOP    = Color.parseColor("#0E2A1B")
    private val BG_BOTTOM = Color.parseColor("#030A06")
    private val PITCH     = Color.parseColor("#2FBF71")
    private val AMBER     = Color.parseColor("#F5B83D")
    private val WHITE     = Color.WHITE
    private val MUTED     = Color.parseColor("#A9BBB0")
    private val SEAM      = Color.parseColor("#C0392B")

    /** Draws the picture. Call this off the main thread: it allocates a ~8 MB bitmap. */
    fun render(state: MatchState, dateText: String, logo: Bitmap? = null): Bitmap {
        val summary = ShareSummaryBuilder.from(state)
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val c = Canvas(bitmap)

        drawBackground(c)
        var y = drawHeader(c, dateText, logo)

        // Teams and result headline
        y += 40f
        c.drawText(summary.title, MARGIN, y, text(MUTED, 46f, bold = true).fitted(summary.title, WIDTH - 2 * MARGIN))
        y += 36f
        y += drawWrapped(c, summary.result, text(WHITE, 88f, bold = true), MARGIN, y, WIDTH - 2 * MARGIN, maxLines = 2)

        // One card per innings
        y += 36f
        for ((index, line) in summary.innings.withIndex()) {
            y += drawInningsCard(c, index + 1, line, y) + 24f
        }

        // Player of the Match
        if (summary.potmName != null) {
            y += 12f
            y += drawPotm(c, summary.potmName, summary.potmStats, y)
        }

        drawFooter(c)
        return bitmap
    }

    // ── Pieces ────────────────────────────────────────────────────────────────

    private fun drawBackground(c: Canvas) {
        val bg = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, HEIGHT.toFloat(), BG_TOP, BG_BOTTOM, Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bg)

        // A big, faint green circle top right, for depth.
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PITCH; alpha = 28 }
        c.drawCircle(WIDTH - 60f, 220f, 460f, glow)
    }

    /** Logo + app name on the left, date on the right. Returns the y just below the header. */
    private fun drawHeader(c: Canvas, dateText: String, logo: Bitmap?): Float {
        val cx = MARGIN + 48f
        val cy = 150f
        if (logo != null) drawLogoBitmap(c, logo, cx, cy, 48f) else drawLogo(c, cx, cy, 48f)

        c.drawText("SCORE247", cx + 48f + 28f, cy + 16f, text(WHITE, 48f, bold = true).apply { letterSpacing = 0.12f })
        val date = text(MUTED, 32f).apply { textAlign = Paint.Align.RIGHT }
        c.drawText(dateText, WIDTH - MARGIN, cy + 12f, date)
        return cy + 48f
    }

    /** Draws the real app icon (the launcher picture) as a rounded square centred on ([cx], [cy]). */
    private fun drawLogoBitmap(c: Canvas, logo: Bitmap, cx: Float, cy: Float, half: Float) {
        val box = RectF(cx - half, cy - half, cx + half, cy + half)
        c.save()
        c.clipPath(Path().apply { addRoundRect(box, half * 0.44f, half * 0.44f, Path.Direction.CW) })
        c.drawBitmap(logo, null, box, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        c.restore()
    }

    /** Fallback logo (used only if the icon picture could not be loaded): a green rounded tile with a white cricket ball and two red seams. */
    private fun drawLogo(c: Canvas, cx: Float, cy: Float, half: Float) {
        val tile = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PITCH }
        c.drawRoundRect(RectF(cx - half, cy - half, cx + half, cy + half), half * 0.56f, half * 0.56f, tile)

        val r = half * 0.56f
        val ball = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
        c.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = WHITE })
        c.save()
        c.clipPath(ball)
        val seam = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = SEAM; style = Paint.Style.STROKE; strokeWidth = r * 0.10f
        }
        c.drawCircle(cx - r * 1.30f, cy, r * 1.15f, seam)
        c.drawCircle(cx + r * 1.30f, cy, r * 1.15f, seam)
        c.restore()
    }

    /** One innings: team name, big score, overs, run rate, top batter and top bowler. */
    private fun drawInningsCard(c: Canvas, number: Int, line: InningsLine, top: Float): Float {
        val height = 330f
        val box = RectF(MARGIN, top, WIDTH - MARGIN, top + height)
        c.drawRoundRect(box, 44f, 44f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = WHITE; alpha = 20 })
        c.drawRoundRect(box, 44f, 44f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = WHITE; alpha = 40; style = Paint.Style.STROKE; strokeWidth = 2f
        })

        val inner = MARGIN + 44f
        val innerWidth = WIDTH - 2 * (MARGIN + 44f)

        val heading = "INNINGS $number  •  ${line.team.uppercase()}"
        c.drawText(heading, inner, top + 64f, text(MUTED, 32f, bold = true).fitted(heading, innerWidth))

        // Big score on the left, overs and run rate on the right
        c.drawText(line.score, inner, top + 185f, text(WHITE, 130f, bold = true))
        val right = text(MUTED, 38f).apply { textAlign = Paint.Align.RIGHT }
        c.drawText("${line.overs} overs", WIDTH - inner, top + 140f, right)
        c.drawText("RR ${line.runRate}", WIDTH - inner, top + 190f, right)

        // Best performers
        line.topBatter?.let {
            val row = "Bat  $it"
            c.drawText(row, inner, top + 258f, text(WHITE, 36f).fitted(row, innerWidth))
        }
        line.topBowler?.let {
            val row = "Bowl  $it"
            c.drawText(row, inner, top + 304f, text(WHITE, 36f).fitted(row, innerWidth))
        }
        return height
    }

    private fun drawPotm(c: Canvas, name: String, stats: String?, top: Float): Float {
        val height = if (stats != null) 220f else 170f
        val box = RectF(MARGIN, top, WIDTH - MARGIN, top + height)
        c.drawRoundRect(box, 44f, 44f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AMBER; alpha = 34 })
        c.drawRoundRect(box, 44f, 44f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AMBER; alpha = 120; style = Paint.Style.STROKE; strokeWidth = 3f
        })
        val inner = MARGIN + 44f
        val innerWidth = WIDTH - 2 * (MARGIN + 44f)
        c.drawText("PLAYER OF THE MATCH", inner, top + 58f, text(AMBER, 30f, bold = true).apply { letterSpacing = 0.1f })
        val namePaint = text(WHITE, 64f, bold = true)
        c.drawText(name, inner, top + 132f, namePaint.fitted(name, innerWidth))
        if (stats != null) c.drawText(stats, inner, top + 190f, text(MUTED, 38f))
        return height
    }

    /** Bottom strip: call to action on the left, QR code to the GitHub page on the right. */
    private fun drawFooter(c: Canvas) {
        val qrSize = 270f
        val pad = 20f
        val boxRight = WIDTH - MARGIN
        val boxLeft = boxRight - qrSize - 2 * pad
        val boxBottom = HEIGHT - MARGIN
        val boxTop = boxBottom - qrSize - 2 * pad

        c.drawRoundRect(RectF(boxLeft, boxTop, boxRight, boxBottom), 36f, 36f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = WHITE })
        drawQr(c, AppLinks.REPO_URL, boxLeft + pad, boxTop + pad, qrSize)

        val textWidth = boxLeft - MARGIN - 36f
        c.drawText("Scored with", MARGIN, boxTop + 70f, text(MUTED, 38f))
        c.drawText(AppLinks.NAME, MARGIN, boxTop + 150f, text(WHITE, 84f, bold = true))
        c.drawText("Free and open source", MARGIN, boxTop + 205f, text(PITCH, 38f, bold = true))
        val link = "github.com/waleedahmedja/Score247"
        c.drawText(link, MARGIN, boxTop + 262f, text(MUTED, 30f).fitted(link, textWidth))
        val scan = "Scan the code to get the app"
        c.drawText(scan, MARGIN, boxTop + 305f, text(MUTED, 30f).fitted(scan, textWidth))
    }

    /** Draws [content] as a QR code filling a square of [size] pixels at ([left], [top]). */
    private fun drawQr(c: Canvas, content: String, left: Float, top: Float, size: Float) {
        val matrix = QRCodeWriter().encode(
            content, BarcodeFormat.QR_CODE, 0, 0,
            mapOf(EncodeHintType.MARGIN to 0),
        )
        val cell = size / matrix.width
        val dark = Paint().apply { color = Color.parseColor("#0E2A1B"); isAntiAlias = false }
        for (x in 0 until matrix.width) for (y in 0 until matrix.height) {
            if (matrix.get(x, y)) {
                c.drawRect(left + x * cell, top + y * cell, left + (x + 1) * cell, top + (y + 1) * cell, dark)
            }
        }
    }

    // ── Small helpers ─────────────────────────────────────────────────────────

    private fun text(color: Int, size: Float, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
    }

    /** Shrinks the text size until [text] fits in [maxWidth] (never below 60% of the original). */
    private fun TextPaint.fitted(text: String, maxWidth: Float): TextPaint {
        val original = textSize
        while (measureText(text) > maxWidth && textSize > original * 0.6f) textSize -= 2f
        return this
    }

    /** Draws wrapped text starting at baseline-ish [y]; returns the height it used. */
    private fun drawWrapped(c: Canvas, text: String, paint: TextPaint, x: Float, y: Float, width: Float, maxLines: Int): Float {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        c.save()
        c.translate(x, y)
        layout.draw(c)
        c.restore()
        return layout.height.toFloat()
    }
}
