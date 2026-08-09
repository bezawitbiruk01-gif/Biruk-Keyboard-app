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
    private val arranger = ArrangerEngine()
    private val keyboardBank: KeyboardBank = defaultKeyboardBank()
    private val layoutSpec: ConsoleLayoutSpec = defaultConsoleLayoutSpec()
    private lateinit var sessionStore: KeyboardSessionStore
    private var uiState = KeyboardUiState()

    private lateinit var voiceValue: TextView
    private lateinit var styleValue: TextView
    private lateinit var registrationValue: TextView
    private lateinit var modeValue: TextView
    private lateinit var transposeValue: TextView
    private lateinit var tempoValue: TextView
    private lateinit var mixerValue: TextView
    private lateinit var effectValue: TextView
    private lateinit var arrangerValue: TextView
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
        arranger.setListener { state ->
            uiState = sanitizeState(uiState.copy(
                arrangerRunning = state.running,
                arrangerSection = state.section.name,
                syncStartEnabled = state.syncStart,
                fillPending = state.fillPending,
                lastScreenId = "arranger",
            ))
            refreshDisplay("Arranger: ${state.section.name.replace('_', ' ')}${if (state.fillPending) " · FILL" else ""}")
        }
        arranger.restore(
            ArrangerEngine.State(
                running = uiState.arrangerRunning,
                section = runCatching { ArrangerEngine.Section.valueOf(uiState.arrangerSection) }.getOrDefault(ArrangerEngine.Section.STOPPED),
                syncStart = uiState.syncStartEnabled,
                fillPending = uiState.fillPending,
            ),
        )
        refreshDisplay(uiState.statusText.ifBlank { "Landscape arranger console ready" })
    }

    override fun onPause() { sessionStore.save(uiState); super.onPause() }

    override fun onDestroy() {
        sessionStore.save(uiState)
        engine.release()
        super.onDestroy()
    }

    private fun buildContent(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(BG)
        setPadding(dp(12), dp(12), dp(12), dp(12))
        addView(buildHeader())
        addView(buildTransportBar())
        addView(buildWorkspace(), LinearLayout.LayoutParams(-1, 0, layoutSpec.workspaceFlex))
        addView(buildKeyboardStrip(), LinearLayout.LayoutParams(-1, dp(layoutSpec.keyboardStripHeightDp)))
    }

    private fun buildHeader(): View {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(SURFACE)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        val title = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        title.addView(TextView(this).apply {
            text = uiState.title
            setTextColor(TEXT_PRIMARY)
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        title.addView(TextView(this).apply {
            text = uiState.subtitle
            setTextColor(TEXT_SECONDARY)
            textSize = 11.5f
        })
        val badges = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
        badges.addView(statusBadge("OFFLINE", PRIMARY_VARIANT))
        badges.addView(statusBadge("ARRANGER", ACCENT))
        badges.addView(statusBadge(lastScreenBadgeText(), TEAL))
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(badges)
        return header
    }

    private fun buildTransportBar(): View {
        val scroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; setPadding(0, dp(10), 0, dp(10)) }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        fun add(label: String, action: () -> Unit) { row.addView(chipButton(label, action)) }
        add("VOICE") { cycleVoice() }
        add("STYLE") { cycleStyle() }
        add("REG") { cycleRegistration() }
        add("FAV") { toggleFavoriteRegistration() }
        add("START") { arranger.start() }
        add("STOP") { arranger.stop(); engine.stopAll() }
        add("INTRO") { arranger.intro() }
        add("MAIN A") { arranger.main(ArrangerEngine.Section.MAIN_A) }
        add("MAIN B") { arranger.main(ArrangerEngine.Section.MAIN_B) }
        add("MAIN C") { arranger.main(ArrangerEngine.Section.MAIN_C) }
        add("MAIN D") { arranger.main(ArrangerEngine.Section.MAIN_D) }
        add("FILL") { arranger.fill() }
        add("ENDING") { arranger.ending() }
        add("SYNC") { arranger.toggleSyncStart() }
        add("SPLIT") { toggleSplit() }
        add("LAYER") { toggleLayer() }
        add("SUSTAIN") { toggleSustain() }
        add("MIXER+") { adjustMixer(10) }
        add("FX+") { adjustEffect(5) }
        add("TEMPO-") { adjustTempo(-5) }
        add("TEMPO+") { adjustTempo(5) }
        add("TRANS-") { adjustTranspose(-1) }
        add("TRANS+") { adjustTranspose(1) }
        add("REC") { refreshDisplay("Recording armed") }
        add("PLAY") { refreshDisplay("Playback started") }
        scroll.addView(row)
        return scroll
    }

    private fun buildWorkspace(): View {
        val workspace = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, dp(8)) }
        val left = buildPanel(layoutSpec.sections[0], layoutSpec.sections[0].accentHint) {
            keyboardBank.voices.forEachIndexed { index, voice ->
                add(controlMiniButton(voice) { updateState(uiState.copy(voiceIndex = index, lastScreenId = "voice", statusText = "Voice selected: $voice")) })
            }
        }
        val center = buildPanel(layoutSpec.sections[1], "Live performance + arranger status") {
            voiceValue = metricRow("Voice", keyboardBank.voices[uiState.voiceIndex])
            styleValue = metricRow("Style", keyboardBank.styles[uiState.styleIndex])
            registrationValue = metricRow("Registration", keyboardBank.registrations[uiState.registrationIndex])
            modeValue = metricRow("Modes", currentModesText())
            transposeValue = metricRow("Transpose", transposeText())
            tempoValue = metricRow("Tempo", "${uiState.tempo} BPM")
            mixerValue = metricRow("Mixer", "${uiState.mixerBalance} / 100")
            effectValue = metricRow("Effect", "${uiState.effectDepth} / 100")
            arrangerValue = metricRow("Arranger", arrangerText())
            lastScreenValue = metricRow("Last Screen", uiState.lastScreenId)
            favoritesValue = metricRow("Favorites", favoritesSummary())
            noteValue = metricRow("Keys", "Ready")
            statusValue = metricRow("Status", uiState.statusText)
            spacer(8)
            val r = LinearLayout(this@KeyboardConsoleActivity).apply { orientation = LinearLayout.HORIZONTAL }
            r.addView(chipButton("A") { arranger.main(ArrangerEngine.Section.MAIN_A) })
            r.addView(chipButton("B") { arranger.main(ArrangerEngine.Section.MAIN_B) })
            r.addView(chipButton("C") { arranger.main(ArrangerEngine.Section.MAIN_C) })
            r.addView(chipButton("D") { arranger.main(ArrangerEngine.Section.MAIN_D) })
            r.addView(chipButton("FILL") { arranger.fill() })
            add(r)
        }
        val right = buildPanel(layoutSpec.sections[2], "Arranger controls") {
            add(controlMiniButton("START / MAIN A") { arranger.start() })
            add(controlMiniButton("INTRO") { arranger.intro() })
            add(controlMiniButton("FILL") { arranger.fill() })
            add(controlMiniButton("ENDING") { arranger.ending() })
            add(controlMiniButton("SYNC START") { arranger.toggleSyncStart() })
            add(controlMiniButton("STYLE NEXT") { cycleStyle() })
            add(controlMiniButton("TEMPO +") { adjustTempo(5) })
            add(controlMiniButton("STOP ALL") { arranger.stop(); engine.stopAll() })
        }
        workspace.addView(left, LinearLayout.LayoutParams(dp(192), -1).apply { marginEnd = dp(10) })
        workspace.addView(center, LinearLayout.LayoutParams(0, -1, 1f).apply { marginEnd = dp(10) })
        workspace.addView(right, LinearLayout.LayoutParams(dp(192), -1))
        return workspace
    }

    private fun buildKeyboardStrip(): View {
        val shell = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(SURFACE); setPadding(dp(10), dp(10), dp(10), dp(10)) }
        shell.addView(TextView(this).apply { text = "FULL 88-NOTE TOUCH KEYBOARD"; setTextColor(TEXT_SECONDARY); textSize = 11f })
        val scroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; setPadding(0, dp(8), 0, 0) }
        scroll.addView(KeyboardSurfaceView(this, engine) { labels ->
            uiState = sanitizeState(uiState.copy(lastScreenId = "keyboard"))
            noteValue.text = buildLabel("Keys", labels.ifBlank { "Ready" })
            lastScreenValue.text = buildLabel("Last Screen", "keyboard")
        })
        shell.addView(scroll, LinearLayout.LayoutParams(-1, -1))
        return shell
    }

    private fun buildPanel(section: KeyboardConsoleSection, hint: String, content: LinearLayout.() -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(SURFACE)
        setPadding(dp(14), dp(14), dp(14), dp(14))
        addView(TextView(this@KeyboardConsoleActivity).apply { text = section.title; setTextColor(TEXT_SECONDARY); textSize = 11.5f })
        addView(TextView(this@KeyboardConsoleActivity).apply { text = hint; setTextColor(TEXT_SECONDARY); textSize = 11f; setPadding(0, 0, 0, dp(8)) })
        content.invoke(this)
    }

    private fun LinearLayout.metricRow(label: String, value: String) = TextView(context).apply {
        text = buildLabel(label, value); setTextColor(TEXT_PRIMARY); textSize = 13.5f; setPadding(0, dp(3), 0, dp(3)); typeface = android.graphics.Typeface.MONOSPACE
    }.also { addView(it) }

    private fun LinearLayout.spacer(heightDp: Int) = View(context).also { addView(it, LinearLayout.LayoutParams(-1, dp(heightDp))) }
    private fun LinearLayout.add(view: View): View { addView(view); return view }

    private fun chipButton(label: String, action: () -> Unit) = MaterialButton(this).apply {
        text = label; setTextColor(TEXT_PRIMARY); isAllCaps = false; cornerRadius = dp(14); setPadding(dp(12), dp(8), dp(12), dp(8)); backgroundTintList = ColorStateList.valueOf(CHIP); setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-2, dp(36)).apply { marginEnd = dp(8); bottomMargin = dp(8) }
    }

    private fun controlMiniButton(label: String, action: () -> Unit) = MaterialButton(this).apply {
        text = label; setTextColor(TEXT_PRIMARY); isAllCaps = false; cornerRadius = dp(14); setPadding(dp(12), dp(8), dp(12), dp(8)); backgroundTintList = ColorStateList.valueOf(PRIMARY); setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1, dp(40)).apply { bottomMargin = dp(8) }
    }

    private fun statusBadge(text: String, color: Int) = TextView(this).apply {
        this.text = text; setTextColor(Color.WHITE); textSize = 10.5f; gravity = Gravity.CENTER; setPadding(dp(10), dp(6), dp(10), dp(6)); setBackgroundColor(color)
        layoutParams = LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(6) }
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
        arrangerValue.text = buildLabel("Arranger", arrangerText())
        lastScreenValue.text = buildLabel("Last Screen", uiState.lastScreenId)
        favoritesValue.text = buildLabel("Favorites", favoritesSummary())
        noteValue.text = buildLabel("Keys", "Ready")
        statusValue.text = buildLabel("Status", uiState.statusText)
    }

    private fun updateState(next: KeyboardUiState) { uiState = sanitizeState(next); engine.transposeSemitones = uiState.transposeSemitones; refreshDisplay(uiState.statusText) }
    private fun cycleVoice() = updateState(uiState.copy(voiceIndex = (uiState.voiceIndex + 1) % keyboardBank.voices.size, lastScreenId = "voice", statusText = "Voice changed"))
    private fun cycleStyle() = updateState(uiState.copy(styleIndex = (uiState.styleIndex + 1) % keyboardBank.styles.size, lastScreenId = "arranger", statusText = "Style changed"))
    private fun cycleRegistration() = updateState(uiState.copy(registrationIndex = (uiState.registrationIndex + 1) % keyboardBank.registrations.size, lastScreenId = "performance", statusText = "Registration changed"))

    private fun toggleFavoriteRegistration() {
        val current = keyboardBank.registrations[uiState.registrationIndex]
        val favorites = favoriteRegistrationSet().toMutableSet()
        val saved = if (favorites.remove(current)) false else { favorites.add(current); true }
        updateState(uiState.copy(favoriteRegistrationsCsv = favorites.joinToString(","), statusText = if (saved) "Favorite saved: $current" else "Favorite removed: $current"))
    }

    private fun toggleSplit() = updateState(uiState.copy(splitEnabled = !uiState.splitEnabled, statusText = "Split toggled"))
    private fun toggleLayer() = updateState(uiState.copy(layerEnabled = !uiState.layerEnabled, statusText = "Layer toggled"))
    private fun toggleSustain() = updateState(uiState.copy(sustainEnabled = !uiState.sustainEnabled, statusText = "Sustain toggled"))
    private fun adjustTranspose(delta: Int) = updateState(uiState.copy(transposeSemitones = (uiState.transposeSemitones + delta).coerceIn(-24, 24), statusText = "Transpose adjusted"))
    private fun adjustTempo(delta: Int) = updateState(uiState.copy(tempo = (uiState.tempo + delta).coerceIn(40, 240), statusText = if (delta > 0) "Tempo up" else "Tempo down"))
    private fun adjustMixer(delta: Int) = updateState(uiState.copy(mixerBalance = (uiState.mixerBalance + delta).coerceIn(0, 100), statusText = "Mixer adjusted"))
    private fun adjustEffect(delta: Int) = updateState(uiState.copy(effectDepth = (uiState.effectDepth + delta).coerceIn(0, 100), statusText = "Effect adjusted"))

    private fun currentModesText() = listOf(if (uiState.splitEnabled) "Split On" else "Split Off", if (uiState.layerEnabled) "Layer On" else "Layer Off", if (uiState.sustainEnabled) "Sustain On" else "Sustain Off").joinToString(" · ")
    private fun transposeText() = if (uiState.transposeSemitones >= 0) "+${uiState.transposeSemitones} semitones" else "${uiState.transposeSemitones} semitones"
    private fun arrangerText() = "${if (uiState.arrangerRunning) "RUN" else "STOP"} · ${uiState.arrangerSection.replace('_', ' ')}${if (uiState.syncStartEnabled) " · SYNC" else ""}${if (uiState.fillPending) " · FILL" else ""}"
    private fun favoriteRegistrationSet(): Set<String> = uiState.favoriteRegistrationsCsv.split(',').map { it.trim() }.filter { it.isNotBlank() }.toSet()
    private fun favoritesSummary() = favoriteRegistrationSet().takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "None"
    private fun lastScreenBadgeText() = uiState.lastScreenId.uppercase()

    private fun sanitizeState(state: KeyboardUiState) = state.copy(
        voiceIndex = state.voiceIndex.coerceIn(0, keyboardBank.voices.lastIndex),
        styleIndex = state.styleIndex.coerceIn(0, keyboardBank.styles.lastIndex),
        registrationIndex = state.registrationIndex.coerceIn(0, keyboardBank.registrations.lastIndex),
        tempo = state.tempo.coerceIn(40, 240),
        mixerBalance = state.mixerBalance.coerceIn(0, 100),
        effectDepth = state.effectDepth.coerceIn(0, 100),
        transposeSemitones = state.transposeSemitones.coerceIn(-24, 24),
    )

    private fun buildLabel(label: String, value: String) = "$label: $value"
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

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
