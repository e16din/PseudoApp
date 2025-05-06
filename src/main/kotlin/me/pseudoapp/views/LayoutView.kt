package me.pseudoapp.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.pseudoapp.Element
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


@Composable
fun LayoutView(
    ctrlPressed: MutableState<Boolean>,
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

        elements.add(
            Element(
                name = "CircleArea1",
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
                color = currentColor,
                isCircle = !ctrlPressed.value
            )
        )
        startPoint = null
        endPoint = null
        dragEnd = false
        currentColor = nextColor()
    }

    var rootRect by remember { mutableStateOf(Rect(Offset.Zero, Offset.Zero)) }
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
//                            requester.requestFocus()

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
                    color = currentColor.copy(alpha = 0.3f),
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
                    )

                } else {
                    drawRoundRect(
                        color = element.color,
                        topLeft = element.area.topLeft,
                        size = element.area.size,
                        cornerRadius = CornerRadius(2f, 2f),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }

        elements.forEach { element ->
            val textStyle = TextStyle.Default.copy(fontSize = 8.sp)
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Green)
            ) {
                Text(
                    style = textStyle,
                    text = element.name,
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}


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

fun nextColor(): Color {
    return colors[colorPosition].apply {
        colorPosition += 1
        if (colorPosition == colors.size) {
            colorPosition = 0
        }
    }
}