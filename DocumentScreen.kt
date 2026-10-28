package com.waleedahmedja.score247.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.waleedahmedja.score247.ui.components.S247Divider
import com.waleedahmedja.score247.ui.components.S247Screen
import com.waleedahmedja.score247.ui.theme.Ink
import com.waleedahmedja.score247.ui.theme.Ink100
import com.waleedahmedja.score247.ui.theme.Ink500
import com.waleedahmedja.score247.ui.theme.Pitch
import com.waleedahmedja.score247.util.LegalDocs
import com.waleedahmedja.score247.util.MarkdownParser
import com.waleedahmedja.score247.util.MdBlock
import com.waleedahmedja.score247.util.MdSpan

// ─────────────────────────────────────────────────────────────────────────────
// DocumentScreen.kt: reads one Markdown document from the app's assets and shows it.
//
// The text is loaded once, split into blocks by MarkdownParser, and each block is drawn as a
// heading, paragraph, list item, code box or divider. Links (including bare https:// addresses)
// are underlined and open in the browser when tapped.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DocumentScreen(docId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val doc = LegalDocs.byId(docId)

    // Read the file once. If it is missing, show a short message instead of crashing.
    val blocks = remember(docId) {
        val text = try {
            context.assets.open("docs/${doc?.file}").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "Sorry, this page could not be loaded."
        }
        MarkdownParser.parse(text)
    }

    S247Screen(doc?.title ?: "Document", onBack = onBack) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(blocks) { block -> BlockView(block) { url -> openUrl(context, url) } }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    // Only ever open web addresses (never file:, intent: or other schemes).
    val uri = Uri.parse(url)
    if (uri.scheme != "https" && uri.scheme != "http") return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: Exception) {
        // No browser installed: nothing sensible to do.
    }
}

@Composable
private fun BlockView(block: MdBlock, onLink: (String) -> Unit) {
    when (block) {
        is MdBlock.Heading -> {
            val style = when (block.level) {
                1    -> MaterialTheme.typography.headlineLarge
                2    -> MaterialTheme.typography.headlineMedium
                else -> MaterialTheme.typography.titleLarge
            }
            if (block.level >= 2) Spacer(Modifier.height(6.dp))
            RichText(block.text, style.copy(color = Ink), onLink)
        }
        is MdBlock.Paragraph -> RichText(block.text, MaterialTheme.typography.bodyMedium.copy(color = Ink500), onLink)
        is MdBlock.Bullet -> Row {
            Text("•", style = MaterialTheme.typography.bodyMedium, color = Pitch, modifier = Modifier.width(20.dp))
            RichText(block.text, MaterialTheme.typography.bodyMedium.copy(color = Ink500), onLink, Modifier.weight(1f))
        }
        is MdBlock.Numbered -> Row {
            Text("${block.number}.", style = MaterialTheme.typography.bodyMedium, color = Pitch, modifier = Modifier.width(28.dp))
            RichText(block.text, MaterialTheme.typography.bodyMedium.copy(color = Ink500), onLink, Modifier.weight(1f))
        }
        is MdBlock.Code -> Box(
            Modifier.fillMaxWidth().background(Ink100, RoundedCornerShape(12.dp)).horizontalScroll(rememberScrollState()).padding(14.dp),
        ) {
            Text(block.text, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = Ink)
        }
        MdBlock.Rule -> S247Divider(Modifier.padding(vertical = 6.dp))
    }
}

/** Text with **bold**, `code` and links. Tapping a link calls [onLink] with its address. */
@Composable
private fun RichText(text: String, style: TextStyle, onLink: (String) -> Unit, modifier: Modifier = Modifier) {
    val linkColor = Pitch
    val codeBackground = Ink100
    val annotated = remember(text, linkColor, codeBackground) {
        buildAnnotatedString {
            for (span in MarkdownParser.spans(text)) {
                when (span) {
                    is MdSpan.Plain  -> appendWithBareLinks(span.text, linkColor)
                    is MdSpan.Bold   -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(span.text) }
                    is MdSpan.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(span.text) }
                    is MdSpan.Code   -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)) { append(span.text) }
                    is MdSpan.Link   -> appendLink(span.label, span.url, linkColor)
                }
            }
        }
    }
    ClickableText(
        text = annotated,
        style = style,
        modifier = modifier,
        onClick = { offset ->
            annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { onLink(it.item) }
        },
    )
}

private fun AnnotatedString.Builder.appendLink(label: String, url: String, color: androidx.compose.ui.graphics.Color) {
    pushStringAnnotation("URL", url)
    withStyle(SpanStyle(color = color, textDecoration = TextDecoration.Underline)) { append(label) }
    pop()
}

/** Appends [text], turning any bare http(s):// address in it into a tappable link. */
private fun AnnotatedString.Builder.appendWithBareLinks(text: String, color: androidx.compose.ui.graphics.Color) {
    val url = Regex("""https?://[^\s)]+""")
    var last = 0
    for (m in url.findAll(text)) {
        append(text.substring(last, m.range.first))
        val trimmed = m.value.trimEnd('.', ',', ';')
        appendLink(trimmed, trimmed, color)
        last = m.range.first + trimmed.length
    }
    append(text.substring(last))
}
