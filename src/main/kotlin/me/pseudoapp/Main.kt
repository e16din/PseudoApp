package me.pseudoapp

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import me.pseudoapp.other.Rect
import me.pseudoapp.other.SizeObserver
import me.pseudoapp.views.MainScreen


@Composable
@Preview
fun App() {
    SizeObserver()

    MaterialTheme {
        MainScreen()
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}

data class Goal(var area: Rect, val color:Color, val element: Element, val prompt: MutableState<String>)

data class Element(val type: Type, val tag: String? = null) {
    enum class Type {
        Screen,
        CustomView,
        Text,
        Button,
        TextField,
        Image, // Icon || Coil
        List, // Lazy || Constant & Horizontal || Vertical /TODO: сразу квадрат итема добавлять, и перетаскивать за лейбл, по alt показывать крестики удаления
    }
}

// task:
// if (goal in CustomView) print goal in CustomView body
// else if (goal is basic view && goal in other basic view rect) print Box, print otherView, print goal in Box
// else if(goal is basic view && goal in other container view rect) print goal in Container View
// else print Goal in Screen
