package com.biruk.keyboardapp.state

import android.content.Context
import com.biruk.keyboardapp.model.KeyboardUiState

class KeyboardSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): KeyboardUiState {
        return KeyboardUiState(
            voiceIndex = prefs.getInt(KEY_VOICE_INDEX, 0),
            styleIndex = prefs.getInt(KEY_STYLE_INDEX, 0),
            registrationIndex = prefs.getInt(KEY_REGISTRATION_INDEX, 0),
            splitEnabled = prefs.getBoolean(KEY_SPLIT_ENABLED, false),
            layerEnabled = prefs.getBoolean(KEY_LAYER_ENABLED, false),
            sustainEnabled = prefs.getBoolean(KEY_SUSTAIN_ENABLED, false),
            transposeSemitones = prefs.getInt(KEY_TRANSPOSE, 0),
            tempo = prefs.getInt(KEY_TEMPO, 120),
            mixerBalance = prefs.getInt(KEY_MIXER_BALANCE, 50),
            effectDepth = prefs.getInt(KEY_EFFECT_DEPTH, 35),
            lastScreenId = prefs.getString(KEY_LAST_SCREEN_ID, "performance") ?: "performance",
            favoriteRegistrationsCsv = prefs.getString(KEY_FAVORITES, "") ?: "",
            statusText = prefs.getString(KEY_STATUS_TEXT, "Ready") ?: "Ready",
        )
    }

    fun save(state: KeyboardUiState) {
        prefs.edit()
            .putInt(KEY_VOICE_INDEX, state.voiceIndex)
            .putInt(KEY_STYLE_INDEX, state.styleIndex)
            .putInt(KEY_REGISTRATION_INDEX, state.registrationIndex)
            .putBoolean(KEY_SPLIT_ENABLED, state.splitEnabled)
            .putBoolean(KEY_LAYER_ENABLED, state.layerEnabled)
            .putBoolean(KEY_SUSTAIN_ENABLED, state.sustainEnabled)
            .putInt(KEY_TRANSPOSE, state.transposeSemitones)
            .putInt(KEY_TEMPO, state.tempo)
            .putInt(KEY_MIXER_BALANCE, state.mixerBalance)
            .putInt(KEY_EFFECT_DEPTH, state.effectDepth)
            .putString(KEY_LAST_SCREEN_ID, state.lastScreenId)
            .putString(KEY_FAVORITES, state.favoriteRegistrationsCsv)
            .putString(KEY_STATUS_TEXT, state.statusText)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "keyboard_session_store"
        private const val KEY_VOICE_INDEX = "voice_index"
        private const val KEY_STYLE_INDEX = "style_index"
        private const val KEY_REGISTRATION_INDEX = "registration_index"
        private const val KEY_SPLIT_ENABLED = "split_enabled"
        private const val KEY_LAYER_ENABLED = "layer_enabled"
        private const val KEY_SUSTAIN_ENABLED = "sustain_enabled"
        private const val KEY_TRANSPOSE = "transpose"
        private const val KEY_TEMPO = "tempo"
        private const val KEY_MIXER_BALANCE = "mixer_balance"
        private const val KEY_EFFECT_DEPTH = "effect_depth"
        private const val KEY_LAST_SCREEN_ID = "last_screen_id"
        private const val KEY_FAVORITES = "favorite_registrations_csv"
        private const val KEY_STATUS_TEXT = "status_text"
    }
}
