package com.biruk.keyboardapp.model

data class KeyboardUiState(
    val title: String = "BIRUK MUSIC KEYBOARD",
    val subtitle: String = "Landscape-only performance console · offline synth · arranger workflow",
    val voiceIndex: Int = 0,
    val styleIndex: Int = 0,
    val registrationIndex: Int = 0,
    val splitEnabled: Boolean = false,
    val layerEnabled: Boolean = false,
    val sustainEnabled: Boolean = false,
    val transposeSemitones: Int = 0,
    val tempo: Int = 120,
    val mixerBalance: Int = 50,
    val effectDepth: Int = 35,
    val statusText: String = "Ready",
)

data class KeyboardConsoleSection(
    val id: String,
    val title: String,
    val accentHint: String,
)

data class KeyboardBank(
    val voices: List<String>,
    val styles: List<String>,
    val registrations: List<String>,
)

fun defaultKeyboardBank() = KeyboardBank(
    voices = listOf(
        "Grand Piano",
        "Bright Piano",
        "Electric Piano",
        "Stage Organ",
        "Warm Strings",
        "Brass Section",
        "Synth Pad",
        "Acoustic Bass",
    ),
    styles = listOf(
        "Pop Ballad",
        "Dance Pop",
        "Latin Groove",
        "Waltz",
        "Rock Band",
        "Funk Session",
        "Afro Groove",
        "House Beat",
    ),
    registrations = listOf(
        "Intro Setup",
        "Ballad Flow",
        "Dance Lead",
        "Live Band",
        "Church Pad",
        "Strings Layer",
        "Piano Split",
        "Finale",
    ),
)

fun defaultConsoleSections() = listOf(
    KeyboardConsoleSection("voice", "VOICE BANK", "Browse timbres"),
    KeyboardConsoleSection("display", "PERFORMANCE DISPLAY", "Track status"),
    KeyboardConsoleSection("performance", "PERFORMANCE", "Play controls"),
)
