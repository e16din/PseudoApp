package me.pseudoapp.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.pseudoapp.Element
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
    var currentColor by remember { mutableStateOf(nextColor()) }

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

        elements.add(
            Element(
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
                isFilled = shiftPressed.value,
            )
        )

        startPoint = null
        endPoint = null
        dragEnd = false
        currentColor = nextColor()
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
            }

    ) {

        Canvas(
            modifier = Modifier.fillMaxSize()
                .pointerInput(Unit) {
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
                        style = if (element.isFilled) Fill else Stroke(width = 2f)
                    )

                } else {
                    drawRoundRect(
                        color = element.color,
                        topLeft = element.area.topLeft,
                        size = element.area.size,
                        cornerRadius = CornerRadius(2f, 2f),
                        style = if (element.isFilled) Fill else Stroke(width = 2f)
                    )
                }

                val textLayoutResult =
                    textMeasurer.measure(
                        text = AnnotatedString(element.name),
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    )
                val textSize = textLayoutResult.size

                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        x = element.area.left + element.area.width / 2 - textSize.width / 2f,
                        y = element.area.top - 8.dp.toPx() + element.area.height / 2 - textSize.height / 2f
                    ),
                )

                val textLayoutResult2 =
                    textMeasurer.measure(
                        text = AnnotatedString(element.value),
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    )
                val textSize2 = textLayoutResult2.size

                drawText(
                    textLayoutResult = textLayoutResult2,
                    topLeft = Offset(
                        x = element.area.left + element.area.width / 2 - textSize2.width / 2f,
                        y = element.area.top + 12.dp.toPx() + element.area.height / 2 - textSize2.height / 2f
                    ),
                )
            }
        }
    }
}

enum class RainbowColor(val color: Color) {
    Red(Color(0xFFF44336)),
    Orange(Color(0xFFFF9800)),
    Yellow(Color(0xFFFFEB3B)),
    Green(Color(0xFF4CAF50)),
    Blue(Color(0xFF2196F3)),
    Indigo(Color(0xFF3F51B5)),
    Violet(Color(0xFF9C27B0)),
}

var colorPosition = 0

fun nextColor(): RainbowColor {
    return RainbowColor.entries[colorPosition].apply {
        colorPosition += 1
        if (colorPosition == RainbowColor.entries.size) {
            colorPosition = 0
        }
    }
}