package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.localization.AppLanguage
import com.example.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.smileDataStore: DataStore<Preferences> by preferencesDataStore(name = "smile_prefs")

enum class ThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System")
}

enum class ChatWallpaper(val label: String) {
    WHITE("Classic White"),
    CREAM("Warm Cream"),
    SUNNY("Smile Yellow Tint"),
    MINT("Fresh Mint"),
    SLATE("Night Slate")
}

data class SmileSettingsState(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val wallpaper: ChatWallpaper = ChatWallpaper.CREAM,
    val appLockPinHash: String = "",
    val notificationsEnabled: Boolean = true,
    val dataSaverEnabled: Boolean = false,
    val autoDownloadImages: Boolean = true,
    val e2eePublicKey: String = "",
    val e2eePrivateKey: String = ""
)

class SmilePreferences(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val WALLPAPER = stringPreferencesKey("wallpaper")
        val APP_LOCK_PIN_HASH = stringPreferencesKey("app_lock_pin_hash")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DATA_SAVER = booleanPreferencesKey("data_saver")
        val AUTO_DOWNLOAD_IMAGES = booleanPreferencesKey("auto_download_images")
        val E2EE_PUBLIC_KEY = stringPreferencesKey("e2ee_public_key")
        val E2EE_PRIVATE_KEY = stringPreferencesKey("e2ee_private_key")
    }

    val settingsFlow: Flow<SmileSettingsState> = context.smileDataStore.data.map { prefs ->
        val theme = ThemeMode.entries.firstOrNull { it.name == prefs[Keys.THEME_MODE] } ?: ThemeMode.LIGHT
        val lang = AppLanguage.entries.firstOrNull { it.name == prefs[Keys.LANGUAGE] } ?: AppLanguage.ENGLISH
        val wall = ChatWallpaper.entries.firstOrNull { it.name == prefs[Keys.WALLPAPER] } ?: ChatWallpaper.CREAM
        SmileSettingsState(
            themeMode = theme,
            language = lang,
            wallpaper = wall,
            appLockPinHash = prefs[Keys.APP_LOCK_PIN_HASH].orEmpty(),
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            dataSaverEnabled = prefs[Keys.DATA_SAVER] ?: false,
            autoDownloadImages = prefs[Keys.AUTO_DOWNLOAD_IMAGES] ?: true,
            e2eePublicKey = prefs[Keys.E2EE_PUBLIC_KEY].orEmpty(),
            e2eePrivateKey = prefs[Keys.E2EE_PRIVATE_KEY].orEmpty()
        )
    }

    suspend fun ensureE2eeKeyPair(): CryptoManager.KeyPairEncoded {
        val current = settingsFlow.first()
        if (current.e2eePublicKey.isNotBlank() && current.e2eePrivateKey.isNotBlank()) {
            return CryptoManager.KeyPairEncoded(current.e2eePublicKey, current.e2eePrivateKey)
        }
        val generated = CryptoManager.generateEcKeyPair()
        context.smileDataStore.edit { prefs ->
            prefs[Keys.E2EE_PUBLIC_KEY] = generated.publicKeyBase64
            prefs[Keys.E2EE_PRIVATE_KEY] = generated.privateKeyBase64
        }
        return generated
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.smileDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.smileDataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setWallpaper(wallpaper: ChatWallpaper) {
        context.smileDataStore.edit { it[Keys.WALLPAPER] = wallpaper.name }
    }

    suspend fun setAppLockPinHash(hash: String) {
        context.smileDataStore.edit { it[Keys.APP_LOCK_PIN_HASH] = hash }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.smileDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setDataSaverEnabled(enabled: Boolean) {
        context.smileDataStore.edit { it[Keys.DATA_SAVER] = enabled }
    }

    suspend fun setAutoDownloadImages(enabled: Boolean) {
        context.smileDataStore.edit { it[Keys.AUTO_DOWNLOAD_IMAGES] = enabled }
    }
}
