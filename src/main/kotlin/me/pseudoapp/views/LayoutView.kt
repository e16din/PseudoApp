package me.pseudoapp.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.pseudoapp.*
import me.pseudoapp.other.dpToPx
import me.pseudoapp.other.measureTextHeight
import me.pseudoapp.other.measureTextWidth
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


@Composable
fun LayoutView(
    ctrlPressed: MutableState<Boolean>,
    shiftPressed: MutableState<Boolean>,
    selectedImage: ImageBitmap?,
    elements: SnapshotStateList<Element>,
    onNewElement: (Element) -> Unit,
    modifier: Modifier
) {
    var startPoint by remember { mutableStateOf<Offset?>(null) }
    var endPoint by remember { mutableStateOf<Offset?>(null) }

    var dragEnd by remember { mutableStateOf(false) }

    val textValueValues = remember { mutableStateListOf<String>() }
    val textNameValues = remember { mutableStateListOf<String>() }

    LaunchedEffect(dragEnd) {
        if (!dragEnd) {
            return@LaunchedEffect
        }

        val isCircle = !ctrlPressed.value

        var name = currentColor.name

        name += if (isCircle) {
            "Circle"
        } else {
            "Rect"
        }

        if (elements.size >= RainbowColor.entries.size) {
            name += elements.size / RainbowColor.entries.size
        }

        val newElement = Element(
            name = name,
            value = "",
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
            isCircle = isCircle,
            isAbstract = shiftPressed.value,
        )
        elements.add(newElement)

        startPoint = null
        endPoint = null
        dragEnd = false
        currentColor = nextColor()

        onNewElement(newElement)

        textNameValues.add(newElement.name)
        textValueValues.add(newElement.value)
    }

    var rootRect by remember { mutableStateOf(Rect(Offset.Zero, Offset.Zero)) }
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                val topLeft = coordinates.positionInParent()
                rootRect = Rect(
                    topLeft,
                    Offset(
                        topLeft.x + coordinates.size.width.toFloat(),
                        topLeft.y + coordinates.size.height.toFloat()
                    )
                )
                layoutRect = rootRect
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
                if (element.isCircle) {
                    drawCircle(
                        color = element.color,
                        center = element.area.center,
                        radius = element.area.size.width / 2,
                        style = if (element.isAbstract) Fill else Stroke(width = 2f)
                    )

                } else {
                    drawRoundRect(
                        color = element.color,
                        topLeft = element.area.topLeft,
                        size = element.area.size,
                        cornerRadius = CornerRadius(2f, 2f),
                        style = if (element.isAbstract) Fill else Stroke(width = 2f)
                    )
                }
            }
        }


        elements.forEachIndexed { i, element ->
            val textWidth = measureTextWidth(element.name) + 8.dp
            val textHeight = measureTextHeight(element.name)
            val x = element.area.left + element.area.width / 2 - textWidth.dpToPx() / 2f
            val y = element.area.top + 4.dp.dpToPx()
            BasicTextField(
                value = textNameValues[i],
                onValueChange = {
                    element.name = it
                    if (textNameValues.size > i) {
                        textNameValues[i] = it
                    } else {
                        textNameValues.add(i, it)
                    }
                },
                textStyle = TextStyle.Default.copy(textAlign = TextAlign.Center),
                modifier = Modifier
                    .offset(
                        x = x.dp,
                        y = y.dp
                    )
                    .width(textWidth)
//                    .dashedBorder(
//                        color = element.color.copy(alpha = 0.42f),
//                        shape = CutCornerShape(4.dp)
//                    )
            )

            val textWidth2 = measureTextWidth(element.value) + 24.dp
            val textHeight2 = measureTextHeight(element.value)
            val x2 = element.area.left + element.area.width / 2 - textWidth2.dpToPx() / 2f
            val y2 = element.area.top + 0.dp.dpToPx() + element.area.height / 2 - textHeight2.dpToPx() / 2f
            BasicTextField(
                value = textValueValues[i],
                onValueChange = {
                    element.value = it
                    if (textValueValues.size > i) {
                        textValueValues[i] = it
                    } else {
                        textValueValues.add(i, it)
                    }
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