package me.pseudoapp.views

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import me.pseudoapp.*
import me.pseudoapp.other.dpToPx
import me.pseudoapp.other.measureTextHeight
import me.pseudoapp.other.measureTextWidth
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ElementsView(
    keyboardRequester: FocusRequester,
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
            condition = mutableStateOf(""),
            action = mutableStateOf(""),
            value = mutableStateOf(""),
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

        startPoint = null
        endPoint = null
        dragEnd = false
        currentColor = nextColor()

        onNewElement(newElement)
    }

    var rootRect by remember { mutableStateOf(Rect(Offset.Zero, Offset.Zero)) }
    val textMeasurer = rememberTextMeasurer()

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
            .focusRequester(keyboardRequester)
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


        elements.forEachIndexed { i, element ->
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

                        calcInstructions(elements, contentElement)
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

            val textWidth2 = measureTextWidth(element.action.value) + 24.dp
            val textHeight2 = measureTextHeight(element.action.value)
            val x2 = element.area.left + element.area.width / 2 - textWidth2.dpToPx() / 2f
            val y2 = element.area.top + 0.dp.dpToPx() + element.area.height / 2 - textHeight2.dpToPx() / 2f
            BasicTextField(
                value = elements[i].action.value,
                onValueChange = {
                    element.action.value = it

                    calcInstructions(elements, contentElement)
                },
                textStyle = TextStyle.Default.copy(textAlign = TextAlign.Center),
                modifier = Modifier
                    .offset(
                        x = x2.dp,
                        y = y2.dp
                    )
                    .width(textWidth2)
                    .dashedBorder(
                        color = element.color.copy(alpha = 0.42f),
                        shape = CutCornerShape(4.dp)
                    )
            )

            val textWidth3 = measureTextWidth(element.value.value) + 24.dp
            val textHeight3 = measureTextHeight(element.value.value)
            val x3 = element.area.left + element.area.width / 2 - textWidth3.dpToPx() / 2f
            val y3 = element.area.top + 0.dp.dpToPx() + element.area.height - textHeight3.dpToPx() / 2f
            BasicTextField(
                value = elements[i].value.value,
                onValueChange = {
                    element.value.value = it
                },
                textStyle = TextStyle.Default.copy(
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier
                    .offset(
                        x = x3.dp,
                        y = y3.dp
                    )
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
                        keyboardRequester.requestFocus()
                        elementWithMenuId = null
                    }
                )
                Divider()
                DropdownMenuItem(
                    content = { Text("Delete") },
                    onClick = {
                        elements.removeAt(elementWithMenuId!!)
                        calcInstructions(elements, contentElement)
                        elementWithMenuId = null
                    }
                )
            }
        }
    }
}

fun Modifier.dashedBorder(
    brush: Brush,
    shape: Shape,
    strokeWidth: Dp = 2.dp,
    dashLength: Dp = 4.dp,
    gapLength: Dp = 4.dp,
    cap: StrokeCap = StrokeCap.Round
) = this.drawWithContent {
    val outline = shape.createOutline(size, layoutDirection, density = this)
    val dashedStroke = Stroke(
        cap = cap,
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(
            intervals = floatArrayOf(dashLength.toPx(), gapLength.toPx())
        )
    )

    drawContent()

    drawOutline(
        outline = outline,
        style = dashedStroke,
        brush = brush
    )
}

fun Modifier.dashedBorder(
    color: Color,
    shape: Shape,
    strokeWidth: Dp = 2.dp,
    dashLength: Dp = 4.dp,
    gapLength: Dp = 4.dp,
    cap: StrokeCap = StrokeCap.Round
) = dashedBorder(brush = SolidColor(color), shape, strokeWidth, dashLength, gapLength, cap)


fun calcInstructions(elements: SnapshotStateList<Element>, element: Element) {
    element.elements.sortedBy { it.area.top }
        .forEach {
            calcInstructions(elements, it)
        }

    val action = element.action.value
    val resetAction = "=>"
    when {
        // # i + 1 => i
        action.contains(resetAction) -> {
            val leftRight = action.split(resetAction)
            val value = leftRight[0].trim()
            val name = leftRight[1].trim()

            val updated = mutableListOf<Int>()
            elements.forEachIndexed { i, it ->
                if (!name.isEmpty() && it.name.value == name) {
                    updated.add(i)
                    elements[i].value.value = value
                }
            }
        }
    }
}