package com.waleedahmedja.score247.util

// ─────────────────────────────────────────────────────────────────────────────
// MarkdownParser.kt: turns simple Markdown text into pieces the screen can draw.
//
// The legal pages (Privacy, Terms, Licence...) are written as ordinary .md files so they look
// right on GitHub AND can be shown inside the app. This file understands only the small part
// of Markdown those files use: headings (#), bullet lists (-), numbered lists (1.), code blocks
// (```), horizontal rules (---), and inside a line: **bold**, *italic*, `code` and [links](url).
//
// It is plain Kotlin with no Android code, so it is unit-tested. DocumentScreen.kt does the
// drawing.
// ─────────────────────────────────────────────────────────────────────────────

/** One block of a document: a heading, a paragraph, a list item, a code box or a divider. */
sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
    data class Bullet(val text: String) : MdBlock()
    data class Numbered(val number: Int, val text: String) : MdBlock()
    data class Code(val text: String) : MdBlock()
    data object Rule : MdBlock()
}

/** A styled piece of one line of text. */
sealed class MdSpan {
    data class Plain(val text: String) : MdSpan()
    data class Bold(val text: String) : MdSpan()
    data class Italic(val text: String) : MdSpan()
    data class Code(val text: String) : MdSpan()
    data class Link(val label: String, val url: String) : MdSpan()
}

object MarkdownParser {

    private val numbered = Regex("""^(\d+)\.\s+(.*)$""")

    /** Splits a whole document into blocks. Blank lines separate paragraphs. */
    fun parse(markdown: String): List<MdBlock> {
        val blocks = mutableListOf<MdBlock>()
        val paragraph = mutableListOf<String>()
        var inCode = false
        val code = mutableListOf<String>()

        fun flushParagraph() {
            if (paragraph.isNotEmpty()) {
                blocks += MdBlock.Paragraph(paragraph.joinToString(" "))
                paragraph.clear()
            }
        }

        for (raw in markdown.lines()) {
            val line = raw.trimEnd()

            // Fenced code blocks: everything between two ``` lines is shown as-is.
            if (line.trimStart().startsWith("```")) {
                if (inCode) { blocks += MdBlock.Code(code.joinToString("\n")); code.clear() } else flushParagraph()
                inCode = !inCode
                continue
            }
            if (inCode) { code += raw; continue }

            val trimmed = line.trimStart()
            when {
                trimmed.isEmpty() -> flushParagraph()
                trimmed.startsWith("#") -> {
                    flushParagraph()
                    val level = trimmed.takeWhile { it == '#' }.length.coerceAtMost(6)
                    blocks += MdBlock.Heading(level, trimmed.drop(level).trim())
                }
                trimmed == "---" || trimmed == "***" -> { flushParagraph(); blocks += MdBlock.Rule }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    flushParagraph(); blocks += MdBlock.Bullet(trimmed.drop(2).trim())
                }
                numbered.matches(trimmed) -> {
                    flushParagraph()
                    val m = numbered.find(trimmed)!!
                    blocks += MdBlock.Numbered(m.groupValues[1].toInt(), m.groupValues[2])
                }
                else -> paragraph += trimmed
            }
        }
        flushParagraph()
        if (inCode && code.isNotEmpty()) blocks += MdBlock.Code(code.joinToString("\n"))
        return blocks
    }

    // Matches, in order of priority: **bold**, `code`, [label](url), *italic*.
    private val inline = Regex("""\*\*(.+?)\*\*|`(.+?)`|\[(.+?)]\((.+?)\)|\*(.+?)\*""")

    /** Splits one line into plain and styled pieces. */
    fun spans(text: String): List<MdSpan> {
        val out = mutableListOf<MdSpan>()
        var last = 0
        for (m in inline.findAll(text)) {
            if (m.range.first > last) out += MdSpan.Plain(text.substring(last, m.range.first))
            val g = m.groups
            out += when {
                g[1] != null -> MdSpan.Bold(g[1]!!.value)
                g[2] != null -> MdSpan.Code(g[2]!!.value)
                g[3] != null -> MdSpan.Link(g[3]!!.value, g[4]!!.value)
                else         -> MdSpan.Italic(g[5]!!.value)
            }
            last = m.range.last + 1
        }
        if (last < text.length) out += MdSpan.Plain(text.substring(last))
        return out
    }
}
