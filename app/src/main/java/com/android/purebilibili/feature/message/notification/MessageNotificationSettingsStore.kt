package com.android.purebilibili.feature.message.notification

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.messageNotificationSettingsStore by preferencesDataStore(name = "message_notification_settings")

internal enum class MessageNotificationMode { POWER_SAVING, MORE_TIMELY }

internal data class MessageNotificationSettings(
    val enabled: Boolean = false,
    val mode: MessageNotificationMode = MessageNotificationMode.POWER_SAVING,
    val residentEnabled: Boolean = false,
    val notifyPrivateMessages: Boolean = true,
    val notifyReplies: Boolean = true,
    val notifyAtMe: Boolean = true,
    val notifyLikes: Boolean = true,
    val notifySystemNotices: Boolean = true,
    val notifyDynamicUpdates: Boolean = true,
    val notifyLiveAlerts: Boolean = true,
)

internal object MessageNotificationSettingsStore {
    private val enabledKey = booleanPreferencesKey("enabled")
    private val modeKey = stringPreferencesKey("mode")
    private val residentKey = booleanPreferencesKey("resident")
    private val privateMessagesKey = booleanPreferencesKey("private_msg")
    private val repliesKey = booleanPreferencesKey("reply")
    private val atMeKey = booleanPreferencesKey("at_me")
    private val likesKey = booleanPreferencesKey("like_me")
    private val systemNoticesKey = booleanPreferencesKey("system_notice")
    private val legacyMessageCenterKey = booleanPreferencesKey("msg_center")
    private val dynamicKey = booleanPreferencesKey("dynamic")
    private val liveKey = booleanPreferencesKey("live")

    fun getSettings(context: Context): Flow<MessageNotificationSettings> =
        context.applicationContext.messageNotificationSettingsStore.data.map { prefs ->
            // Legacy migration: the former single "msg_center" switch covered all five
            // subcategories. If it was turned off before the split, propagate that opt-out
            // to the new granular keys unless the user has since set any of them explicitly.
            val legacyCenterOff = prefs[legacyMessageCenterKey] == false
            val anySubcategorySet = privateMessagesKey in prefs || repliesKey in prefs ||
                atMeKey in prefs || likesKey in prefs || systemNoticesKey in prefs
            val migratedDefault = !legacyCenterOff || anySubcategorySet
            MessageNotificationSettings(
                enabled = prefs[enabledKey] ?: false,
                mode = MessageNotificationMode.entries.firstOrNull { it.name == prefs[modeKey] }
                    ?: MessageNotificationMode.POWER_SAVING,
                residentEnabled = prefs[residentKey] ?: false,
                notifyPrivateMessages = prefs[privateMessagesKey] ?: migratedDefault,
                notifyReplies = prefs[repliesKey] ?: migratedDefault,
                notifyAtMe = prefs[atMeKey] ?: migratedDefault,
                notifyLikes = prefs[likesKey] ?: migratedDefault,
                notifySystemNotices = prefs[systemNoticesKey] ?: migratedDefault,
                notifyDynamicUpdates = prefs[dynamicKey] ?: true,
                notifyLiveAlerts = prefs[liveKey] ?: true,
            )
        }

    suspend fun setEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.messageNotificationSettingsStore.edit { it[enabledKey] = enabled }
    }

    suspend fun setMode(context: Context, mode: MessageNotificationMode) {
        context.applicationContext.messageNotificationSettingsStore.edit { it[modeKey] = mode.name }
    }

    suspend fun setResidentEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.messageNotificationSettingsStore.edit { it[residentKey] = enabled }
    }

    suspend fun setPrivateMessagesEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.messageNotificationSettingsStore.edit { it[privateMessagesKey] = enabled }
    }

    suspend fun setRepliesEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.messageNotificationSettingsStore.edit { it[repliesKey] = enabled }
    }

    suspend fun setAtMeEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.messageNotificationSettingsStore.edit { it[atMeKey] = enabled }
    }

    suspend fun setLikesEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.messageNotificationSettingsStore.edit { it[likesKey] = enabled }
    }

    suspend fun setSystemNoticesEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.messageNotificationSettingsStore.edit { it[systemNoticesKey] = enabled }
    }

    suspend fun setDynamicUpdatesEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.messageNotificationSettingsStore.edit { it[dynamicKey] = enabled }
    }

    suspend fun setLiveAlertsEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.messageNotificationSettingsStore.edit { it[liveKey] = enabled }
    }
}
