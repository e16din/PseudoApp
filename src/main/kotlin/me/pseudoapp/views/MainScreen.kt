package me.pseudoapp.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp
import me.pseudoapp.Element
import me.pseudoapp.Goal
import me.pseudoapp.other.Rect
import me.pseudoapp.other.copyToClipboard
import me.pseudoapp.other.pickImage
import me.pseudoapp.views.prompts.PromptImageItem
import me.pseudoapp.views.prompts.PromptItem
import me.pseudoapp.views.prompts.PromptListItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen() {
    var selectedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var layoutRect by remember { mutableStateOf<Rect>(Rect()) }
    val goals = remember { mutableStateListOf<Goal>() }
    val updated = remember { mutableStateOf(Unit) }
    val undoGoals = remember { mutableStateListOf<Goal>() }

    val requester = remember { FocusRequester() }

    Column(

        Modifier.onKeyEvent { keyEvent ->
            if (keyEvent.key == Key.Z
                && keyEvent.isShiftPressed
                && keyEvent.isCtrlPressed
                && keyEvent.type == KeyEventType.KeyUp
            ) {
                println("Ctrl + Shifrt + Z")
                if (undoGoals.isNotEmpty()) {
                    goals.add(undoGoals.last())
                    updated.value = Unit
                    undoGoals.removeAt(undoGoals.size - 1)
                }
                return@onKeyEvent true

            } else if (keyEvent.key == Key.Z
                && keyEvent.isCtrlPressed
                && keyEvent.type == KeyEventType.KeyUp
            ) {
                println("Ctrl + Z")
                if (goals.isNotEmpty()) {
                    undoGoals.add(goals.last())
                    goals.removeAt(goals.size - 1)
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
                goals,
                onNewGoal = { goal ->
                    selectedColor = nextColor()
                    goals.add(goal)
                    updated.value = Unit
                    requester.requestFocus()
                },
                modifier = Modifier.weight(1f)
                    .onGloballyPositioned { layoutCoordinates ->
                        val size = layoutCoordinates.size
                        val position = layoutCoordinates.positionInParent()

                        layoutRect = Rect(
                            Offset(position.x, position.y),
                            Offset(
                                position.x + size.width.toFloat(),
                                position.y + size.height.toFloat()
                            )
                        )

                        if (goals.isEmpty()) {
                            goals.add(
                                Goal(
                                    area = layoutRect,
                                    element = Element(Element.Type.Screen),
                                    prompt = mutableStateOf("Создать Compose экран с элементами:"),
                                    color = Color.Transparent
                                )
                            )
                            updated.value = Unit
                        } else {
                            goals.first().area = layoutRect
                        }
                    }
            )
            Card {
                LazyColumn(modifier = Modifier.width(320.dp).padding(16.dp)) {
                    stickyHeader {
                        Button(onClick = {
                            var finalPrompt = "${goals.first()}\n"
                            goals.drop(1).forEach {
                                finalPrompt += "-${it.prompt};\n"
                            }
                            copyToClipboard(finalPrompt)

                        }) {
                            Text("Копировать результат")
                        }

                    }
                    items(goals.size) { i ->
                        val goal = goals[i]
                        val onRemoveClick = {
                            goals.removeAt(i)
                            updated.value = Unit
                            Unit
                        }
                        val onPromptChanged: (String) -> Unit = { value ->
                            requester.freeFocus()
                        }
                        when (goal.element.type) {
                            Element.Type.Image -> PromptImageItem(
                                goal,
                                onPromptChanged,
                                onRemoveClick,
                                canRemove = i != 0
                            )

                            Element.Type.List -> PromptListItem(
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

                Card {
                    ResultView(createScreenCode(goals))
                }


        }
    }

    LaunchedEffect(Unit) {
        requester.requestFocus()
    }
}

