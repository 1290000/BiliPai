package com.android.purebilibili.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MessageLinkNavigationPolicyTest {

    @Test
    fun resolveMessageLinkNavigationAction_routesAidDeepLinkWithCommentRootToVideoComment() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://video/115391124741470?page=0&comment_root_id=279569905408"
        )

        val videoAction = assertIs<MessageLinkNavigationAction.VideoComment>(action)
        assertEquals("av115391124741470", videoAction.videoId)
        assertEquals(279569905408L, videoAction.rootReplyId)
        assertEquals(0L, videoAction.targetReplyId)
    }

    @Test
    fun resolveMessageLinkNavigationAction_routesWebVideoFragmentReplyToVideoComment() {
        val action = resolveMessageLinkNavigationAction(
            "https://www.bilibili.com/video/BV1xx411c7mD?comment_root_id=1#reply2"
        )

        val videoAction = assertIs<MessageLinkNavigationAction.VideoComment>(action)
        assertEquals("BV1xx411c7mD", videoAction.videoId)
        assertEquals(1L, videoAction.rootReplyId)
        assertEquals(2L, videoAction.targetReplyId)
    }

    @Test
    fun resolveMessageLinkNavigationAction_routesVideoDeepLinkWithoutCommentToVideo() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://video/115391124741470?page=0"
        )

        val videoAction = assertIs<MessageLinkNavigationAction.Video>(action)
        assertEquals("av115391124741470", videoAction.videoId)
    }

    @Test
    fun resolveMessageLinkNavigationAction_routesLikelyDynamicCommentFallbackToDynamic() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://comment/detail/1/1199344045210468386/265141324256"
        )

        val dynamicAction = assertIs<MessageLinkNavigationAction.DynamicComment>(action)
        assertEquals("1199344045210468386", dynamicAction.dynamicId)
        assertEquals(265141324256L, dynamicAction.rootReplyId)
        assertEquals(0L, dynamicAction.targetReplyId)
    }

    @Test
    fun resolveMessageLinkNavigationAction_decodesEncodedEnterUriCompatibly() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://comment/detail/1/1199344045210468386/265141324256" +
                "?enterUri=bilibili%3A%2F%2Ffollowing%2Fdetail%2F1199344045210468386" +
                "&comment_id=265141324999"
        )

        val dynamicAction = assertIs<MessageLinkNavigationAction.DynamicComment>(action)
        assertEquals("1199344045210468386", dynamicAction.dynamicId)
        assertEquals(265141324256L, dynamicAction.rootReplyId)
        assertEquals(265141324999L, dynamicAction.targetReplyId)
    }

    @Test
    fun resolveMessageLinkNavigationAction_routesOpusCommentLinkToDynamicComment() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://opus/detail/1073543151725051921?comment_root_id=265141324256&comment_on=1"
        )

        val dynamicAction = assertIs<MessageLinkNavigationAction.DynamicComment>(action)
        assertEquals("1073543151725051921", dynamicAction.dynamicId)
        assertEquals(265141324256L, dynamicAction.rootReplyId)
        assertEquals(0L, dynamicAction.targetReplyId)
    }

    @Test
    fun resolveMessageLinkNavigationAction_routesArticleCommentBusinessToArticle() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://comment/detail/12/34646640/265141324256"
        )

        val articleAction = assertIs<MessageLinkNavigationAction.Article>(action)
        assertEquals(34646640L, articleAction.articleId)
    }

    @Test
    fun resolveMessageLinkNavigationAction_routesAnchorParameterToTargetReplyId() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://comment/detail/17/832703053858603029/238686570016/?anchor=238686628816"
        )

        val dynamicAction = assertIs<MessageLinkNavigationAction.DynamicComment>(action)
        assertEquals("832703053858603029", dynamicAction.dynamicId)
        assertEquals(238686570016L, dynamicAction.rootReplyId)
        assertEquals(238686628816L, dynamicAction.targetReplyId)
    }

    @Test
    fun buildMessageFeedCommentNavigationLink_handlesQueryOnlyNativeUri() {
        val link = com.android.purebilibili.feature.message.feed.buildMessageFeedCommentNavigationLink(
            nativeUri = "?comment_id=238686628816",
            uri = "https://www.bilibili.com/video/BV1xx411c7mD",
            businessId = 1,
            subjectId = 115391124741470L,
            rootId = 238686570016L,
            sourceId = 238686628816L,
            targetId = 238686628816L,
        )

        assertEquals(
            "bilibili://comment/detail/1/115391124741470/238686570016?comment_id=238686628816&enterUri=https%3A%2F%2Fwww.bilibili.com%2Fvideo%2FBV1xx411c7mD",
            link
        )
    }

    @Test
    fun resolveMessageLinkNavigationAction_routesMsgFoldVideoComment() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://comment/msg_fold/1/22222/33333/11111/?enterUri=bilibili%3A%2F%2Fvideo%2F22222"
        )

        val videoAction = assertIs<MessageLinkNavigationAction.VideoComment>(action)
        assertEquals("av22222", videoAction.videoId)
        assertEquals(33333L, videoAction.rootReplyId)
        assertEquals(11111L, videoAction.targetReplyId)
    }

    @Test
    fun resolveMessageLinkNavigationAction_routesH5CommentSubToVideoComment() {
        val action = resolveMessageLinkNavigationAction(
            "https://www.bilibili.com/h5/comment/sub?oid=12345&pageType=1&root=67890&comment_secondary_id=11111"
        )

        val videoAction = assertIs<MessageLinkNavigationAction.VideoComment>(action)
        assertEquals("av12345", videoAction.videoId)
        assertEquals(67890L, videoAction.rootReplyId)
        assertEquals(11111L, videoAction.targetReplyId)
    }

    @Test
    fun resolveMessageLinkNavigationAction_routesH5CommentSubToDynamicComment() {
        val action = resolveMessageLinkNavigationAction(
            "https://www.bilibili.com/h5/comment/sub?oid=832703053858603029&pageType=17&root=238686570016&comment_secondary_id=238686628816"
        )

        val dynamicAction = assertIs<MessageLinkNavigationAction.DynamicComment>(action)
        assertEquals("832703053858603029", dynamicAction.dynamicId)
        assertEquals(238686570016L, dynamicAction.rootReplyId)
        assertEquals(238686628816L, dynamicAction.targetReplyId)
    }

    @Test
    fun resolveMessageLinkNavigationAction_resolvesBrowserUrlRecursively() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://browser/?url=https%3A%2F%2Fwww.bilibili.com%2Fvideo%2FBV1xx411c7mD%3Fcomment_root_id%3D123%26comment_secondary_id%3D456"
        )

        val videoAction = assertIs<MessageLinkNavigationAction.VideoComment>(action)
        assertEquals("BV1xx411c7mD", videoAction.videoId)
        assertEquals(123L, videoAction.rootReplyId)
        assertEquals(456L, videoAction.targetReplyId)
    }

    @Test
    fun resolveMessageLinkNavigationAction_routesArticleSchemeToArticle() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://article/40679479?jump_opus=1"
        )

        val articleAction = assertIs<MessageLinkNavigationAction.Article>(action)
        assertEquals(40679479L, articleAction.articleId)
    }

    @Test
    fun resolveMessageLinkNavigationAction_doesNotRouteCustomSchemeToWeb() {
        val action = resolveMessageLinkNavigationAction("bilibili://unknown_feature/xyz")
        val webAction = assertIs<MessageLinkNavigationAction.Web>(action)
        assertEquals("", webAction.url)
    }

    @Test
    fun buildMessageFeedCommentNavigationLink_buildsCommentLinkWhenNativeUriIsPlainVideoUrl() {
        val link = com.android.purebilibili.feature.message.feed.buildMessageFeedCommentNavigationLink(
            nativeUri = "https://www.bilibili.com/video/BV1xx411c7mD",
            uri = "https://www.bilibili.com/video/BV1xx411c7mD",
            businessId = 1,
            subjectId = 115391124741470L,
            rootId = 238686570016L,
            sourceId = 238686628816L,
            targetId = 238686628816L,
            business = "视频"
        )

        assertEquals(
            "bilibili://comment/detail/1/115391124741470/238686570016?comment_id=238686628816&enterUri=https%3A%2F%2Fwww.bilibili.com%2Fvideo%2FBV1xx411c7mD",
            link
        )
    }

    @Test
    fun buildMessageFeedCommentNavigationLink_infersBusinessIdFromBusinessText() {
        val link = com.android.purebilibili.feature.message.feed.buildMessageFeedCommentNavigationLink(
            nativeUri = "",
            uri = "https://t.bilibili.com/832703053858603029",
            businessId = 0,
            subjectId = 832703053858603029L,
            rootId = 238686570016L,
            sourceId = 238686628816L,
            targetId = 238686628816L,
            business = "动态"
        )

        assertEquals(
            "bilibili://comment/detail/17/832703053858603029/238686570016?comment_id=238686628816&enterUri=https%3A%2F%2Ft.bilibili.com%2F832703053858603029",
            link
        )
    }
}
