package me.pseudoapp.views


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import me.pseudoapp.Element
import me.pseudoapp.Instruction
import me.pseudoapp.other.pickImage


@Composable
fun MainScreen() {
    var selectedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    val elements = remember { mutableStateListOf<Element>() }
    val instructions = remember { mutableStateListOf<Instruction>() }
//    val undoElements = remember { mutableStateListOf<Element>() }
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

        Row {
            Box(
                Modifier.weight(1f)
                    .padding(12.dp)
            ) {
                ElementsView(
                    ctrlPressed,
                    shiftPressed,
                    selectedImage,
                    elements,
                    onNewElement = { element ->
                        newElement.value = element
                    },
                    modifier = Modifier
                )
            }

            Card(
                Modifier
                    .padding( 12.dp)
                    .width(420.dp)
                ,
                border = BorderStroke(2.dp, Color.LightGray),
//                backgroundColor = Color.Black
            ) {
                InstructionsLineView(instructions)
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


