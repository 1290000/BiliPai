package com.android.purebilibili.feature.dynamic.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.getLinkAnnotations
import com.android.purebilibili.data.model.response.DynamicDesc
import com.android.purebilibili.data.model.response.RichTextNode
import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicMultipleMentionPolicyTest {
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
}
