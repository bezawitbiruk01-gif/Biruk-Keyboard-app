package com.biruk.keyboardapp

/**
 * UI-independent arranger transport state machine.
 * Pattern/MIDI playback can be attached here later without coupling it to the console UI.
 */
class ArrangerEngine {
    enum class Section { STOPPED, INTRO, MAIN_A, MAIN_B, MAIN_C, MAIN_D, ENDING }

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

    fun restore(restored: State) {
        state = restored
        publish()
    }

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
        state = state.copy(fillPending = true)
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

    private fun publish() = listener?.invoke(state)
}
