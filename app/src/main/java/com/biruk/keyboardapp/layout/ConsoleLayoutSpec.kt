package com.biruk.keyboardapp.layout

import com.biruk.keyboardapp.model.KeyboardConsoleSection
import com.biruk.keyboardapp.model.defaultConsoleSections

data class ConsoleLayoutSpec(
    val orientation: String = "landscape",
    val topBarHeightDp: Int = 76,
    val workspaceFlex: Float = 1f,
    val keyboardStripHeightDp: Int = 260,
    val sections: List<KeyboardConsoleSection> = defaultConsoleSections(),
)

fun defaultConsoleLayoutSpec() = ConsoleLayoutSpec()
