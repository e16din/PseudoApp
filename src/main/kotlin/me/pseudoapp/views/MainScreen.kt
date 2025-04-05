package me.pseudoapp.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import me.pseudoapp.Element
import me.pseudoapp.View
import me.pseudoapp.other.createContainerCode
import me.pseudoapp.other.pickImage
import me.pseudoapp.rootElement

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen() {
    var selectedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    val elements = remember { mutableStateListOf(rootElement) }
    val undoElements = remember { mutableStateListOf<Element>() }

    val requester = remember { FocusRequester() }
    val selectedView = remember { View("MainView") }

    Column(

        Modifier.onKeyEvent { keyEvent ->
            if (keyEvent.key == Key.Z
                && keyEvent.isShiftPressed
                && keyEvent.isCtrlPressed
                && keyEvent.type == KeyEventType.KeyUp
            ) {
                println("Ctrl + Shifrt + Z")
//                if (undoElements.isNotEmpty()) {
//                    elements.add(undoElements.last())
//                    updated.value = Unit
//                    undoElements.removeAt(undoElements.size - 1)
                      // todo: update rootElement
//                }
                return@onKeyEvent true

            } else if (keyEvent.key == Key.Z
                && keyEvent.isCtrlPressed
                && keyEvent.type == KeyEventType.KeyUp
            ) {
                println("Ctrl + Z")
                if (elements.isNotEmpty()) {
                    val last = elements.last()
//                    undoElements.add(removed)
                    elements.remove(last)
                    rootElement.removeInner(last)
                }
                return@onKeyEvent true
            }
            false
        }
            .focusRequester(requester)
            .focusable()
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

        Row {
            LayoutView(
                selectedImage,
                elements,
                onNewElement = { element ->
                    elements.add(element)
                    requester.requestFocus()
                },
                modifier = Modifier.weight(1f)
                    .padding(6.dp)
            )

            Card {
                ResultView(
                    createContainerCode(elements, selectedView.name)
                )
            }


        }
    }

    LaunchedEffect(Unit) {
        requester.requestFocus()
    }
}

