package com.waleedahmedja.score247.util

// ─────────────────────────────────────────────────────────────────────────────
// LegalDocs.kt: the list of documents shown under "About and legal" inside the app.
//
// Each document is an ordinary Markdown file kept in app/src/main/assets/docs/. The same text
// also lives at the top level of the GitHub repository (PRIVACY.md, TERMS.md ...). The two sets
// must stay identical: run scripts/sync-docs.sh (or sync-docs.ps1 on Windows) after editing
// one of the top-level files.
// ─────────────────────────────────────────────────────────────────────────────

data class LegalDoc(
    /** Short name used in the screen's route, e.g. "privacy". */
    val id: String,
    val title: String,
    /** One line shown under the title in the list. */
    val blurb: String,
    /** File name inside assets/docs/. */
    val file: String,
)

object LegalDocs {
    val all: List<LegalDoc> = listOf(
        LegalDoc("privacy",      "Privacy Policy",   "What the app stores and what it never collects", "privacy.md"),
        LegalDoc("terms",        "Terms of Use",     "The simple rules for using Score247",           "terms.md"),
        LegalDoc("license",      "Open-source licence", "MIT: free to use, change and share",         "license.md"),
        LegalDoc("changelog",    "What's new",       "Every change, version by version",               "changelog.md"),
        LegalDoc("contributing", "Contributing",     "How to report bugs and help build the app",      "contributing.md"),
        LegalDoc("conduct",      "Code of Conduct",  "How we treat each other in the project",         "code_of_conduct.md"),
        LegalDoc("security",     "Security",         "How to report a vulnerability",                  "security.md"),
    )

    fun byId(id: String): LegalDoc? = all.firstOrNull { it.id == id }
}
