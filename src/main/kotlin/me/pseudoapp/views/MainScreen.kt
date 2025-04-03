package me.pseudoapp.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import me.pseudoapp.other.copyToClipboard
import me.pseudoapp.other.createContainerCode
import me.pseudoapp.other.pickImage
import me.pseudoapp.rootElement
import me.pseudoapp.views.prompts.PromptImageItem
import me.pseudoapp.views.prompts.PromptItem
import me.pseudoapp.views.prompts.PromptListItem
import me.pseudoapp.views.scrollbar.LazyListScrollbarHost

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen() {
    var selectedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    val elements = remember { mutableStateListOf(rootElement) }
    val updated = remember { mutableStateOf(Unit) }
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
                if (undoElements.isNotEmpty()) {
                    elements.add(undoElements.last())
                    updated.value = Unit
                    undoElements.removeAt(undoElements.size - 1)
                }
                return@onKeyEvent true

            } else if (keyEvent.key == Key.Z
                && keyEvent.isCtrlPressed
                && keyEvent.type == KeyEventType.KeyUp
            ) {
                println("Ctrl + Z")
                if (elements.isNotEmpty()) {
                    undoElements.add(elements.last())
                    elements.removeAt(elements.size - 1)
                    updated.value = Unit
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

                    updated.value = Unit
                    requester.requestFocus()
                },
                modifier = Modifier.weight(1f)
                    .padding(6.dp)
            )
            Card {

                LazyListScrollbarHost(
                    modifier = Modifier
                        .width(320.dp)
                        .padding(16.dp)
                ) { lazyListState ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        state = lazyListState
                    ) {
                        stickyHeader {
                            Button(onClick = {
                                var finalPrompt = "${elements.first()}\n"
                                elements.forEach {
                                    finalPrompt += "-${it.prompt};\n"
                                }
                                copyToClipboard(finalPrompt)

                            }) {
                                Text("Копировать результат")
                            }

                        }
                        items(elements.size) { i ->
                            val goal = elements[i]
                            val onRemoveClick = {
                                elements.removeAt(i)
                                updated.value = Unit
                            }
                            val onPromptChanged: (String) -> Unit = { value ->
                                requester.freeFocus()
                            }
                            when (goal.type) {
                                Element.Type.Coil -> PromptImageItem(
                                    goal,
                                    onPromptChanged,
                                    onRemoveClick,
                                    canRemove = i != 0
                                )

                                Element.Type.Column -> PromptListItem(
                                    goal,
                                    onPromptChanged,
                                    onRemoveClick,
                                    canRemove = i != 0
                                )

                                else -> PromptItem(goal, onPromptChanged, onRemoveClick, canRemove = i != 0)
                            }
                        }
                    }
                }
            }

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

