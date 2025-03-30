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
