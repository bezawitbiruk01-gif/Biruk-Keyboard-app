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
import com.biruk.keyboardapp.layout.defaultConsoleLayoutSpec
import com.biruk.keyboardapp.model.KeyboardBank
import com.biruk.keyboardapp.model.KeyboardUiState
import com.biruk.keyboardapp.model.defaultKeyboardBank
import com.google.android.material.button.MaterialButton

class KeyboardConsoleActivity : AppCompatActivity() {
    private val engine = KeyboardEngine()
    private val keyboardBank: KeyboardBank = defaultKeyboardBank()
    private val layoutSpec = defaultConsoleLayoutSpec()

    private var uiState = KeyboardUiState()

    private lateinit var voiceValue: TextView
    private lateinit var styleValue: TextView
    private lateinit var registrationValue: TextView
    private lateinit var modeValue: TextView
    private lateinit var transposeValue: TextView
    private lateinit var tempoValue: TextView
    private lateinit var mixerValue: TextView
    private lateinit var effectValue: TextView
    private lateinit var noteValue: TextView
    private lateinit var statusValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        engine.start()
        setContentView(buildContent())
        refreshDisplay("Landscape Genos-inspired console ready")
    }

    override fun onDestroy() {
        engine.release()
        super.onDestroy()
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        root.addView(buildHeader())
        root.addView(buildTransportBar())
        root.addView(buildWorkspace(), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, layoutSpec.workspaceFlex))
        root.addView(buildKeyboardStrip(), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(layoutSpec.keyboardStripHeightDp)))

        return root
    }

    private fun buildHeader(): View {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(SURFACE)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        val titleStack = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

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
        badges.addView(statusBadge("ORIGINAL UI", TEAL))

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

        add("VOICE") { cycleVoice() }
        add("STYLE") { cycleStyle() }
        add("REG") { cycleRegistration() }
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

        val leftPanel = panelCard(layoutSpec.sections[0].title) {
            add(sectionHint(layoutSpec.sections[0].accentHint))
            keyboardBank.voices.forEachIndexed { index, voice ->
                add(controlMiniButton(voice) {
                    uiState = uiState.copy(voiceIndex = index)
                    refreshDisplay("Voice selected: $voice")
                })
            }
        }

        val centerPanel = panelCard(layoutSpec.sections[1].title) {
            add(metricRow("Voice", keyboardBank.voices[uiState.voiceIndex]))
            add(metricRow("Style", keyboardBank.styles[uiState.styleIndex]))
            add(metricRow("Registration", keyboardBank.registrations[uiState.registrationIndex]))
            add(metricRow("Modes", currentModesText()))
            add(metricRow("Transpose", transposeText()))
            add(metricRow("Tempo", "${uiState.tempo} BPM"))
            add(metricRow("Mixer", "${uiState.mixerBalance} / 100"))
            add(metricRow("Effect", "${uiState.effectDepth} / 100"))
            add(metricRow("Keys", "Ready"))
            add(metricRow("Status", uiState.statusText))

            add(spacer(12))

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

        val rightPanel = panelCard(layoutSpec.sections[2].title) {
            add(sectionHint(layoutSpec.sections[2].accentHint))
            add(controlMiniButton("Style next") { cycleStyle() })
            add(controlMiniButton("Voice next") { cycleVoice() })
            add(controlMiniButton("Reg next") { cycleRegistration() })
            add(controlMiniButton("Tempo +") { adjustTempo(5) })
            add(controlMiniButton("Tempo -") { adjustTempo(-5) })
            add(controlMiniButton("Mixer +") { adjustMixer(5) })
            add(controlMiniButton("Effect +") { adjustEffect(5) })
            add(controlMiniButton("Stop All") { engine.stopAll(); refreshDisplay("All voices stopped") })
        }

        workspace.addView(leftPanel, LinearLayout.LayoutParams(dp(192), LinearLayout.LayoutParams.MATCH_PARENT).apply {
            marginEnd = dp(10)
        })
        workspace.addView(centerPanel, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
            marginEnd = dp(10)
        })
        workspace.addView(rightPanel, LinearLayout.LayoutParams(dp(192), LinearLayout.LayoutParams.MATCH_PARENT))

        return workspace
    }

    private fun buildKeyboardStrip(): View {
        val keyboardShell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(SURFACE)
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }

        keyboardShell.addView(TextView(this).apply {
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
            onNotesChanged = { labels -> noteValue.text = buildLabel("Keys", labels.ifBlank { "Ready" }) },
        )

        keyboardScroll.addView(keyboardView)
        keyboardShell.addView(keyboardScroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))
        return keyboardShell
    }

    private fun panelCard(title: String, body: LinearLayout.() -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(SURFACE)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            addView(TextView(this@KeyboardConsoleActivity).apply {
                text = title
                setTextColor(TEXT_SECONDARY)
                textSize = 11.5f
            })
            body()
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

    private fun sectionHint(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(TEXT_SECONDARY)
            textSize = 11f
            setPadding(0, 0, 0, dp(8))
        }
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
        uiState = uiState.copy(statusText = status)
        voiceValue.text = buildLabel("Voice", keyboardBank.voices[uiState.voiceIndex])
        styleValue.text = buildLabel("Style", keyboardBank.styles[uiState.styleIndex])
        registrationValue.text = buildLabel("Registration", keyboardBank.registrations[uiState.registrationIndex])
        modeValue.text = buildLabel("Modes", currentModesText())
        transposeValue.text = buildLabel("Transpose", transposeText())
        tempoValue.text = buildLabel("Tempo", "${uiState.tempo} BPM")
        mixerValue.text = buildLabel("Mixer", "${uiState.mixerBalance} / 100")
        effectValue.text = buildLabel("Effect", "${uiState.effectDepth} / 100")
        noteValue.text = buildLabel("Keys", "Ready")
        statusValue.text = buildLabel("Status", uiState.statusText)
    }

    private fun resetControls() {
        uiState = uiState.copy(
            splitEnabled = false,
            layerEnabled = false,
            sustainEnabled = false,
            transposeSemitones = 0,
            tempo = 120,
            mixerBalance = 50,
            effectDepth = 35,
            statusText = "Reset to center",
        )
        engine.transposeSemitones = 0
        refreshDisplay("Reset to center")
    }

    private fun cycleVoice() {
        val next = (uiState.voiceIndex + 1) % keyboardBank.voices.size
        uiState = uiState.copy(voiceIndex = next, statusText = "Voice changed")
        refreshDisplay("Voice changed")
    }

    private fun cycleStyle() {
        val next = (uiState.styleIndex + 1) % keyboardBank.styles.size
        uiState = uiState.copy(styleIndex = next, statusText = "Style changed")
        refreshDisplay("Style changed")
    }

    private fun cycleRegistration() {
        val next = (uiState.registrationIndex + 1) % keyboardBank.registrations.size
        uiState = uiState.copy(registrationIndex = next, statusText = "Registration changed")
        refreshDisplay("Registration changed")
    }

    private fun toggleSplit() {
        uiState = uiState.copy(splitEnabled = !uiState.splitEnabled, statusText = "Split toggled")
        refreshDisplay("Split toggled")
    }

    private fun toggleLayer() {
        uiState = uiState.copy(layerEnabled = !uiState.layerEnabled, statusText = "Layer toggled")
        refreshDisplay("Layer toggled")
    }

    private fun toggleSustain() {
        uiState = uiState.copy(sustainEnabled = !uiState.sustainEnabled, statusText = "Sustain toggled")
        refreshDisplay("Sustain toggled")
    }

    private fun adjustTranspose(delta: Int) {
        uiState = uiState.copy(transposeSemitones = uiState.transposeSemitones + delta)
        engine.transposeSemitones = uiState.transposeSemitones
        refreshDisplay(if (delta > 0) "Transpose up" else "Transpose down")
    }

    private fun adjustTempo(delta: Int) {
        uiState = uiState.copy(tempo = (uiState.tempo + delta).coerceIn(40, 240))
        refreshDisplay(if (delta > 0) "Tempo up" else "Tempo down")
    }

    private fun adjustMixer(delta: Int) {
        uiState = uiState.copy(mixerBalance = (uiState.mixerBalance + delta).coerceIn(0, 100))
        refreshDisplay(if (delta > 0) "Mixer up" else "Mixer down")
    }

    private fun adjustEffect(delta: Int) {
        uiState = uiState.copy(effectDepth = (uiState.effectDepth + delta).coerceIn(0, 100))
        refreshDisplay(if (delta > 0) "Effect up" else "Effect down")
    }

    private fun currentModesText(): String = listOf(
        if (uiState.splitEnabled) "Split On" else "Split Off",
        if (uiState.layerEnabled) "Layer On" else "Layer Off",
        if (uiState.sustainEnabled) "Sustain On" else "Sustain Off",
    ).joinToString(" · ")

    private fun transposeText(): String = if (uiState.transposeSemitones >= 0) {
        "+${uiState.transposeSemitones} semitones"
    } else {
        "${uiState.transposeSemitones} semitones"
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
        private val CHIP = Color.parseColor("#182433")
        private val TEXT_PRIMARY = Color.parseColor("#F6FAFF")
        private val TEXT_SECONDARY = Color.parseColor("#B5C1D1")
    }
}
