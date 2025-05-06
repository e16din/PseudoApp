package me.pseudoapp

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import me.pseudoapp.other.SizeObserver
import me.pseudoapp.other.convertToPx
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

@Composable
@Preview
fun App() {
    SizeObserver()
    println("px: ${1f.convertToPx()}")
    println("DP: ${1.dp.value}")
    println("DP to px: ${1.dp.value.convertToPx()}")
    MaterialTheme {
        MainScreen()
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}

