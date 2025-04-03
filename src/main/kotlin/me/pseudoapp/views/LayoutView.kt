package me.pseudoapp.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.pseudoapp.Element
import me.pseudoapp.findContainerRectOf
import me.pseudoapp.other.Rect
import me.pseudoapp.other.convertToPx
import me.pseudoapp.other.measureTextWidth
import me.pseudoapp.other.rectOf
import me.pseudoapp.rootElement

@Composable
fun LayoutView(
    selectedImage: ImageBitmap?,
    elements: SnapshotStateList<Element>,
    onNewElement: (Element) -> Unit,
    modifier: Modifier
) {

    var startPoint by remember { mutableStateOf<Offset?>(null) }
    var endPoint by remember { mutableStateOf<Offset?>(null) }
    val dragRect = remember { Rect(Offset.Zero, Offset.Zero) }

    var elementsMenuExpanded by remember { mutableStateOf(false) }


    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    startPoint = offset
                    println("onDragStart")
                    println("$startPoint")
                },
                onDrag = { change, dragAmount ->
                    change.consume()

                    startPoint?.let { start ->
                        endPoint = if (endPoint == null) {
                            start + dragAmount
                        } else {
                            endPoint!! + dragAmount
                        }
                    }
                },
                onDragEnd = {
                    elementsMenuExpanded = true
                }
            )
        }) {
            selectedImage?.let {
                drawImage(it)
            }

            endPoint?.let {
                dragRect.apply {
                    topLeft = startPoint!!
                    bottomRight = endPoint!!
                }
                drawRect(
                    color = selectedColor.copy(alpha = 0.3f),
                    topLeft = dragRect.topLeft,
                    size = dragRect.size
                )
            }

            fun drawElementAndInner(element: Element) {
                val isContainer = element.isContainer()
                drawRect(
                    color = element.color,
                    topLeft = element.area.topLeft,
                    size = element.area.size,
                    style = Stroke(
                        width = if (isContainer) 1f else borderWidth.dp.toPx(),
                        pathEffect = if (isContainer)
                            PathEffect.dashPathEffect(floatArrayOf(10f, 5f), phase = 0f)
                        else
                            null
                    ),

                    )

                element.inner.forEach {
                    drawElementAndInner(it)
                }
            }

            drawElementAndInner(rootElement)
        }

        elements.forEach { element ->
            val textStyle = TextStyle.Default.copy(fontSize = 8.sp)
            Box(
                modifier = Modifier
                    .offset(
                        x = element.area.bottomRight.x.convertToPx().dp - (measureTextWidth(
                            element.type.name,
                            textStyle
                        ) / 2),
                        y = element.area.bottomRight.y.convertToPx().dp
                    )
                    .clip(CircleShape)
                    .background(Color.Green)
            ) {
                Text(
                    style = textStyle,
                    text = element.type.name,
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        fun selectElement(it: Element.Type, about: String) {
            val finalRect = Rect(startPoint!!, endPoint!!)

            val newElement = Element(
                area = finalRect,
                type = it,
                prompt = mutableStateOf(about),
                color = selectedColor,
                inner = mutableListOf()
            )

            val container = elements.findContainerRectOf(newElement)!!
//            if (container.isContainer()) {
//                container.inner.add(newElement)
//            }
            val containerInner = container.inner
            if (containerInner.isEmpty()) {
                selectedColor = nextColor()
                val newBox = Element(
                    area = rectOf(10, finalRect),
                    type = Element.Type.Box,
                    prompt = mutableStateOf(about),
                    color = selectedColor,
                    inner = mutableListOf()
                )

                selectedColor = nextColor()
                val newColumn = Element(
                    area = rectOf(7, finalRect),
                    type = Element.Type.Column,
                    prompt = mutableStateOf(about),
                    color = selectedColor,
                    inner = mutableListOf()
                )

                selectedColor = nextColor()
                val newRow = Element(
                    area = rectOf(4, finalRect),
                    type = Element.Type.Row,
                    prompt = mutableStateOf(about),
                    color = selectedColor,
                    inner = mutableListOf()
                )

                container.inner.add(newBox)
                onNewElement(newBox)

                newBox.inner.add(newColumn)
                onNewElement(newColumn)

                newColumn.inner.add(newRow)
                onNewElement(newRow)

                newRow.inner.add((newElement))
                onNewElement(newElement)

            } else {
                for (box in containerInner) {
                    val column = box.inner.first()
                    val row = box.inner.first().inner.first()

                    for (e in row.inner) {
                        if (newElement.area.topLeft.x > e.area.bottomRight.x
                            && newElement.area.bottomRight.y > e.area.topLeft.y
                            && newElement.area.topLeft.y < e.area.bottomRight.y
                        ) {
                            val rects = row.inner.map { it.area } + newElement.area
                            row.area = rectOf(4, rects)
                            column.area = rectOf(7, rects)
                            box.area = rectOf(10, rects)

                            row.inner.add(newElement)
                            onNewElement(newElement)
                            break

                        } else if (newElement.area.topLeft.y > e.area.bottomRight.y
                            && newElement.area.topLeft.x < e.area.bottomRight.x
                        ) {
                            var handled = false
                            for (r in column.inner) {
                                if (newElement.area.topLeft.x > r.area.bottomRight.x
                                    && newElement.area.bottomRight.y > r.area.topLeft.y
                                    && newElement.area.topLeft.y < r.area.bottomRight.y
                                ) {
                                    handled = true
                                    r.inner.add(newElement)
                                    r.area = rectOf(
                                        4,
                                        r.inner.map { it.area } + newElement.area
                                    )
                                    column.area = rectOf(
                                        7,
                                        column.inner.map { it.area } + newElement.area
                                    )
                                    box.area = rectOf(
                                        10,
                                        box.inner.map { it.area } + newElement.area
                                    )
                                    onNewElement(newElement)
                                    break
                                }
                            }
                            if (handled) {
                                break
                            } else {
                                selectedColor = nextColor()
                                val newRow = Element(
                                    area = rectOf(4, newElement.area),
                                    type = Element.Type.Row,
                                    prompt = mutableStateOf(about),
                                    color = selectedColor,
                                    inner = mutableListOf()
                                )

                                column.area = rectOf(
                                    7,
                                    column.inner.map { it.area } + newElement.area
                                )
                                box.area = rectOf(
                                    10,
                                    box.inner.map { it.area } + newElement.area
                                )

                                newRow.inner.add(newElement)
                                column.inner.add(newRow)
                                onNewElement(newRow)
                                onNewElement(newElement)
                                break

                            }
                        }
                    }
                }
            }


//            if (newElement.isContainer()) {
//                val inner = newElement.findInner(elements)
//                elements.forEach {
//                    it.inner.removeAll(inner)
//                }
//                newElement.inner = inner
//            }


            elementsMenuExpanded = false

            startPoint = null
            endPoint = null
        }

        val x = if (endPoint == null) 0.dp else (endPoint!!.x.convertToPx()).dp
        val y = if (endPoint == null) 0.dp else (endPoint!!.y.convertToPx()).dp
        DropdownMenu(
            expanded = elementsMenuExpanded,
            onDismissRequest = { elementsMenuExpanded = false },
            offset = DpOffset(x = x, y = y)
        ) {
            Text(
                "Select Use Case:",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            var aboutText by remember { mutableStateOf("") }
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
            TextField(
                value = aboutText,
                label = { Text("About") },
                singleLine = true,
                maxLines = 1,
                onValueChange = {
                    aboutText = it
                },
                modifier = Modifier.focusRequester(focusRequester).onKeyEvent {
                    when {
//                        it.type == KeyEventType.KeyUp && it.key == Key.Enter -> {
//                            true
//                        }

                        else -> false
                    }
                }
            )

            fun menuItemTextBy(type: Element.Type): String {
                return when (type) {
                    Element.Type.Button -> "Button" //"ClickTo"
                    Element.Type.TextField -> "TextField" //"InputText"
                    Element.Type.Column -> "Column" //"SelectItem"
                    Element.Type.Row -> "Row" //"SelectItem"
                    Element.Type.Text -> "Text" //"LookAtText"
                    Element.Type.Icon -> "Icon" //"LookAtImage"
                    Element.Type.Coil -> "Coil" //"LookAtImage"
                    Element.Type.Box -> "Box"
                }
            }

            Element.Type.entries.forEach {
                DropdownMenuItem(
                    onClick = {
                        selectElement(it, aboutText)
                    }
                ) { Text(menuItemTextBy(it)) }
            }
        }
    }
}

const val borderWidth = 4f

private val colors = listOf(
    Color(0xFFF44336), // Красный (Red)
    Color(0xFFFF9800), // Оранжевый (Orange)
    Color(0xFFFFEB3B), // Желтый (Yellow)
    Color(0xFF4CAF50), // Зеленый (Green)
    Color(0xFF2196F3), // Голубой (Blue)
    Color(0xFF3F51B5), // Индиго (Indigo)
    Color(0xFF9C27B0)  // Фиолетовый (Violet)
)
private var colorPosition = 0
var selectedColor = nextColor()
fun nextColor(): Color {
    return colors[colorPosition].apply {
        colorPosition += 1
        if (colorPosition == colors.size) {
            colorPosition = 0
        }
    }
}