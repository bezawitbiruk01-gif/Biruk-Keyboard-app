package com.biruk.keyboardapp

import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    private val engine = KeyboardEngine()

    private val voicePresets = listOf(
        "Grand Piano",
        "Bright Piano",
        "Electric Piano",
        "Stage Organ",
        "Warm Strings",
        "Brass Section",
        "Synth Pad",
        "Acoustic Bass",
    )

    private val stylePresets = listOf(
        "Pop Ballad",
        "Dance Pop",
        "Latin Groove",
        "Waltz",
        "Rock Band",
        "Funk Session",
        "Afro Groove",
        "House Beat",
    )

    private val registrationNames = listOf(
        "Intro Setup",
        "Ballad Flow",
        "Dance Lead",
        "Live Band",
        "Church Pad",
        "Strings Layer",
        "Piano Split",
        "Finale",
    )

    private var voiceIndex = 0
    private var styleIndex = 0
    private var registrationIndex = 0
    private var splitEnabled = false
    private var layerEnabled = false
    private var sustainEnabled = false
    private var transposeSemitones = 0
    private var tempo = 120
    private var mixerBalance = 50
    private var effectDepth = 35

    private lateinit var voiceValue: TextView
    private lateinit var styleValue: TextView
    private lateinit var regValue: TextView
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
        refreshDisplay("Landscape Genos-style console ready")
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
        root.addView(buildWorkspace(), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(buildKeyboardStrip(), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(260)))

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
            text = "BIRUK MUSIC KEYBOARD"
            setTextColor(TEXT_PRIMARY)
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        titleStack.addView(TextView(this).apply {
            text = "Landscape-only performance console · offline synth · arranger workflow"
            setTextColor(TEXT_SECONDARY)
            textSize = 11.5f
        })

        val badges = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        badges.addView(statusBadge("OFFLINE", PRIMARY_VARIANT))
        badges.addView(statusBadge("LANDSCAPE", ACCENT))
        badges.addView(statusBadge("GENOS INSPIRED", TEAL))

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
        add("SPLIT") { splitEnabled = !splitEnabled; refreshDisplay("Split toggled") }
        add("LAYER") { layerEnabled = !layerEnabled; refreshDisplay("Layer toggled") }
        add("SUSTAIN") { sustainEnabled = !sustainEnabled; refreshDisplay("Sustain toggled") }
        add("MIXER") { mixerBalance = (mixerBalance + 10).coerceAtMost(100); refreshDisplay("Mixer adjusted") }
        add("EFFECT") { effectDepth = (effectDepth + 5).coerceAtMost(100); refreshDisplay("Effect adjusted") }
        add("TEMPO -") { tempo = (tempo - 5).coerceAtLeast(40); refreshDisplay("Tempo down") }
        add("TEMPO +") { tempo = (tempo + 5).coerceAtMost(240); refreshDisplay("Tempo up") }
        add("TRANS -") { transposeSemitones -= 1; engine.transposeSemitones = transposeSemitones; refreshDisplay("Transpose down") }
        add("TRANS +") { transposeSemitones += 1; engine.transposeSemitones = transposeSemitones; refreshDisplay("Transpose up") }
        add("OCT -") { transposeSemitones -= 12; engine.transposeSemitones = transposeSemitones; refreshDisplay("Octave down") }
        add("OCT +") { transposeSemitones += 12; engine.transposeSemitones = transposeSemitones; refreshDisplay("Octave up") }
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

        val leftPanel = panelCard("VOICE BANK") {
            add(controlMiniButton("Grand Piano") { voiceIndex = 0; refreshDisplay("Grand Piano selected") })
            add(controlMiniButton("Bright Piano") { voiceIndex = 1; refreshDisplay("Bright Piano selected") })
            add(controlMiniButton("EP / Organ") { voiceIndex = 2; refreshDisplay("Electric Piano selected") })
            add(controlMiniButton("Strings") { voiceIndex = 4; refreshDisplay("Strings selected") })
            add(controlMiniButton("Brass") { voiceIndex = 5; refreshDisplay("Brass selected") })
            add(controlMiniButton("Pad") { voiceIndex = 6; refreshDisplay("Pad selected") })
        }

        val centerPanel = panelCard("PERFORMANCE DISPLAY") {
            add(metricRow("Voice", voicePresets[voiceIndex]))
            add(metricRow("Style", stylePresets[styleIndex]))
            add(metricRow("Registration", registrationNames[registrationIndex]))
            add(metricRow("Modes", currentModesText()))
            add(metricRow("Transpose", transposeText()))
            add(metricRow("Tempo", "$tempo BPM"))
            add(metricRow("Mixer", "$mixerBalance / 100"))
            add(metricRow("Effect", "$effectDepth / 100"))
            add(metricRow("Keys", "Ready"))
            add(metricRow("Status", "Touch the keys to play offline audio"))

            add(spacer(12))

            add(TextView(this@MainActivity).apply {
                text = "Quick performance"
                setTextColor(TEXT_SECONDARY)
                textSize = 12f
            })

            val quickRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            quickRow.addView(chipButton("SPLIT") { splitEnabled = !splitEnabled; refreshDisplay("Split toggled") })
            quickRow.addView(chipButton("LAYER") { layerEnabled = !layerEnabled; refreshDisplay("Layer toggled") })
            quickRow.addView(chipButton("SUSTAIN") { sustainEnabled = !sustainEnabled; refreshDisplay("Sustain toggled") })
            quickRow.addView(chipButton("CENTER") { resetControls(); refreshDisplay("Reset to center") })
            add(quickRow)
        }

        val rightPanel = panelCard("PERFORMANCE") {
            add(controlMiniButton("Style next") { cycleStyle() })
            add(controlMiniButton("Voice next") { cycleVoice() })
            add(controlMiniButton("Reg next") { cycleRegistration() })
            add(controlMiniButton("Tempo +") { tempo = (tempo + 5).coerceAtMost(240); refreshDisplay("Tempo up") })
            add(controlMiniButton("Tempo -") { tempo = (tempo - 5).coerceAtLeast(40); refreshDisplay("Tempo down") })
            add(controlMiniButton("Mixer +") { mixerBalance = (mixerBalance + 5).coerceAtMost(100); refreshDisplay("Mixer up") })
            add(controlMiniButton("Effect +") { effectDepth = (effectDepth + 5).coerceAtMost(100); refreshDisplay("Effect up") })
            add(controlMiniButton("Stop All") { engine.stopAll(); refreshDisplay("All voices stopped") })
        }

        workspace.addView(leftPanel, LinearLayout.LayoutParams(dp(190), LinearLayout.LayoutParams.MATCH_PARENT).apply {
            marginEnd = dp(10)
        })
        workspace.addView(centerPanel, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
            marginEnd = dp(10)
        })
        workspace.addView(rightPanel, LinearLayout.LayoutParams(dp(190), LinearLayout.LayoutParams.MATCH_PARENT))

        return workspace
    }

    private fun buildKeyboardStrip(): View {
        val keyboardShell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(SURFACE)
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }

        val title = TextView(this).apply {
            text = "KEYBOARD"
            setTextColor(TEXT_SECONDARY)
            textSize = 11f
        }
        keyboardShell.addView(title)

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
            val titleView = TextView(this@MainActivity).apply {
                text = title
                setTextColor(TEXT_SECONDARY)
                textSize = 11.5f
            }
            addView(titleView)
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
        voiceValue.text = buildLabel("Voice", voicePresets[voiceIndex])
        styleValue.text = buildLabel("Style", stylePresets[styleIndex])
        regValue.text = buildLabel("Registration", registrationNames[registrationIndex])
        modeValue.text = buildLabel("Modes", currentModesText())
        transposeValue.text = buildLabel("Transpose", transposeText())
        tempoValue.text = buildLabel("Tempo", "$tempo BPM")
        mixerValue.text = buildLabel("Mixer", "$mixerBalance / 100")
        effectValue.text = buildLabel("Effect", "$effectDepth / 100")
        noteValue.text = buildLabel("Keys", "Ready")
        statusValue.text = buildLabel("Status", status)
    }

    private fun resetControls() {
        splitEnabled = false
        layerEnabled = false
        sustainEnabled = false
        transposeSemitones = 0
        engine.transposeSemitones = 0
        tempo = 120
        mixerBalance = 50
        effectDepth = 35
    }

    private fun cycleVoice() {
        voiceIndex = (voiceIndex + 1) % voicePresets.size
        refreshDisplay("Voice changed")
    }

    private fun cycleStyle() {
        styleIndex = (styleIndex + 1) % stylePresets.size
        refreshDisplay("Style changed")
    }

    private fun cycleRegistration() {
        registrationIndex = (registrationIndex + 1) % registrationNames.size
        refreshDisplay("Registration changed")
    }

    private fun currentModesText(): String {
        return listOf(
            if (splitEnabled) "Split On" else "Split Off",
            if (layerEnabled) "Layer On" else "Layer Off",
            if (sustainEnabled) "Sustain On" else "Sustain Off",
        ).joinToString(" · ")
    }

    private fun transposeText(): String {
        return if (transposeSemitones >= 0) "+$transposeSemitones semitones" else "$transposeSemitones semitones"
    }

    private fun buildLabel(label: String, value: String): String = "$label: $value"

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private val BG = Color.parseColor("#09111A")
        private val SURFACE = Color.parseColor("#101A24")
        private val PRIMARY = Color.parseColor("#4D8DFF")
        private val PRIMARY_VARIANT = Color.parseColor("#2757C9")
        private val ACCENT = Color.parseColor("#7E57C2")
        private val TEAL = Color.parseColor("#00796B")
        private val CHIP = Color.parseColor("#1B2A39")
        private val TEXT_PRIMARY = Color.parseColor("#F5F8FF")
        private val TEXT_SECONDARY = Color.parseColor("#B9C6D6")
    }
}
