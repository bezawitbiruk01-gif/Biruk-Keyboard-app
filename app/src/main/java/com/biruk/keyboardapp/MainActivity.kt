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
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    private val engine = KeyboardEngine()

    private val voicePresets = listOf(
        "Grand Piano",
        "Bright Piano",
        "Electric Piano",
        "Organ",
        "Strings",
        "Brass",
        "Synth Pad",
    )

    private val stylePresets = listOf(
        "Pop Ballad",
        "Dance Pop",
        "Latin Groove",
        "Waltz",
        "Rock Band",
        "Funk Session",
    )

    private var voiceIndex = 0
    private var styleIndex = 0
    private var splitEnabled = false
    private var layerEnabled = false
    private var sustainEnabled = false
    private var transposeSemitones = 0

    private lateinit var voiceValue: TextView
    private lateinit var styleValue: TextView
    private lateinit var modeValue: TextView
    private lateinit var noteValue: TextView
    private lateinit var statusValue: TextView
    private lateinit var transposeValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        engine.start()
        setContentView(buildContent())
        refreshDisplay("Ready")
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

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setBackgroundColor(SURFACE)
        }

        val titleStack = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        titleStack.addView(TextView(this).apply {
            text = "BIRUK MUSIC KEYBOARD"
            setTextColor(TEXT_PRIMARY)
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        titleStack.addView(TextView(this).apply {
            text = "Landscape performance console · offline synth · Genos-inspired workflow"
            setTextColor(TEXT_SECONDARY)
            textSize = 11.5f
        })

        val badge = TextView(this).apply {
            text = "OFFLINE"
            setTextColor(Color.WHITE)
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setBackgroundColor(PRIMARY_VARIANT)
        }

        header.addView(titleStack, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(badge)
        root.addView(header, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val actionsScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, dp(10), 0, dp(10))
        }
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        actionsScroll.addView(actionRow)

        actionRow.addView(controlButton("VOICE") { cycleVoice() })
        actionRow.addView(controlButton("STYLE") { cycleStyle() })
        actionRow.addView(controlButton("SPLIT") { splitEnabled = !splitEnabled; refreshDisplay("Split toggled") })
        actionRow.addView(controlButton("LAYER") { layerEnabled = !layerEnabled; refreshDisplay("Layer toggled") })
        actionRow.addView(controlButton("SUSTAIN") { sustainEnabled = !sustainEnabled; refreshDisplay("Sustain toggled") })
        actionRow.addView(controlButton("MIXER") { refreshDisplay("Mixer opened") })
        actionRow.addView(controlButton("REC") { refreshDisplay("Record ready") })
        actionRow.addView(controlButton("PLAY") { refreshDisplay("Playback started") })
        actionRow.addView(controlButton("STOP") { engine.stopAll(); refreshDisplay("Playback stopped") })
        actionRow.addView(controlButton("TRANS -") { transposeSemitones -= 1; engine.transposeSemitones = transposeSemitones; refreshDisplay("Transpose down") })
        actionRow.addView(controlButton("TRANS +") { transposeSemitones += 1; engine.transposeSemitones = transposeSemitones; refreshDisplay("Transpose up") })
        actionRow.addView(controlButton("OCT -") { transposeSemitones -= 12; engine.transposeSemitones = transposeSemitones; refreshDisplay("Octave down") })
        actionRow.addView(controlButton("OCT +") { transposeSemitones += 12; engine.transposeSemitones = transposeSemitones; refreshDisplay("Octave up") })

        root.addView(actionsScroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val performanceArea = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(8))
        }

        val displayPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(SURFACE)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        displayPanel.addView(TextView(this).apply {
            text = "PERFORMANCE DISPLAY"
            setTextColor(TEXT_SECONDARY)
            textSize = 11f
        })

        voiceValue = textRow("Voice", voicePresets[voiceIndex])
        styleValue = textRow("Style", stylePresets[styleIndex])
        modeValue = textRow("Modes", "Split Off · Layer Off · Sustain Off")
        transposeValue = textRow("Transpose", "+0 semitones")
        noteValue = textRow("Keys", "Ready")
        statusValue = textRow("Status", "Landscape layout loaded")

        displayPanel.addView(voiceValue)
        displayPanel.addView(styleValue)
        displayPanel.addView(modeValue)
        displayPanel.addView(transposeValue)
        displayPanel.addView(noteValue)
        displayPanel.addView(statusValue)

        val quickPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(SURFACE)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            gravity = Gravity.CENTER_HORIZONTAL
        }
        quickPanel.addView(controlButton("SPLIT") { splitEnabled = !splitEnabled; refreshDisplay("Split toggled") })
        quickPanel.addView(controlButton("LAYER") { layerEnabled = !layerEnabled; refreshDisplay("Layer toggled") })
        quickPanel.addView(controlButton("SUSTAIN") { sustainEnabled = !sustainEnabled; refreshDisplay("Sustain toggled") })
        quickPanel.addView(controlButton("MIXER") { refreshDisplay("Mixer opened") })
        quickPanel.addView(controlButton("REGISTRY") { refreshDisplay("Registration bank ready") })

        val displayParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
            marginEnd = dp(10)
        }
        val quickParams = LinearLayout.LayoutParams(dp(170), LinearLayout.LayoutParams.MATCH_PARENT)
        performanceArea.addView(displayPanel, displayParams)
        performanceArea.addView(quickPanel, quickParams)
        root.addView(performanceArea, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        val keyboardScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, dp(8), 0, 0)
        }
        val keyboardView = KeyboardSurfaceView(
            context = this,
            engine = engine,
            onNotesChanged = { labels -> noteValue.text = buildLabel("Keys", labels.ifBlank { "Ready" }) },
        ).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        keyboardScroll.addView(keyboardView)
        root.addView(keyboardScroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(250)))

        return root
    }

    private fun controlButton(label: String, onClick: () -> Unit): MaterialButton {
        return MaterialButton(this).apply {
            text = label
            setTextColor(TEXT_PRIMARY)
            cornerRadius = dp(16)
            isAllCaps = false
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(PRIMARY)
            backgroundTintList = ColorStateList.valueOf(PRIMARY)
            setOnClickListener { onClick() }
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(36)).apply {
                marginEnd = dp(8)
            }
            layoutParams = params
        }
    }

    private fun textRow(label: String, value: String): TextView {
        return TextView(this).apply {
            text = buildLabel(label, value)
            setTextColor(TEXT_PRIMARY)
            textSize = 14f
            setPadding(0, dp(4), 0, dp(4))
            typeface = android.graphics.Typeface.MONOSPACE
        }
    }

    private fun buildLabel(label: String, value: String): String = "$label: $value"

    private fun refreshDisplay(status: String) {
        voiceValue.text = buildLabel("Voice", voicePresets[voiceIndex])
        styleValue.text = buildLabel("Style", stylePresets[styleIndex])
        modeValue.text = buildLabel(
            "Modes",
            listOf(
                if (splitEnabled) "Split On" else "Split Off",
                if (layerEnabled) "Layer On" else "Layer Off",
                if (sustainEnabled) "Sustain On" else "Sustain Off",
            ).joinToString(" · "),
        )
        transposeValue.text = buildLabel("Transpose", if (transposeSemitones >= 0) "+$transposeSemitones" else "$transposeSemitones")
        statusValue.text = buildLabel("Status", status)
    }

    private fun cycleVoice() {
        voiceIndex = (voiceIndex + 1) % voicePresets.size
        refreshDisplay("Voice changed")
    }

    private fun cycleStyle() {
        styleIndex = (styleIndex + 1) % stylePresets.size
        refreshDisplay("Style changed")
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private val BG = Color.parseColor("#0B1118")
        private val SURFACE = Color.parseColor("#101923")
        private val PRIMARY = Color.parseColor("#4D8DFF")
        private val PRIMARY_VARIANT = Color.parseColor("#2757C9")
        private val TEXT_PRIMARY = Color.parseColor("#F6FAFF")
        private val TEXT_SECONDARY = Color.parseColor("#B5C1D1")
    }
}
