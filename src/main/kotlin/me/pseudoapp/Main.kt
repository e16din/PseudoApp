package me.pseudoapp

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import me.pseudoapp.views.MainScreen


// NOTE: Работа с тем чего нет - источник ошибок при разработке приложения
// если работать с настоящим, а не воображаемым то кол-во ошибок снизится
// чем меньше воображаемых абстракций - тем меньше ошибок,
// абстракции должны однозначно соответствовать реальности,
// но это не возможно так как абстракция это одна часть реальности,
// а то на что указывает абстракция - другая
// поэтому ошибки неизбежны

// и каждый интерпретатор воспринимает абстракции по своему

// количество ошибок можно снизить если не работать с фактическими результатами, а не воображаемыми

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Preview
fun App() {
    windowSize = LocalWindowInfo.current.containerSize
    MaterialTheme {
        MainScreen()
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}

var windowSize = IntSize.Zero
var layoutRect = Rect.Zero
var currentColor = RainbowColor.Violet

enum class RainbowColor(val color: Color) {
    Red(Color(0xFFF44336)),
    Orange(Color(0xFFFF9800)),
    Yellow(Color(0xFFFFEB3B)),
    Green(Color(0xFF4CAF50)),
    Blue(Color(0xFF2196F3)),
    Indigo(Color(0xFF3F51B5)),
    Violet(Color(0xFF9C27B0)),
}

var colorPosition = 0

fun nextColor(): RainbowColor {
    return RainbowColor.entries[colorPosition].apply {
        colorPosition += 1
        if (colorPosition == RainbowColor.entries.size) {
            colorPosition = 0
        }
    }
}