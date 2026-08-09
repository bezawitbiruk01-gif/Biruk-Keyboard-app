package com.biruk.keyboardapp

import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.biruk.keyboardapp.layout.ConsoleLayoutSpec
import com.biruk.keyboardapp.layout.defaultConsoleLayoutSpec
import com.biruk.keyboardapp.model.KeyboardBank
import com.biruk.keyboardapp.model.KeyboardConsoleSection
import com.biruk.keyboardapp.model.KeyboardUiState
import com.biruk.keyboardapp.model.defaultKeyboardBank
import com.biruk.keyboardapp.state.KeyboardSessionStore
import com.google.android.material.button.MaterialButton

class KeyboardConsoleActivity : AppCompatActivity() {
    private val engine = KeyboardEngine()
    private val keyboardBank: KeyboardBank = defaultKeyboardBank()
    private val layoutSpec: ConsoleLayoutSpec = defaultConsoleLayoutSpec()

    private lateinit var sessionStore: KeyboardSessionStore
    private var uiState: KeyboardUiState = KeyboardUiState()

    private lateinit var voiceValue: TextView
    private lateinit var styleValue: TextView
    private lateinit var registrationValue: TextView
    private lateinit var modeValue: TextView
    private lateinit var transposeValue: TextView
    private lateinit var tempoValue: TextView
    private lateinit var mixerValue: TextView
    private lateinit var effectValue: TextView
    private lateinit var lastScreenValue: TextView
    private lateinit var favoritesValue: TextView
    private lateinit var noteValue: TextView
    private lateinit var statusValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        sessionStore = KeyboardSessionStore(this)
        uiState = sanitizeState(sessionStore.load())
        engine.transposeSemitones = uiState.transposeSemitones
        engine.start()
        setContentView(buildContent())
        refreshDisplay(uiState.statusText.ifBlank { "Landscape Genos-inspired console ready" })
    }

    override fun onPause() {
        sessionStore.save(uiState)
        super.onPause()
    }

    override fun onDestroy() {
        sessionStore.save(uiState)
        engine.release()
        super.onDestroy()
    }

    private fun buildContent(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            addView(buildHeader())
            addView(buildTransportBar())
            addView(
                buildWorkspace(),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    layoutSpec.workspaceFlex,
                ),
            )
            addView(
                buildKeyboardStrip(),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(layoutSpec.keyboardStripHeightDp),
                ),
            )
        }
    }

    private fun buildHeader(): View {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(SURFACE)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        val titleStack = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleStack.addView(TextView(this).apply {
            text = uiState.title
            setTextColor(TEXT_PRIMARY)
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        titleStack.addView(TextView(this).apply {
            text = uiState.subtitle
            setTextColor(TEXT_SECONDARY)
            textSize = 11.5f
        })

        val badges = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        badges.addView(statusBadge("OFFLINE", PRIMARY_VARIANT))
        badges.addView(statusBadge("LANDSCAPE", ACCENT))
        badges.addView(statusBadge(lastScreenBadgeText(), TEAL))

        header.addView(titleStack, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(badges)
        return header
    }

    private fun buildTransportBar(): View {
        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, dp(10), 0, dp(10))
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        fun add(label: String, action: () -> Unit) {
            row.addView(chipButton(label, action))
        }

        add("VOICE") { selectLastScreen("voice"); cycleVoice() }
        add("STYLE") { selectLastScreen("performance"); cycleStyle() }
        add("REG") { selectLastScreen("performance"); cycleRegistration() }
        add("FAV") { selectLastScreen("performance"); toggleFavoriteRegistration() }
        add("SPLIT") { toggleSplit() }
        add("LAYER") { toggleLayer() }
        add("SUSTAIN") { toggleSustain() }
        add("MIXER") { adjustMixer(10) }
        add("EFFECT") { adjustEffect(5) }
        add("TEMPO -") { adjustTempo(-5) }
        add("TEMPO +") { adjustTempo(5) }
        add("TRANS -") { adjustTranspose(-1) }
        add("TRANS +") { adjustTranspose(1) }
        add("OCT -") { adjustTranspose(-12) }
        add("OCT +") { adjustTranspose(12) }
        add("REC") { refreshDisplay("Recording armed") }
        add("PLAY") { refreshDisplay("Playback started") }
        add("STOP") { engine.stopAll(); refreshDisplay("Playback stopped") }

        scroll.addView(row)
        return scroll
    }

    private fun buildWorkspace(): View {
        val workspace = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(8))
        }

        val leftPanel = buildPanel(layoutSpec.sections[0], layoutSpec.sections[0].accentHint) {
            keyboardBank.voices.forEachIndexed { index, voice ->
                add(controlMiniButton(voice) {
                    updateState(
                        uiState.copy(
                            voiceIndex = index,
                            lastScreenId = "voice",
                            statusText = "Voice selected: $voice",
                        ),
                    )
                })
            }
        }

        val centerPanel = buildPanel(layoutSpec.sections[1], layoutSpec.sections[1].accentHint) {
            voiceValue = metricRow("Voice", keyboardBank.voices[uiState.voiceIndex])
            styleValue = metricRow("Style", keyboardBank.styles[uiState.styleIndex])
            registrationValue = metricRow("Registration", keyboardBank.registrations[uiState.registrationIndex])
            modeValue = metricRow("Modes", currentModesText())
            transposeValue = metricRow("Transpose", transposeText())
            tempoValue = metricRow("Tempo", "${uiState.tempo} BPM")
            mixerValue = metricRow("Mixer", "${uiState.mixerBalance} / 100")
            effectValue = metricRow("Effect", "${uiState.effectDepth} / 100")
            lastScreenValue = metricRow("Last Screen", uiState.lastScreenId)
            favoritesValue = metricRow("Favorites", favoritesSummary())
            noteValue = metricRow("Keys", "Ready")
            statusValue = metricRow("Status", uiState.statusText)

            spacer(12)
            add(TextView(this@KeyboardConsoleActivity).apply {
                text = "Quick performance"
                setTextColor(TEXT_SECONDARY)
                textSize = 12f
            })

            val quickRow = LinearLayout(this@KeyboardConsoleActivity).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            quickRow.addView(chipButton("SPLIT") { toggleSplit() })
            quickRow.addView(chipButton("LAYER") { toggleLayer() })
            quickRow.addView(chipButton("SUSTAIN") { toggleSustain() })
            quickRow.addView(chipButton("CENTER") { resetControls() })
            add(quickRow)
        }

        val rightPanel = buildPanel(layoutSpec.sections[2], layoutSpec.sections[2].accentHint) {
            add(controlMiniButton("Style next") { selectLastScreen("performance"); cycleStyle() })
            add(controlMiniButton("Voice next") { selectLastScreen("voice"); cycleVoice() })
            add(controlMiniButton("Reg next") { selectLastScreen("performance"); cycleRegistration() })
            add(controlMiniButton("Tempo +") { adjustTempo(5) })
            add(controlMiniButton("Tempo -") { adjustTempo(-5) })
            add(controlMiniButton("Mixer +") { adjustMixer(5) })
            add(controlMiniButton("Effect +") { adjustEffect(5) })
            add(controlMiniButton("Stop All") { engine.stopAll(); refreshDisplay("All voices stopped") })
        }

        workspace.addView(leftPanel, LinearLayout.LayoutParams(dp(192), LinearLayout.LayoutParams.MATCH_PARENT).apply { marginEnd = dp(10) })
        workspace.addView(centerPanel, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply { marginEnd = dp(10) })
        workspace.addView(rightPanel, LinearLayout.LayoutParams(dp(192), LinearLayout.LayoutParams.MATCH_PARENT))
        return workspace
    }

    private fun buildKeyboardStrip(): View {
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(SURFACE)
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }

        shell.addView(TextView(this).apply {
            text = "FULL KEYBOARD"
            setTextColor(TEXT_SECONDARY)
            textSize = 11f
        })

        val keyboardScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, dp(8), 0, 0)
        }

        val keyboardView = KeyboardSurfaceView(
            context = this,
            engine = engine,
            onNotesChanged = { labels ->
                uiState = sanitizeState(uiState.copy(lastScreenId = "keyboard"))
                noteValue.text = buildLabel("Keys", labels.ifBlank { "Ready" })
                lastScreenValue.text = buildLabel("Last Screen", uiState.lastScreenId)
            },
        )

        keyboardScroll.addView(keyboardView)
        shell.addView(keyboardScroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))
        return shell
    }

    private fun buildPanel(section: KeyboardConsoleSection, hint: String, content: LinearLayout.() -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(SURFACE)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            addView(TextView(this@KeyboardConsoleActivity).apply {
                text = section.title
                setTextColor(TEXT_SECONDARY)
                textSize = 11.5f
            })
            addView(TextView(this@KeyboardConsoleActivity).apply {
                text = hint
                setTextColor(TEXT_SECONDARY)
                textSize = 11f
                setPadding(0, 0, 0, dp(8))
            })
            content.invoke(this)
        }
    }

    private fun LinearLayout.metricRow(label: String, value: String): TextView {
        return TextView(context).apply {
            text = buildLabel(label, value)
            setTextColor(TEXT_PRIMARY)
            textSize = 14f
            setPadding(0, dp(4), 0, dp(4))
            typeface = android.graphics.Typeface.MONOSPACE
        }.also { addView(it) }
    }

    private fun LinearLayout.spacer(heightDp: Int): View {
        return View(context).also {
            addView(it, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp)))
        }
    }

    private fun LinearLayout.add(view: View): View {
        addView(view)
        return view
    }

    private fun chipButton(label: String, action: () -> Unit): MaterialButton {
        return MaterialButton(this).apply {
            text = label
            setTextColor(TEXT_PRIMARY)
            isAllCaps = false
            cornerRadius = dp(14)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            backgroundTintList = ColorStateList.valueOf(CHIP)
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(36)).apply {
                marginEnd = dp(8)
                bottomMargin = dp(8)
            }
        }
    }

    private fun controlMiniButton(label: String, action: () -> Unit): MaterialButton {
        return MaterialButton(this).apply {
            text = label
            setTextColor(TEXT_PRIMARY)
            isAllCaps = false
            cornerRadius = dp(14)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            backgroundTintList = ColorStateList.valueOf(PRIMARY)
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40)).apply {
                bottomMargin = dp(8)
            }
        }
    }

    private fun statusBadge(text: String, color: Int): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 10.5f
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setBackgroundColor(color)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = dp(6)
            }
        }
    }

    private fun refreshDisplay(status: String) {
        uiState = sanitizeState(uiState.copy(statusText = status))
        engine.transposeSemitones = uiState.transposeSemitones
        voiceValue.text = buildLabel("Voice", keyboardBank.voices[uiState.voiceIndex])
        styleValue.text = buildLabel("Style", keyboardBank.styles[uiState.styleIndex])
        registrationValue.text = buildLabel("Registration", keyboardBank.registrations[uiState.registrationIndex])
        modeValue.text = buildLabel("Modes", currentModesText())
        transposeValue.text = buildLabel("Transpose", transposeText())
        tempoValue.text = buildLabel("Tempo", "${uiState.tempo} BPM")
        mixerValue.text = buildLabel("Mixer", "${uiState.mixerBalance} / 100")
        effectValue.text = buildLabel("Effect", "${uiState.effectDepth} / 100")
        lastScreenValue.text = buildLabel("Last Screen", uiState.lastScreenId)
        favoritesValue.text = buildLabel("Favorites", favoritesSummary())
        noteValue.text = buildLabel("Keys", "Ready")
        statusValue.text = buildLabel("Status", uiState.statusText)
    }

    private fun updateState(next: KeyboardUiState) {
        uiState = sanitizeState(next)
        engine.transposeSemitones = uiState.transposeSemitones
        refreshDisplay(uiState.statusText)
    }

    private fun resetControls() {
        updateState(
            uiState.copy(
                splitEnabled = false,
                layerEnabled = false,
                sustainEnabled = false,
                transposeSemitones = 0,
                tempo = 120,
                mixerBalance = 50,
                effectDepth = 35,
                lastScreenId = "performance",
                statusText = "Reset to center",
            ),
        )
    }

    private fun cycleVoice() {
        updateState(uiState.copy(voiceIndex = (uiState.voiceIndex + 1) % keyboardBank.voices.size, lastScreenId = "voice", statusText = "Voice changed"))
    }

    private fun cycleStyle() {
        updateState(uiState.copy(styleIndex = (uiState.styleIndex + 1) % keyboardBank.styles.size, lastScreenId = "performance", statusText = "Style changed"))
    }

    private fun cycleRegistration() {
        updateState(uiState.copy(registrationIndex = (uiState.registrationIndex + 1) % keyboardBank.registrations.size, lastScreenId = "performance", statusText = "Registration changed"))
    }

    private fun toggleFavoriteRegistration() {
        val current = keyboardBank.registrations[uiState.registrationIndex]
        val favorites = favoriteRegistrationSet().toMutableSet()
        val nowFavorite = if (favorites.contains(current)) {
            favorites.remove(current)
            false
        } else {
            favorites.add(current)
            true
        }
        updateState(
            uiState.copy(
                favoriteRegistrationsCsv = favorites.joinToString(","),
                lastScreenId = "performance",
                statusText = if (nowFavorite) "Favorite saved: $current" else "Favorite removed: $current",
            ),
        )
    }

    private fun toggleSplit() {
        updateState(uiState.copy(splitEnabled = !uiState.splitEnabled, lastScreenId = "performance", statusText = "Split toggled"))
    }

    private fun toggleLayer() {
        updateState(uiState.copy(layerEnabled = !uiState.layerEnabled, lastScreenId = "performance", statusText = "Layer toggled"))
    }

    private fun toggleSustain() {
        updateState(uiState.copy(sustainEnabled = !uiState.sustainEnabled, lastScreenId = "performance", statusText = "Sustain toggled"))
    }

    private fun adjustTranspose(delta: Int) {
        updateState(uiState.copy(transposeSemitones = (uiState.transposeSemitones + delta).coerceIn(-24, 24), lastScreenId = "performance", statusText = "Transpose adjusted"))
    }

    private fun adjustTempo(delta: Int) {
        updateState(uiState.copy(tempo = (uiState.tempo + delta).coerceIn(40, 240), lastScreenId = "performance", statusText = if (delta > 0) "Tempo up" else "Tempo down"))
    }

    private fun adjustMixer(delta: Int) {
        updateState(uiState.copy(mixerBalance = (uiState.mixerBalance + delta).coerceIn(0, 100), lastScreenId = "performance", statusText = if (delta > 0) "Mixer up" else "Mixer down"))
    }

    private fun adjustEffect(delta: Int) {
        updateState(uiState.copy(effectDepth = (uiState.effectDepth + delta).coerceIn(0, 100), lastScreenId = "performance", statusText = if (delta > 0) "Effect up" else "Effect down"))
    }

    private fun selectLastScreen(screenId: String) {
        updateState(uiState.copy(lastScreenId = screenId))
    }

    private fun currentModesText(): String {
        return listOf(
            if (uiState.splitEnabled) "Split On" else "Split Off",
            if (uiState.layerEnabled) "Layer On" else "Layer Off",
            if (uiState.sustainEnabled) "Sustain On" else "Sustain Off",
        ).joinToString(" · ")
    }

    private fun transposeText(): String {
        return if (uiState.transposeSemitones >= 0) "+${uiState.transposeSemitones} semitones" else "${uiState.transposeSemitones} semitones"
    }

    private fun favoriteRegistrationSet(): Set<String> {
        return uiState.favoriteRegistrationsCsv.split(',').map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    private fun favoritesSummary(): String {
        val favorites = favoriteRegistrationSet()
        return if (favorites.isEmpty()) "None" else favorites.joinToString(" · ")
    }

    private fun lastScreenBadgeText(): String {
        return when (uiState.lastScreenId.lowercase()) {
            "voice" -> "VOICE"
            "keyboard" -> "KEYBOARD"
            else -> uiState.lastScreenId.uppercase()
        }
    }

    private fun sanitizeState(state: KeyboardUiState): KeyboardUiState {
        return state.copy(
            voiceIndex = state.voiceIndex.coerceIn(0, keyboardBank.voices.lastIndex),
            styleIndex = state.styleIndex.coerceIn(0, keyboardBank.styles.lastIndex),
            registrationIndex = state.registrationIndex.coerceIn(0, keyboardBank.registrations.lastIndex),
            tempo = state.tempo.coerceIn(40, 240),
            mixerBalance = state.mixerBalance.coerceIn(0, 100),
            effectDepth = state.effectDepth.coerceIn(0, 100),
            transposeSemitones = state.transposeSemitones.coerceIn(-24, 24),
        )
    }

    private fun buildLabel(label: String, value: String): String = "$label: $value"

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private val BG = Color.parseColor("#09111A")
        private val SURFACE = Color.parseColor("#101A24")
        private val PRIMARY = Color.parseColor("#4D8DFF")
        private val PRIMARY_VARIANT = Color.parseColor("#2757C9")
        private val ACCENT = Color.parseColor("#7E57C2")
        private val TEAL = Color.parseColor("#00897B")
        private val CHIP = Color.parseColor("#1B2736")
        private val TEXT_PRIMARY = Color.parseColor("#F6FAFF")
        private val TEXT_SECONDARY = Color.parseColor("#B5C1D1")
    }
}
