package com.waleedahmedja.score247.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownParserTest {

    @Test fun headings_bullets_numbers_and_rules() {
        val md = """
            # Title
            Some text
            on two lines.

            - one
            - two
            1. first
            2. second
            ---
        """.trimIndent()
        assertEquals(
            listOf(
                MdBlock.Heading(1, "Title"),
                MdBlock.Paragraph("Some text on two lines."),
                MdBlock.Bullet("one"),
                MdBlock.Bullet("two"),
                MdBlock.Numbered(1, "first"),
                MdBlock.Numbered(2, "second"),
                MdBlock.Rule,
            ),
            MarkdownParser.parse(md),
        )
    }

    @Test fun codeBlock_keepsLinesAsTheyAre() {
        val md = "Before\n```\nline 1\n  line 2\n```\nAfter"
        assertEquals(
            listOf(MdBlock.Paragraph("Before"), MdBlock.Code("line 1\n  line 2"), MdBlock.Paragraph("After")),
            MarkdownParser.parse(md),
        )
    }

    @Test fun inlineStyles_areSplitOut() {
        assertEquals(
            listOf(
                MdSpan.Plain("a "), MdSpan.Bold("bold"), MdSpan.Plain(" and "),
                MdSpan.Code("code"), MdSpan.Plain(" and "), MdSpan.Link("link", "http://x.y"),
                MdSpan.Plain(" and "), MdSpan.Italic("italic"),
            ),
            MarkdownParser.spans("a **bold** and `code` and [link](http://x.y) and *italic*"),
        )
    }

    @Test fun plainText_isOneSpan() {
        assertEquals(listOf(MdSpan.Plain("hello")), MarkdownParser.spans("hello"))
    }
}
