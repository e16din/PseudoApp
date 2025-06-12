package me.pseudoapp.views

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.pseudoapp.*
import me.pseudoapp.icons.play
import me.pseudoapp.icons.playPause
import me.pseudoapp.other.dashedBorder
import me.pseudoapp.other.dpToPx
import me.pseudoapp.other.measureTextHeight
import me.pseudoapp.other.measureTextWidth
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


enum class CalcState {
    InProgress,
    Paused,
    Done
}

var i = 0
var startIndex = 0

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ElementsView(
    hotkeysFocusRequester: FocusRequester,
    ctrlPressed: MutableState<Boolean>,
    shiftPressed: MutableState<Boolean>,
    selectedImage: ImageBitmap?,
    onNewElement: (Element) -> Unit,
    onDiveInClick: (Element) -> Unit,
    contentElement: Element,
    modifier: Modifier
) {
    var startPoint by remember { mutableStateOf<Offset?>(null) }
    var endPoint by remember { mutableStateOf<Offset?>(null) }

    var dragEnd by remember { mutableStateOf(false) }
    var elementWithMenuId by remember { mutableStateOf<Int?>(null) }

    val elements = contentElement.elements

    val stepDelayMsValue = remember { mutableStateOf("200") }
    var calcState by remember { mutableStateOf(CalcState.InProgress) }
    val isNextStepAllowed = remember { mutableStateOf(false) }

    val startCycleElements = mutableStateListOf<Element?>()

    LaunchedEffect(calcState, isNextStepAllowed) {
        println("lifecycle:")
        // Lifecycle
        calcScope.launch {
            while (calcState != CalcState.Done) {
                val delayMs = if (stepDelayMsValue.value.isEmpty()) 0 else stepDelayMsValue.value.toLong()
                delay(delayMs)

                if (contentElement.elements.isEmpty()) {
                    continue
                }

                if (calcState != CalcState.Paused
                    || isNextStepAllowed.value
                ) {
                    println("calcState: $calcState")
                    println("isNextStepAllowed: ${isNextStepAllowed.value}")
                    println("contentElement: ${contentElement.name.value}")

                    if (isNextStepAllowed.value) {
                        calcState = CalcState.Paused
                        isNextStepAllowed.value = false
                    }

                    println("s3")
                    fun findStartIndex(): Int {
                        startIndex = elements.indexOf(startCycleElements.lastOrNull())
                        if (startIndex == -1) {
                            startIndex = 0
                        }
                        return startIndex
                    }

                    if (i > contentElement.elements.size - 1 || i < startIndex) {
                        i = startIndex
                    }
                    println("i c: $i")

                    try {
                        val e = contentElement.elements[i]

                        startIndex = findStartIndex()
                        println("startCycleElement: ${startCycleElements.lastOrNull()?.name?.value}")
                        println("startIndex a: $startIndex")

                        println("i a: $i")

                        if (i >= startIndex) {
                            val result = calcInstructions(elements, startCycleElements, e)
                            if (result == CalcState.Paused || result == CalcState.Done) {
                                calcState = result
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    i += 1

                    startIndex = findStartIndex()
                    println("startIndex b: $startIndex")
                    println("i b: $i")
                }
            }
        }
    }

    LaunchedEffect(dragEnd) {
        if (!dragEnd) {
            return@LaunchedEffect
        }

        val isAbstract = ctrlPressed.value
        val isFilled = shiftPressed.value

        var name = if (isAbstract) "" else currentColor.name

        if (!isAbstract && elements.any { it.name.value == name }) {
            name += elements.size / RainbowColor.entries.size
        }

        val newElement = Element(
            name = mutableStateOf(name),
            text = mutableStateOf(""),
            result = mutableStateOf(""),
            area = Rect(
                Offset(
                    min(startPoint!!.x, endPoint!!.x),
                    min(startPoint!!.y, endPoint!!.y)
                ),
                Offset(
                    max(startPoint!!.x, endPoint!!.x),
                    max(startPoint!!.y, endPoint!!.y)
                )
            ),
            color = currentColor.color,
            isAbstract = isAbstract,
            isFilled = isFilled,
        )
        elements.add(newElement)
        val sorted = elements.sortedBy { it.area.top }
        elements.clear()
        elements.addAll(sorted)
        println("tag1: ${elements.map { it.name.value }}")

        startPoint = null
        endPoint = null
        dragEnd = false
        currentColor = nextColor()

        onNewElement(newElement)
    }

    var rootRect by remember { mutableStateOf(Rect(Offset.Zero, Offset.Zero)) }
//    val textMeasurer = rememberTextMeasurer()

    fun onPlayClick() {
        calcState = CalcState.InProgress
        hotkeysFocusRequester.requestFocus()
    }

    @Composable
    fun PlayPauseButtons() {
        if (calcState == CalcState.Paused) {
            Icon(
                playPause, "playPause", Modifier
                    .padding(4.dp)
                    .clip(CircleShape)
                    //                                .background(element.color.copy(alpha = 0.42f))
                    .clickable {
                        isNextStepAllowed.value = true
                        hotkeysFocusRequester.requestFocus()
                    }
            )
        }

        if (calcState == CalcState.Paused || calcState == CalcState.Done) {
            Icon(
                play, "play", Modifier
                    .padding(4.dp)
                    .clip(CircleShape)
                    //                                .background(element.color.copy(alpha = 0.42f))
                    .clickable {
                        onPlayClick()
                    }
            )
        }

    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                val topLeft = Offset.Zero // coordinates.positionInParent()
                rootRect = Rect(
                    topLeft,
                    Offset(
                        topLeft.x + coordinates.size.width.toFloat(),
                        topLeft.y + coordinates.size.height.toFloat()
                    )
                )
                layoutRect = rootRect
            }
            .focusable()
            .focusRequester(hotkeysFocusRequester)
            .onKeyEvent { keyEvent ->
                ctrlPressed.value = keyEvent.isCtrlPressed
                        && keyEvent.type == KeyEventType.KeyDown

                shiftPressed.value = keyEvent.isShiftPressed
                        && keyEvent.type == KeyEventType.KeyDown

                return@onKeyEvent true
            }

    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            startPoint = offset
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
                            dragEnd = true
                        }
                    )
                }) {

            selectedImage?.let {
                drawImage(it)
            }

            drawRect(
                color = Color.Black.copy(alpha = 0.3f),
                topLeft = rootRect.topLeft,
                size = rootRect.size,
                style = Stroke(
                    width = 1f,
                    pathEffect =
                        PathEffect.dashPathEffect(floatArrayOf(10f, 5f), phase = 0f)
                )
            )

            endPoint?.let {
                drawRect(
                    color = currentColor.color.copy(alpha = 0.3f),
                    topLeft = Offset(
                        min(startPoint!!.x, endPoint!!.x),
                        min(startPoint!!.y, endPoint!!.y)
                    ),
                    size = Size(
                        width = abs(startPoint!!.x - endPoint!!.x),
                        height = abs(startPoint!!.y - endPoint!!.y)
                    )
                )
            }

            elements.forEach { element ->
                if (element.isAbstract) {
                    drawRoundRect(
                        color = element.color,
                        topLeft = element.area.topLeft,
                        size = element.area.size,
                        cornerRadius = CornerRadius(2f, 2f),
                        style = if (element.isFilled) Fill else Stroke(width = 2f)
                    )
                } else {
                    drawCircle(
                        color = element.color,
                        center = element.area.center,
                        radius = element.area.size.width / 2,
                        style = if (element.isFilled) Fill else Stroke(width = 2f)
                    )

                }
            }
        }

        @Composable
        fun addRealElementView(element: Element, i: Int) {
            val textWidth = measureTextWidth(element.name.value) + 8.dp
            val textHeight = measureTextHeight(element.name.value)
            val x = element.area.left + element.area.width / 2 - textWidth.dpToPx() / 2f
            val y = element.area.top + 4.dp.dpToPx()

            Row(
                Modifier.offset(
                    x = x.dp,
                    y = y.dp
                )
            ) {

                BasicTextField(
                    value = elements[i].name.value,
                    onValueChange = {
                        element.name.value = it
                    },
                    textStyle = TextStyle.Default.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.width(textWidth)
                    //                    .dashedBorder(
                    //                        color = element.color.copy(alpha = 0.42f),
                    //                        shape = CutCornerShape(4.dp)
                    //                    )
                )

                Text(
                    " ⋮ ",
                    color = Color.White,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(element.color.copy(alpha = 0.32f))
                        .clickable {
                            elementWithMenuId = i
                        }
                        .padding(bottom = 2.dp)
                )
            }

            val textWidth2 = measureTextWidth(element.result.value) + 24.dp
            val textHeight2 = measureTextHeight(element.result.value)
            val x2 = element.area.left + element.area.width / 2 - textWidth2.dpToPx() / 2f
            val y2 = element.area.top + 0.dp.dpToPx() + element.area.height / 2 - textHeight2.dpToPx() / 2f
            Row(
                Modifier
                    .offset(
                        x = x2.dp,
                        y = y2.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = elements[i].result.value,
                    onValueChange = {
                        element.result.value = it
                    },
                    textStyle = TextStyle.Default.copy(
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier =
                        Modifier.width(textWidth2)
                            .border(
                                1.dp,
                                color = element.color,
                                shape = CircleShape
                            )
                            .clip(CircleShape)
                            .background(element.color.copy(alpha = 0.82f))
                            .padding(vertical = 2.dp)
                )


                if (element.inProgress.value) {
                    PlayPauseButtons()
                }
            }
        }

        @Composable
        fun addAbstractElementView(element: Element, i: Int) {
            val textWidth = measureTextWidth(element.name.value) + 8.dp
            val textHeight = measureTextHeight(element.name.value)
            val x = element.area.left + element.area.width / 2 - textWidth.dpToPx() / 2f
            val y = element.area.top + 4.dp.dpToPx()

            Row(
                Modifier.offset(
                    x = x.dp,
                    y = y.dp
                )
            ) {

                BasicTextField(
                    value = elements[i].name.value,
                    onValueChange = {
                        element.name.value = it
                    },
                    textStyle = TextStyle.Default.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.width(textWidth)
                    //                    .dashedBorder(
                    //                        color = element.color.copy(alpha = 0.42f),
                    //                        shape = CutCornerShape(4.dp)
                    //                    )
                )

                Text(
                    " ⋮ ",
                    color = Color.White,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(element.color.copy(alpha = 0.32f))
                        .clickable {
                            elementWithMenuId = i
                        }
                        .padding(bottom = 2.dp)
                )
            }

            val textWidth2 = measureTextWidth(element.text.value) + 24.dp
            val textHeight2 = measureTextHeight(element.text.value)
            val x2 = element.area.left + element.area.width / 2 - textWidth2.dpToPx() / 2f
            val y2 = element.area.top + 0.dp.dpToPx() + element.area.height / 2 - textHeight2.dpToPx() / 2f
            BasicTextField(
                value = elements[i].text.value,
                onValueChange = {
                    element.text.value = it
                },
                textStyle = TextStyle.Default.copy(textAlign = TextAlign.Center),
                modifier =
                    Modifier.offset(
                        x = x2.dp,
                        y = y2.dp
                    )
                        .width(textWidth2)
                        .dashedBorder(
                            color = element.color.copy(alpha = 0.42f),
                            shape = CutCornerShape(4.dp)
                        )
            )

            val textWidth3 = measureTextWidth(element.result.value) + 24.dp
            val textHeight3 = measureTextHeight(element.result.value)
            val x3 = element.area.left + element.area.width / 2 - textWidth3.dpToPx() / 2f
            val y3 = element.area.top + 0.dp.dpToPx() + element.area.height - textHeight3.dpToPx() / 2f
            Row(
                Modifier
                    .offset(
                        x = x3.dp,
                        y = y3.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = elements[i].result.value,
                    onValueChange = {
                        element.result.value = it
                    },
                    textStyle = TextStyle.Default.copy(
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier
                        .width(textWidth3)
                        .border(
                            1.dp,
                            color = element.color,
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .background(element.color.copy(alpha = 0.82f))
                        .padding(vertical = 2.dp)
                )

                if (element.inProgress.value) {
                    PlayPauseButtons()
                }
            }
        }

        elements.forEachIndexed { i, element ->
            if (element.isAbstract) {
                addAbstractElementView(element, i)
            } else {
                addRealElementView(element, i)
            }
        }

        if (elementWithMenuId != null) {
            val e = elements[elementWithMenuId!!]

            DropdownMenu(
                expanded = true,
                onDismissRequest = { elementWithMenuId = null },
                offset = DpOffset(
                    x = e.area.left.dp,
                    y = e.area.top.dp
                ),
                modifier = Modifier
            ) {
                DropdownMenuItem(
                    content = { Text("Dive In") },
                    onClick = {
                        onDiveInClick(elements[elementWithMenuId!!])
                        hotkeysFocusRequester.requestFocus()
                        elementWithMenuId = null
                    }
                )
                Divider()
                DropdownMenuItem(
                    content = { Text("Delete") },
                    onClick = {
                        elements.removeAt(elementWithMenuId!!)
                        hotkeysFocusRequester.requestFocus()
                        elementWithMenuId = null
                    }
                )
            }
        }

        Row(
            Modifier.align(Alignment.BottomEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Step Delay: ")

            BasicTextField(
                value = stepDelayMsValue.value,
                onValueChange = {
                    stepDelayMsValue.value = it
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(80.dp)
                    .padding(horizontal = 8.dp)
                    .background(Color.LightGray)
                    .padding(6.dp)
            )

            if (calcState == CalcState.Paused) {
                Button(onClick = {
                    isNextStepAllowed.value = true

                }, Modifier.padding(horizontal = 21.dp)) {
                    Text("Next Step >")
                }
            }

            IconToggleButton(
                calcState == CalcState.Paused || calcState == CalcState.Done,
                onCheckedChange = {
                    calcState = if (it) CalcState.Paused else CalcState.InProgress
                },
                content = {
                    if (calcState == CalcState.Paused || calcState == CalcState.Done)
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "play"
                        )
                    else
                        Icon(
                            Icons.Default.Lock,
                            "pause",
                            tint = Color.LightGray
                        )
                }
            )
        }
    }
}

