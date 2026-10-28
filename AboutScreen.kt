package com.waleedahmedja.score247.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.waleedahmedja.score247.ui.components.AppLogo
import com.waleedahmedja.score247.ui.components.S247Button
import com.waleedahmedja.score247.ui.components.S247Card
import com.waleedahmedja.score247.ui.components.S247Divider
import com.waleedahmedja.score247.ui.components.S247Screen
import com.waleedahmedja.score247.ui.theme.Ink
import com.waleedahmedja.score247.ui.theme.Ink300
import com.waleedahmedja.score247.ui.theme.Ink500
import com.waleedahmedja.score247.ui.theme.Pitch
import com.waleedahmedja.score247.util.AppLinks
import com.waleedahmedja.score247.util.LegalDoc
import com.waleedahmedja.score247.util.LegalDocs

// ─────────────────────────────────────────────────────────────────────────────
// AboutScreen.kt: "About and legal".
//
// What the user sees: the app logo, its name and version, a button that opens the GitHub page,
// and a list of documents (Privacy Policy, Terms, Licence, ...). Tapping one opens it in
// DocumentScreen. Reached from the small "About and legal" link at the bottom of Home.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AboutScreen(onBack: () -> Unit, onOpenDoc: (LegalDoc) -> Unit) {
    val context = LocalContext.current

    // The version number comes from the app's own package information (set in build.gradle.kts).
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    S247Screen("About and legal", onBack = onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            AppLogo()
            Spacer(Modifier.height(16.dp))
            Text(AppLinks.NAME, style = MaterialTheme.typography.displaySmall, color = Ink)
            if (version.isNotEmpty()) {
                Text("Version $version", style = MaterialTheme.typography.bodySmall, color = Ink300)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Street cricket. Your rules.\nFree, open source and works offline.",
                style = MaterialTheme.typography.bodyMedium, color = Ink500, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))

            S247Button(text = "View on GitHub", onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppLinks.REPO_URL)))
                } catch (e: Exception) {
                    // No browser installed: nothing sensible to do.
                }
            })
            Spacer(Modifier.height(20.dp))

            S247Card {
                LegalDocs.all.forEachIndexed { i, doc ->
                    DocRow(doc, onClick = { onOpenDoc(doc) })
                    if (i != LegalDocs.all.lastIndex) S247Divider()
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Built by waleedahmedja", style = MaterialTheme.typography.labelSmall, color = Ink300)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DocRow(doc: LegalDoc, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(doc.title, style = MaterialTheme.typography.titleMedium, color = Ink)
            Text(doc.blurb, style = MaterialTheme.typography.bodySmall, color = Ink300)
        }
        Text("›", style = MaterialTheme.typography.headlineMedium, color = Pitch)
    }
}
