package me.pseudoapp.views

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
import me.pseudoapp.other.pickImage


@Composable
fun MainScreen() {
    var selectedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    val elements = remember { mutableStateListOf<Element>() }
//    val undoElements = remember { mutableStateListOf<Element>() }
    val requester = remember { FocusRequester() }
    val ctrlPressed = remember { mutableStateOf(false) }
    val shiftPressed = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        requester.requestFocus()
    }

    Column(
        Modifier
            .focusable()
            .focusRequester(requester)
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

        Row {
            Box(
                Modifier.weight(1f)
                    .padding(12.dp)
            ) {
                LayoutView(
                    ctrlPressed,
                    shiftPressed,
                    selectedImage,
                    elements,
                    onNewElement = { element ->

                    },
                    modifier = Modifier
                )
            }

            Card(Modifier.weight(1f)) {
                Column(Modifier.fillMaxSize()) {

                }
            }


        }
    }

}

