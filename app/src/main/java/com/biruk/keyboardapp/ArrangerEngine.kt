package com.biruk.keyboardapp

/**
 * State machine for the arranger transport layer.
 *
 * This deliberately keeps timing/audio pattern playback separate from the UI.
 * The next stage can attach MIDI/style pattern tracks without changing the
 * console controls.
 */
class ArrangerEngine {
    enum class Section {
        STOPPED,
        INTRO,
        MAIN_A,
        MAIN_B,
        MAIN_C,
        MAIN_D,
        ENDING,
    }

    data class State(
        val running: Boolean = false,
        val section: Section = Section.STOPPED,
        val syncStart: Boolean = false,
        val fillPending: Boolean = false,
    )

    private var state = State()
    private var listener: ((State) -> Unit)? = null

    fun setListener(listener: ((State) -> Unit)?) {
        this.listener = listener
        listener?.invoke(state)
    }

    fun snapshot(): State = state

    fun start() {
        state = state.copy(running = true, section = Section.MAIN_A, fillPending = false)
        publish()
    }

    fun stop() {
        state = state.copy(running = false, section = Section.STOPPED, fillPending = false)
        publish()
    }

    fun intro() {
        state = state.copy(running = true, section = Section.INTRO, fillPending = false)
        publish()
    }

    fun main(section: Section) {
        val selected = when (section) {
            Section.MAIN_A, Section.MAIN_B, Section.MAIN_C, Section.MAIN_D -> section
            else -> Section.MAIN_A
        }
        state = state.copy(running = true, section = selected, fillPending = false)
        publish()
    }

    fun fill() {
        if (!state.running) {
            state = state.copy(fillPending = true)
        } else {
            state = state.copy(fillPending = true)
        }
        publish()
    }

    fun ending() {
        state = state.copy(running = true, section = Section.ENDING, fillPending = false)
        publish()
    }

    fun toggleSyncStart(): Boolean {
        state = state.copy(syncStart = !state.syncStart)
        publish()
        return state.syncStart
    }

    fun clearFill() {
        if (state.fillPending) {
            state = state.copy(fillPending = false)
            publish()
        }
    }

    private fun publish() {
        listener?.invoke(state)
    }
}
