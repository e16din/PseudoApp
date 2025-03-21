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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.pseudoapp.*
import me.pseudoapp.other.Rect
import me.pseudoapp.other.measureTextWidth
import me.pseudoapp.other.toPx

@Composable
fun LayoutView(
    selectedImage: ImageBitmap?,
    goals: SnapshotStateList<Goal>,
    onNewGoal: (Goal) -> Unit,
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
                        if (endPoint == null) {
                            endPoint = start + dragAmount
                        } else {
                            endPoint = endPoint!! + dragAmount
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

            goals.forEach { goal ->
                drawRect(
                    color = goal.color,
                    topLeft = goal.area.topLeft,
                    size = goal.area.size,
                    style = Stroke(width = 4.dp.toPx())
                )
            }
        }

        goals.forEach { goal ->
            val textStyle = TextStyle.Default.copy(fontSize = 8.sp)
            Box(
                modifier = Modifier
                    .offset(
                        x = goal.area.bottomRight.x.toPx().dp - (measureTextWidth(
                            goal.element.type.name,
                            textStyle
                        ) / 2),
                        y = goal.area.bottomRight.y.toPx().dp
                    )
                    .clip(CircleShape)
                    .background(Color.Green)
            ) {
                Text(
                    style = textStyle,
                    text = if (goal.element.type == Element.Type.CustomView)
                        goal.element.tag ?: goal.element.type.name
                    else
                        goal.element.type.name,
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        fun selectElement(it: Element.Type, tag: String? = null) {
            val finalRect = Rect(startPoint!!, endPoint!!)

            val goal = Goal(
                area = finalRect,
                element = Element(it, tag),
                prompt = mutableStateOf(""),
                color = selectedColor
            )

            onNewGoal(goal)

            elementsMenuExpanded = false

            startPoint = null
            endPoint = null
        }

        val x = if (endPoint == null) 0.dp else (endPoint!!.x.toPx()).dp
        val y = if (endPoint == null) 0.dp else (endPoint!!.y.toPx()).dp
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
            var text by remember { mutableStateOf("") }
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
            TextField(
                value = text,
                label = { Text("View Name") },
                singleLine = true,
                maxLines = 1,
                onValueChange = {
                    text = it
                },
                modifier = Modifier.focusRequester(focusRequester).onKeyEvent {
                    when {
                        it.type == KeyEventType.KeyUp && it.key == Key.Enter -> {
                            selectElement(Element.Type.CustomView, text.replace("\n", ""))
                            true
                        }

                        else -> false
                    }
                }
            )

            fun menuItemTextBy(type: Element.Type): String {
                return when (type) {
                    Element.Type.Screen -> "Screen" // "StartScreen"
                    Element.Type.CustomView -> "CustomView" // "StartScreen"
                    Element.Type.Button -> "Button" //"ClickTo"
                    Element.Type.TextField -> "TextField" //"InputText"
                    Element.Type.List -> "List" //"SelectItem"
                    Element.Type.Text -> "Text" //"LookAtText"
                    Element.Type.Image -> "Image" //"LookAtImage"
                }
            }

            Element.Type.entries.drop(1).forEach {
                DropdownMenuItem(
                    onClick = {
                        selectElement(it)
                    }
                ) { Text(menuItemTextBy(it)) }
            }
        }
    }
}

private val colors = listOf(
    Color.Red.copy(alpha = 0.75f),
//    Color.Orange.copy(alpha = 0.75f),
    Color.Yellow.copy(alpha = 0.75f),
    Color.Green.copy(alpha = 0.75f),
//    Color.Sky.copy(alpha = 0.75f),
    Color.Blue.copy(alpha = 0.75f),
//    Color.Violet.copy(alpha = 0.75f),
    Color.Black.copy(alpha = 0.75f),
    Color.Gray.copy(alpha = 0.75f),
    Color.White.copy(alpha = 0.75f),
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