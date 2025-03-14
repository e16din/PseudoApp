import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection


@Composable
@Preview
fun App() {
    SizeObserver()

    MaterialTheme {
        MainScreen()
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}

data class Goal(var area: Rect, val color:Color, val element: Element, val prompt: MutableState<String>)

fun Rect.contains(area: Rect): Boolean {
    return this.topLeft.x < area.topLeft.x
            && this.topLeft.y < area.topLeft.y
            && this.bottomRight.x > area.bottomRight.x
            && this.bottomRight.y > area.bottomRight.y
}

data class Element(val type: Type, val tag: String? = null) {
    enum class Type {
        Screen,
        CustomView,
        Button,
        TextField,
        LazyList, // Horizontal || Vertical /TODO: сразу квадрат итема добавлять, и перетаскивать за лейбл, по alt показывать крестики удаления
        Text,
        Image, // Icon || Coil
    }
}
// onValueChanged {
//   action: platform, backend
//   ...
//
//   // prompt:
// }

// Показывать код для копирования и результат сгенерированного приложения

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen() {
    var selectedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var layoutRect by remember { mutableStateOf<Rect>(Rect()) }
    var goals = remember { mutableStateListOf<Goal>() }
    val undoGoals = remember { mutableStateListOf<Goal>() }

    val clipboardManager = remember { ClipboardManager() }
    val imagePicker = remember { ImagePicker() }
    var codeText by remember { mutableStateOf("") }

    Column(Modifier.onKeyEvent { keyEvent ->
        if (keyEvent.key == Key.Z
            && keyEvent.isCtrlPressed
            && keyEvent.type == KeyEventType.KeyUp
        ) {
            undoGoals.add(goals.last())
            goals = goals.dropLast(1).toMutableStateList()
            return@onKeyEvent true
        }
        false
    }) {
        Button(onClick = {
            imagePicker.pickImage {
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
                    codeText = createScreenCode(goals)
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
                            clipboardManager.copyToClipboard(finalPrompt)

                        }) {
                            Text("Копировать результат")
                        }

                    }
                    items(goals.size) { i ->
                        PromptItem(goals[i], onRemoveClick = {
                            goals.removeAt(i)
                            codeText = createScreenCode(goals)
                        }, canRemove = i != 0)
                    }

                }
            }
            Card {
                TextField(value = codeText, onValueChange = {
                    codeText = it
                }, modifier = Modifier.width(360.dp))
            }
        }
    }
}

fun createScreenCode(goals: SnapshotStateList<Goal>):String {
    var result = ""
    root = "\n"
    result = "fun "
    result += createCode(goals)
    result += root

    return result
}

var root = ""
fun createCode(goals: List<Goal>): String {
    var result = ""
    val handled = mutableListOf<Goal>()
    goals.forEach { i ->
        if (!handled.contains(i)) {
            val inner = mutableListOf<Goal>()
            goals.forEach { j ->
                if (i.area.contains(j.area)) {
                    inner.add(j)
                    handled.add(j)
                }
            }

            val name = if (i.element.tag != null && i.element.tag != "") i.element.tag else i.element.type.name
            val args = if (inner.isNotEmpty()) createCode(inner) else ""
            if (args.isEmpty()) {
                result += "$name(modifier = Modifier)\n"
            } else {
                result += "$name() {\n" +
                        "$args\n" +
                        "}\n"
            }
            if (i.element.type == Element.Type.CustomView) {
                root += "fun $name($args) {\n" +
                        "}\n\n"
            }
        }
    }
    return result
}

@Composable
fun PromptItem(
    goal: Goal,
    onRemoveClick: () -> Unit,
    canRemove: Boolean = true
) {
    val elementName =
        if (goal.element.tag != null && goal.element.tag != "") goal.element.tag else goal.element.type.name
    Text(elementName)
    Box {
        TextField(
            value = goal.prompt.value,
            label = { Text("Prompt") },
            onValueChange = { value ->
                goal.prompt.value = value
            },
            modifier = Modifier.padding(vertical = 8.dp)
        )
        if (canRemove) {
            IconButton(onClick = onRemoveClick, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Default.Close, "clear")
            }
        }
    }
}


class ClipboardManager {
    fun copyToClipboard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
    }
}

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
                    text = if(goal.element.type == Element.Type.CustomView)
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

val colors = listOf(
    Color.Red.copy(alpha = 0.5f),
//    Color.Orange.copy(alpha = 0.5f),
    Color.Yellow.copy(alpha = 0.5f),
    Color.Green.copy(alpha = 0.5f),
//    Color.Sky.copy(alpha = 0.5f),
    Color.Blue.copy(alpha = 0.5f),
//    Color.Violet.copy(alpha = 0.5f),
    Color.Black.copy(alpha = 0.5f),
    Color.Gray.copy(alpha = 0.5f),
    Color.White.copy(alpha = 0.5f),
)
var colorPosition = 0
var selectedColor = nextColor()
fun nextColor(): Color {
    return colors[colorPosition].apply {
        colorPosition += 1
        if (colorPosition == colors.size) {
            colorPosition = 0
        }
    }

}

fun menuItemTextBy(type: Element.Type): String {
    return when (type) {
        Element.Type.Screen -> "Screen" // "StartScreen"
        Element.Type.CustomView -> "CustomView" // "StartScreen"
        Element.Type.Button -> "Button" //"ClickTo"
        Element.Type.TextField -> "TextField" //"InputText"
        Element.Type.LazyList -> "List" //"ScrollTo"
        Element.Type.Text -> "Text" //"LookAtText"
        Element.Type.Image -> "Image" //"LookAtImage"
    }
}

data class Rect(
    var topLeft: Offset = Offset.Zero,
    var bottomRight: Offset = Offset.Zero
) {
    val size: Size
        get() = Size(bottomRight.x - topLeft.x, bottomRight.y - topLeft.y)
}

// task:
// if (goal in CustomView) print goal in CustomView body
// else if (goal is basic view && goal in other basic view rect) print Box, print otherView, print goal in Box
// else if(goal is basic view && goal in other container view rect) print goal in Container View
// else print Goal in Screen
