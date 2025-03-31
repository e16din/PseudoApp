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
import me.pseudoapp.other.Rect
import me.pseudoapp.other.SizeObserver
import me.pseudoapp.other.contains
import me.pseudoapp.other.convertToPx
import me.pseudoapp.views.MainScreen

// NOTE: Работа с тем чего нет - источник ошибок при разработке приложения
// если работать с настоящим а не воображаемым то кол-во ошибок снизится
// чем меньше воображаемых абстракций - тем меньше ошибок,
// абстрации должны однозначно соответствовать реальности,
// но это не возможно так как абстракция это одно часть реальности,
// а то на что указывает абстракция - другая
// поэтому ошибки неизбежны

// и каждый интерпретатор воспринимает абстракции по своему

// количество ошибок можно снизить если не работать с фактическими результатами а не воображаемыми

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

val rootElement = Element(
    area = Rect(
        Offset(0f, 0f),
        Offset(
            Float.MAX_VALUE,
            Float.MAX_VALUE
        )
    ),
    type = Element.Type.Column,
    prompt = mutableStateOf("Создать Compose экран с элементами:"),
    color = Color.LightGray,
    inner = mutableListOf()
)

fun List<Element>.findContainerOf(
    element: Element
): Element? {
    val filtered = this.filter { it.area.contains(element.area) && it.isContainer() }
    return if (filtered.isEmpty()) {
        null
    } else {
        filtered.minBy {
            it.area.topLeft.x - element.area.topLeft.x
        }
    }
}

fun Element.findInner(elements: List<Element>, all: Boolean = false): MutableList<Element> {
    val result = mutableListOf<Element>()
    elements.forEach { el ->
        if (this.area.contains(el.area)) {
            result.add(el)
        }
    }
    if (!all) {
        var i = 0
        while (i < result.size - 1) {
            val element = result[i]
            result.removeAll(element.findInner(result))
            i++
        }
    }
    return result
}

data class Element(
    var area: Rect,
    val color: Color,
    val type: Type,
    val prompt: MutableState<String>,
    var inner: MutableList<Element>
) {
    fun isContainer(): Boolean {
        return when (type) {
            Type.Text,
            Type.TextField,
            Type.Icon,
            Type.Coil -> false

            Type.Button,
            Type.Box,
            Type.Row,
            Type.Column -> true
        }
    }

    enum class Type {
        Text,
        TextField,
        Icon,
        Coil,
        Button,
        Box,
        Row,
        Column
    }
}


data class View(val name: String, val designImagePath: String? = null)
