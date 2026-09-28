package com.android.purebilibili.feature.dynamic.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.getLinkAnnotations
import com.android.purebilibili.data.model.response.DynamicDesc
import com.android.purebilibili.data.model.response.RichTextNode
import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicMultipleMentionPolicyTest {
    private fun AnnotatedString.highlightedText(): List<String> = spanStyles
        .filter { it.item.color == Color.Blue }
        .map { text.substring(it.start, it.end) }

    @Test
    fun detailBodyHighlightsEveryMentionWhenMetadataIsOutOfTextOrder() {
        val text = "感谢@甲 和@乙 的支持"
        val desc = DynamicDesc(
            text = text,
            rich_text_nodes = listOf(
                RichTextNode(type = "AT", text = "@乙", rid = "2"),
                RichTextNode(type = "AT", text = "@甲", rid = "1"),
            ),
        )

        val annotated = buildDynamicRichTextAnnotatedString(
            desc = desc,
            primaryColor = Color.Blue,
            textColor = Color.Black,
        )
        val mentions = annotated.getLinkAnnotations(0, annotated.length)
            .mapNotNull { annotation ->
                val tag = (annotation.item as? LinkAnnotation.Clickable)?.tag ?: return@mapNotNull null
                if (!tag.startsWith(DYNAMIC_RICH_TEXT_LINK_USER_PREFIX)) return@mapNotNull null
                annotated.text.substring(annotation.start, annotation.end) to tag
            }

        assertEquals(text, annotated.text)
        assertEquals(
            listOf("@甲" to "USER:1", "@乙" to "USER:2"),
            mentions,
        )
    }

    @Test
    fun detailBodyHighlightsMentionsAndTopicWithoutAnyRichTextNodes() {
        val body = resolveDynamicOpusTextBlockRichDesc(
            blockText = "谢谢@椒椒椒、@艾香亦心动 #中秋快乐#",
            preferredDesc = null,
        ) ?: error("full paragraph must remain rich-text renderable")

        val annotated = buildDynamicRichTextAnnotatedString(body, Color.Blue, Color.Black)

        assertEquals(body.text, annotated.text)
        assertEquals(
            listOf("@椒椒椒", "@艾香亦心动", "#中秋快乐#"),
            annotated.highlightedText(),
        )
    }

    @Test
    fun textOnlyDetailNodesStillHighlightEveryMention() {
        val text = "谢谢@甲 和@乙"
        val body = resolveDynamicOpusTextBlockRichDesc(
            blockText = text,
            preferredDesc = null,
            blockRichTextNodes = listOf(RichTextNode(type = "TEXT", text = text)),
        ) ?: error("expected full body")

        val annotated = buildDynamicRichTextAnnotatedString(body, Color.Blue, Color.Black)

        assertEquals(text, annotated.text)
        assertEquals(listOf("@甲", "@乙"), annotated.highlightedText())
    }

    @Test
    fun plainTextNodesHighlightMissingMentionWithoutLosingKnownUserLink() {
        val body = resolveDynamicOpusTextBlockRichDesc(
            blockText = "谢谢@甲 和@乙",
            preferredDesc = DynamicDesc(
                rich_text_nodes = listOf(RichTextNode(type = "AT", text = "@甲", rid = "1")),
            ),
            blockRichTextNodes = listOf(RichTextNode(type = "TEXT", text = "谢谢@甲 和@乙")),
        ) ?: error("expected full body")

        val annotated = buildDynamicRichTextAnnotatedString(body, Color.Blue, Color.Black)
        val userLinks = annotated.getLinkAnnotations(0, annotated.length)
            .mapNotNull { (it.item as? LinkAnnotation.Clickable)?.tag }
            .filter { it.startsWith(DYNAMIC_RICH_TEXT_LINK_USER_PREFIX) }

        assertEquals("谢谢@甲 和@乙", annotated.text)
        assertEquals(listOf("@甲", "@乙"), annotated.highlightedText())
        assertEquals(listOf("USER:1"), userLinks)
    }

    @Test
    fun plainTextMentionFallbackIgnoresEmailAndUrlAtSigns() {
        val annotated = buildDynamicRichTextAnnotatedString(
            desc = DynamicDesc(text = "邮箱 foo@example.com，链接 https://example.com/@path，@真正用户"),
            primaryColor = Color.Blue,
            textColor = Color.Black,
        )

        assertEquals(
            listOf("@真正用户"),
            annotated.highlightedText().filter { it.startsWith("@") },
        )
    }
}
