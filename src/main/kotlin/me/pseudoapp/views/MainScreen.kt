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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp
import me.pseudoapp.Element
import me.pseudoapp.View
import me.pseudoapp.other.*
import me.pseudoapp.views.prompts.PromptImageItem
import me.pseudoapp.views.prompts.PromptItem
import me.pseudoapp.views.prompts.PromptListItem
import me.pseudoapp.views.scrollbar.LazyListScrollbarHost

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen() {
    var selectedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var layoutRect by remember { mutableStateOf<Rect>(Rect()) }
    val elements = remember { mutableStateListOf<Element>() }
    val rows = remember { mutableStateListOf<Element>() }
    val boxes = remember { mutableStateListOf<Element>() }
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

        fun updateInnerRows(source: List<Element>) {
            for (element in source) {
                val sortedRows = sortedRows(source)
                for (it in sortedRows) {
                    if (it.size < 2) {
                        continue
                    }

                    val start = it.first().area.topLeft
                    val end = it.last().area.bottomRight

                    val minY = it.minBy { it.area.topLeft.y }.area.topLeft.y
                    val maxY = it.maxBy { it.area.bottomRight.y }.area.bottomRight.y
                    println("debug: add1")
                    rows.add(
                        Element(
                            area = Rect(start.copy(y = minY), end.copy(y = maxY)),
                            color = nextColor(),
                            type = Element.Type.Row,
                            prompt = mutableStateOf(""),
                            inner = mutableListOf()
                        )
                    )
                }
            }



            source.forEach {
                val inner = it.inner(elements)
                it.inner = inner
                println(inner)
                updateInnerRows(inner)
            }
        }

        fun updateAllBoxes() {
            fun addBoxes(rows: List<List<Element>>) {
                rows.forEachIndexed { i, row ->
                    for (j in row.indices) {
                        if (j > 0 && isBox(row[j - 1], row[j])) {
                            val inner = mutableListOf(row[j - 1], row[j])
                            val minX = inner.minBy { it.area.topLeft.x }.area.topLeft.x
                            val minY = inner.minBy { it.area.topLeft.y }.area.topLeft.y
                            val maxX = inner.maxBy { it.area.bottomRight.x }.area.bottomRight.x
                            val maxY = inner.maxBy { it.area.bottomRight.y }.area.bottomRight.y

//                            elements.forEach {
//                                it.inner.removeAll(inner)
//                            }
                            println("debug: add2")
                            val boxElement = Element(
                                area = Rect(
                                    Offset(minX, minY),
                                    Offset(maxX, maxY)
                                ),
                                color = Color.LightGray,
                                type = Element.Type.Box,
                                prompt = mutableStateOf(""),
                                inner = inner
                            )
//                            println("Add New Box: ${boxElement}")
//                            elements.filter { it.area.contains(boxElement.area) }.minBy {
//                                it.area.topLeft.x - boxElement.area.topLeft.x
//                            }.inner.add(boxElement)
                            boxes.add(boxElement)
                            println("NewBox: ${boxElement}")
                        }

                        addBoxes(
                            sortedRows(row[j].inner)
                        )
                    }
                }
            }

            val lines = sortedRows(elements.first().inner)
            println("lines: ${lines}")
            addBoxes(lines)
        }

        fun updateAllRows() {
            val element = elements.first()
            val inner = element.inner(elements)
            element.inner = inner
            updateInnerRows(inner)
        }

        Row {
            LayoutView(
                selectedImage,
                elements,
                rows,
                boxes,
                onNewGoal = { element ->
                    selectedColor = nextColor()
                    elements.add(element)

                    updated.value = Unit
                    requester.requestFocus()

                    rows.clear()
                    boxes.clear()
                    updateAllRows()
                    updateAllBoxes()
                },
                modifier = Modifier.weight(1f)
                    .padding(6.dp)
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

                        if (elements.isEmpty()) {
                            println("debug: add0")
                            elements.add(
                                Element(
                                    area = layoutRect,
                                    type = Element.Type.Column,
                                    prompt = mutableStateOf("Создать Compose экран с элементами:"),
                                    color = Color.LightGray,
                                    inner = mutableListOf()
                                )
                            )
                            updated.value = Unit
                        } else {
                            elements.first().area = layoutRect
                        }
                    }
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
                                Unit
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

