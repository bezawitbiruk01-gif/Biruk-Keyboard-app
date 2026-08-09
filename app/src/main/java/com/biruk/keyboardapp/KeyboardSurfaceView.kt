package com.biruk.keyboardapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

class KeyboardSurfaceView(
    context: Context,
    private val engine: KeyboardEngine,
    private val onNotesChanged: (String) -> Unit,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    constructor(
        context: Context,
        engine: KeyboardEngine,
        onNotesChanged: (String) -> Unit,
    ) : this(context, engine, onNotesChanged, null)

    private val startMidi = 21
    private val endMidi = 108

    private val whiteNotes = mutableListOf<Int>()
    private val blackNotes = mutableListOf<Int>()
    private val keyRects = mutableMapOf<Int, RectF>()
    private val whiteIndexBefore = IntArray(128)

    private val rawCounts = linkedMapOf<Int, Int>()
    private val pointerToRawNote = mutableMapOf<Int, Int>()
    private val pointerToActualNote = mutableMapOf<Int, Int>()

    private val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = WHITE_KEY
        style = Paint.Style.FILL
    }
    private val whitePressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = WHITE_PRESSED
        style = Paint.Style.FILL
    }
    private val blackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = BLACK_KEY
        style = Paint.Style.FILL
    }
    private val blackPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = BLACK_PRESSED
        style = Paint.Style.FILL
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = OUTLINE
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = KEY_TEXT
        textAlign = Paint.Align.CENTER
        textSize = dp(12f)
    }

    private val whiteKeyWidthPx = dp(44f).toInt()
    private val blackKeyWidthPx = dp(26f).toInt()
    private val blackKeyHeightPx = dp(140f).toInt()
    private val keyboardHeightPx = dp(232f).toInt()

    init {
        var whiteCount = 0
        for (note in startMidi..endMidi) {
            whiteIndexBefore[note] = whiteCount
            if (isWhite(note)) {
                whiteNotes += note
                whiteCount++
            } else {
                blackNotes += note
            }
        }
        setBackgroundColor(Color.TRANSPARENT)
        isClickable = true
        isFocusable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = whiteNotes.size * whiteKeyWidthPx + paddingLeft + paddingRight
        val desiredHeight = keyboardHeightPx + paddingTop + paddingBottom
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildGeometry(max(w, measuredWidth), max(h, measuredHeight))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(BACKGROUND)

        for (note in whiteNotes) {
            val rect = keyRects[note] ?: continue
            val pressed = rawCounts[note]?.let { it > 0 } == true
            canvas.drawRoundRect(rect, dp(8f), dp(8f), if (pressed) whitePressedPaint else whitePaint)
            canvas.drawRoundRect(rect, dp(8f), dp(8f), outlinePaint)
            if (note % 12 == 0) {
                drawLabel(canvas, rect, midiLabel(note))
            }
        }

        for (note in blackNotes) {
            val rect = keyRects[note] ?: continue
            val pressed = rawCounts[note]?.let { it > 0 } == true
            canvas.drawRoundRect(rect, dp(6f), dp(6f), if (pressed) blackPressedPaint else blackPaint)
            if (pressed) {
                canvas.drawRoundRect(rect, dp(6f), dp(6f), outlinePaint)
            }
            if (note % 12 == 1 || note % 12 == 3 || note % 12 == 6 || note % 12 == 8 || note % 12 == 10) {
                drawLabel(canvas, rect, midiLabel(note), forBlackKey = true)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                handlePointerDown(event, event.actionIndex)
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    updatePointer(event, i)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                handlePointerUp(event, event.actionIndex)
            }
            MotionEvent.ACTION_CANCEL -> {
                clearAllPointers()
            }
        }
        return true
    }

    private fun handlePointerDown(event: MotionEvent, pointerIndex: Int) {
        val pointerId = event.getPointerId(pointerIndex)
        val rawNote = hitTest(event.getX(pointerIndex), event.getY(pointerIndex)) ?: return
        val actualNote = engine.noteOn(rawNote)
        pointerToRawNote[pointerId] = rawNote
        pointerToActualNote[pointerId] = actualNote
        adjustRawCount(rawNote, +1)
        notifyNotes()
        invalidate()
    }

    private fun updatePointer(event: MotionEvent, pointerIndex: Int) {
        val pointerId = event.getPointerId(pointerIndex)
        val currentRaw = hitTest(event.getX(pointerIndex), event.getY(pointerIndex))
        val previousRaw = pointerToRawNote[pointerId]
        if (currentRaw == previousRaw) return

        previousRaw?.let { note ->
            pointerToActualNote[pointerId]?.let(engine::noteOff)
            adjustRawCount(note, -1)
        }

        if (currentRaw != null) {
            val actualNote = engine.noteOn(currentRaw)
            pointerToRawNote[pointerId] = currentRaw
            pointerToActualNote[pointerId] = actualNote
            adjustRawCount(currentRaw, +1)
        } else {
            pointerToRawNote.remove(pointerId)
            pointerToActualNote.remove(pointerId)
        }

        notifyNotes()
        invalidate()
    }

    private fun handlePointerUp(event: MotionEvent, pointerIndex: Int) {
        val pointerId = event.getPointerId(pointerIndex)
        pointerToRawNote.remove(pointerId)?.let { rawNote ->
            adjustRawCount(rawNote, -1)
        }
        pointerToActualNote.remove(pointerId)?.let(engine::noteOff)
        notifyNotes()
        invalidate()
    }

    private fun clearAllPointers() {
        pointerToRawNote.values.forEach { rawNote ->
            adjustRawCount(rawNote, -1)
        }
        pointerToActualNote.values.forEach(engine::noteOff)
        pointerToRawNote.clear()
        pointerToActualNote.clear()
        notifyNotes()
        invalidate()
    }

    private fun adjustRawCount(note: Int, delta: Int) {
        val newCount = (rawCounts[note] ?: 0) + delta
        if (newCount <= 0) rawCounts.remove(note) else rawCounts[note] = newCount
    }

    private fun hitTest(x: Float, y: Float): Int? {
        blackNotes.forEach { note ->
            val rect = keyRects[note]
            if (rect != null && rect.contains(x, y)) return note
        }
        whiteNotes.forEach { note ->
            val rect = keyRects[note]
            if (rect != null && rect.contains(x, y)) return note
        }
        return null
    }

    private fun rebuildGeometry(width: Int, height: Int) {
        keyRects.clear()
        val availableHeight = height - paddingTop - paddingBottom
        val whiteHeight = availableHeight.toFloat()
        val blackHeight = blackKeyHeightPx.coerceAtMost(availableHeight).toFloat()
        val top = paddingTop.toFloat()

        for (note in whiteNotes) {
            val whiteIndex = whiteIndexBefore[note]
            val left = paddingLeft + (whiteIndex * whiteKeyWidthPx)
            keyRects[note] = RectF(
                left.toFloat(),
                top,
                (left + whiteKeyWidthPx).toFloat(),
                top + whiteHeight,
            )
        }

        for (note in blackNotes) {
            val whiteIndex = whiteIndexBefore[note]
            val left = paddingLeft + (whiteIndex * whiteKeyWidthPx) - (blackKeyWidthPx / 2)
            keyRects[note] = RectF(
                left.toFloat(),
                top,
                (left + blackKeyWidthPx).toFloat(),
                top + blackHeight,
            )
        }
    }

    private fun drawLabel(canvas: Canvas, rect: RectF, label: String, forBlackKey: Boolean = false) {
        val baseline = if (forBlackKey) rect.bottom - dp(14f) else rect.bottom - dp(12f)
        canvas.drawText(label, rect.centerX(), baseline, textPaint)
    }

    private fun notifyNotes() {
        val notes = rawCounts.keys.sorted().joinToString(" · ") { midiLabel(it) }
        onNotesChanged(notes)
    }

    private fun isWhite(note: Int): Boolean {
        val pitchClass = note % 12
        return pitchClass == 0 || pitchClass == 2 || pitchClass == 4 || pitchClass == 5 || pitchClass == 7 || pitchClass == 9 || pitchClass == 11
    }

    private fun midiLabel(midiNote: Int): String {
        val pitchNames = listOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
        return "${pitchNames[midiNote % 12]}${midiNote / 12 - 1}"
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    companion object {
        private val BACKGROUND = Color.parseColor("#141C26")
        private val WHITE_KEY = Color.parseColor("#F5F7FB")
        private val WHITE_PRESSED = Color.parseColor("#CFE0FF")
        private val BLACK_KEY = Color.parseColor("#1A1F26")
        private val BLACK_PRESSED = Color.parseColor("#4D8DFF")
        private val OUTLINE = Color.parseColor("#1A2431")
        private val KEY_TEXT = Color.parseColor("#111722")
    }
}
