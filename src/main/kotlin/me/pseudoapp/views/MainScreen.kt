package me.pseudoapp.views


import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import me.pseudoapp.Element
import me.pseudoapp.other.pickImage


@Composable
fun MainScreen() {

    var selectedImage by remember { mutableStateOf<ImageBitmap?>(null) }

    val keyboardRequester = remember { FocusRequester() }

    val ctrlPressed = remember { mutableStateOf(false) }
    val shiftPressed = remember { mutableStateOf(false) }
    val newElement = remember { mutableStateOf<Element?>(null) }


    LaunchedEffect(Unit) {
        keyboardRequester.requestFocus()
    }

    Column(
        Modifier
            .focusable()
            .focusRequester(keyboardRequester)
            .onKeyEvent { keyEvent ->
                ctrlPressed.value = keyEvent.isCtrlPressed
                        && keyEvent.type == KeyEventType.KeyDown

                shiftPressed.value = keyEvent.isShiftPressed
                        && keyEvent.type == KeyEventType.KeyDown

                return@onKeyEvent true
            }
    ) {
        Button(onClick = {
            pickImage {
                it?.let {
                    selectedImage = it
                }
            }

        }) {
            Text("Выбрать изображение")
        }

        val rootElement = remember {
            Element(
                name = mutableStateOf("App"),
//                condition = mutableStateOf(""),
                action = mutableStateOf(""),
                value = mutableStateOf(""),
                area = Rect(
                    topLeft = Offset.Zero,
                    bottomRight = Offset.Zero
                ),
                color = Color.White
            )
        }
        var selectedElement by remember { mutableStateOf(rootElement) }
        val diveElements = remember { mutableStateListOf<Element>(rootElement) }
        Row {
            Column {
                Row {
                    for (it in diveElements) {
                        Button(
                            onClick = {
                                selectedElement = it
                                var selected = false

                                val removed = mutableListOf<Element>()
                                for (it in diveElements) {
                                    if (!selected && it.name == selectedElement.name) {
                                        selected = true
                                        continue
                                    }
                                    if (selected) {
                                        removed.add(it)
                                    }
                                }
                                diveElements.removeAll(removed)
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (it.name == selectedElement.name) MaterialTheme.colors.primary.copy(
                                    alpha = 0.62f
                                ) else MaterialTheme.colors.primary
                            ),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(it.name.value)
                        }
                    }
                }
                Box(
                    Modifier.weight(1f)
                        .padding(12.dp)
                ) {
                    ElementsView(
                        keyboardRequester,
                        ctrlPressed,
                        shiftPressed,
                        selectedImage,
                        onNewElement = { element ->
                            newElement.value = element
                        },
                        onDiveInClick = {
                            selectedElement = it
                            diveElements.add(selectedElement)
                        },
                        contentElement = selectedElement,
                        modifier = Modifier
                    )
                }
            }

//            Card(
//                Modifier.weight(1f)
//                    .padding(12.dp)
//            ) {
//                CodeEditorView(
//                    elements,
//                    newElement
//                )
//            }
        }
    }
}


