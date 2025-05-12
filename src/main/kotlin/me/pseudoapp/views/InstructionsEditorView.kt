package me.pseudoapp.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import me.pseudoapp.Element
import me.pseudoapp.other.calcMath
import me.pseudoapp.other.measureTextHeight

@Composable
fun InstructionsEditorView(
    elements: SnapshotStateList<Element>,
    newElement: State<Element?>,
    currentColor: MutableState<RainbowColor>
) {
    val instructionsRequester = remember { FocusRequester() }
    var codeValue by remember { mutableStateOf(TextFieldValue()) }
//    var isElementInserted by remember { mutableStateOf(false) }
    var isCodeCompletionEnabled by remember { mutableStateOf(false) }
    var textFieldPosition by remember { mutableStateOf(Offset.Zero) } // позиция TextField на экране
    var cursorOffsetInTextField by remember { mutableStateOf(Offset.Zero) } // позиция курсора внутри TextField
    var textFieldSize by remember { mutableStateOf(IntSize.Zero) }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val namesMap = remember { mutableMapOf<Int, String>() }// <Index, Name>
    val textStyle = TextStyle.Default

    LaunchedEffect(newElement.value) {
        if (newElement.value == null) return@LaunchedEffect

        val newText = codeValue.text + "\n = ${newElement.value!!.name}\n"
        codeValue = TextFieldValue(
            text = newText,
            selection = TextRange(newText.length)
        )
    }

    fun calc(data: String): String { // simple implementation
        return try {
            calcMath(data)
        } catch (e: Exception) {
            "?"
        }
    }

    fun updateValues() {
        // detect value changed
        val source = StringBuilder(codeValue.text)
        val lines = source.split("\n")
        lines.forEachIndexed { i, line ->
            // если нашли знак = и справа от него - словарь:имя,
            // то наполняем словарь тем что слева от =
            val index = line.lastIndexOf("=")

            if (index != -1
                && line.trim().length > 1
                && index < line.length - 1
            ) {
                var left = line.substring(0, index)
                val right = line.substring(index + 1, line.length).trim()

                var totalTextIndex = 0
                var pointer = i - 1
                while (pointer >= 0) {
                    totalTextIndex += lines[pointer].length + 1
                    pointer--
                }
                totalTextIndex += index + 1

                namesMap[totalTextIndex] = right
                println("right: $right")

                if (left.trim() == "}") {
                    var closedBracketIndex = 0
                    var pointer = i - 1
                    while (pointer >= 0) {
                        closedBracketIndex += lines[pointer].length + 1
                        pointer--
                    }
                    println("endBracketIndex: $closedBracketIndex")

                    var i = closedBracketIndex - 1
                    var counter = 1
                    while (counter > 0 && i >= 0) {
                        when {
                            source[i] == '}' -> counter++
                            source[i] == '{' -> counter--
                        }
                        i--
                    }

                    val openBracketIndex = i + 1
//                    var lineIndex = source.indexOf("\n", start)
//                    while (lineIndex != -1 && lineIndex <= end) {
//                        source.insert(lineIndex + 1, "    ")
//                        lineIndex = source.indexOf("\n", lineIndex+1)
//                        end = source.indexOf("}", start)
//                    }
//
//                    codeValue = TextFieldValue(
//                        text = source.toString(),
//                        selection = TextRange(0),
//                    )

                    left = source.substring(openBracketIndex + 1, closedBracketIndex)
                }
                println("left a: $left")

                var calculated = ""
                left.split("\n").forEach { leftLine ->
                    val lineCalculated = StringBuilder(leftLine)
                    var startMathIndex = lineCalculated.indexOf("{", 0)
                    var endMathIndex = lineCalculated.indexOf("}", 0)
                    if (startMathIndex != -1 && endMathIndex != -1 && startMathIndex != endMathIndex - 1) {

                        while (startMathIndex != -1 && endMathIndex != -1) {
                            if (startMathIndex + 1 != endMathIndex) {
                                println("x1")
                                val value = StringBuilder(lineCalculated.substring(startMathIndex + 1, endMathIndex))
                                println("x2")
                                elements.forEach {
                                    println("it.name: ${it.name}")
                                    val nameIndex = value.indexOf(it.name)
                                    if (nameIndex != -1) {
                                        value.replace(nameIndex, nameIndex + it.name.length, it.value)
                                    }
                                }
                                val calcResult = calc(value.toString())
                                lineCalculated.replace(
                                    startMathIndex, endMathIndex + 1,
                                    calcResult
                                )
                            }

                            startMathIndex = lineCalculated.indexOf("{", endMathIndex + 1)
                            endMathIndex = lineCalculated.indexOf("}", endMathIndex + 1)
                        }

                        calculated += lineCalculated.toString() + "\n"

                    } else {
                        calculated += leftLine + "\n"
                    }

                }
                left = calculated

                val name = right.trim()
                val value = left
                val element = elements.firstOrNull { it.name == name }

                element?.let {
                    elements.remove(element)
                    elements.add(element.copy(value = value, index = totalTextIndex))
                } ?: run {
                    // ренэйм элемента,
                    //  чтобы при редактировании не создавалось куча новых элементов
                    val editedElement = elements.firstOrNull { it.index == totalTextIndex }

                    if (editedElement != null) {
                        elements.remove(editedElement)
                        elements.add(editedElement.copy(name = name, value = value))

                    } else {
                        // добавление новой абстракции
                        elements.add(
                            Element(
                                name = name,
                                value = value,
                                area = Rect(
                                    Offset(100f, 100f),
                                    Offset(
                                        300f, 300f
                                    )
                                ),
                                color = currentColor.value.color,
                                isCircle = false,
                                isAbstract = true,
                                index = totalTextIndex
                            )
                        )
                    }
                    currentColor.value = nextColor()
                }

                // очистка мусора :)
                // элементы которые мы удалили в коде - удаляем и на макете
                println("namesMap: ${namesMap}")
                val removed = elements.filter { !namesMap.values.contains(it.name) }
                println("elements: ${elements.map { it.name }}")
                println("removed: ${removed}")
//                elements.removeAll(removed)
            }

            // это именование/добавление в словарь/создание функции
            // x = {1+1} = etc & "blablabla" = RedCircle

            // # это много-линейный блок
            // {
            // x = {1+1}
            // = etc
            //
            // &
            //
            // "blablabla"
            //
            // } = RedCircle

            // # цикл
            // 0 = i
            // i > 12 ?
            // yes -> do
            // no -> i+1 = i # присвоение пересчитывает все инструкции в блоке с таким же названием
        }
    }

    @Composable
    fun completeCode() {
        val prevDividerIndex = listOf(
            codeValue.text.lastIndexOf("\n", codeValue.selection.end - 1),
            codeValue.text.lastIndexOf(" ", codeValue.selection.end - 1),
            codeValue.text.lastIndexOf(",", codeValue.selection.end - 1),
            codeValue.text.lastIndexOf("{", codeValue.selection.end - 1),
            codeValue.text.lastIndexOf("=", codeValue.selection.end - 1),
        ).filter { it >= 0 }.maxByOrNull { it } ?: 0

        val start = prevDividerIndex + 1
        val query = codeValue.text.substring(
            startIndex = start, endIndex = if (codeValue.selection.end > start) codeValue.selection.end else start
        )
        val isCodeCompletionShown = query.length > 1 //&& !isElementInserted
        if (isCodeCompletionShown) {
            val x = textFieldPosition.x + cursorOffsetInTextField.x
            val y = textFieldPosition.y + cursorOffsetInTextField.y

            val nextDividerIndex = listOf(
                codeValue.text.indexOf("\n", codeValue.selection.end - 1),
                codeValue.text.indexOf(" ", codeValue.selection.end - 1),
                codeValue.text.indexOf(",", codeValue.selection.end - 1),
            ).filter { it >= 0 }.minByOrNull { it } ?: (codeValue.text.length)

            Column(
                Modifier.offset(
                    x = x.dp - 16.dp,
                    y = y.dp + measureTextHeight("Height", textStyle),
                ).background(Color(0xFF141414))
                // NOTE: todo: клик показывать не на кнопке а там где нажал, если попадает в кнопку то срабатывает

            ) {


                val filtered = elements.filter { it.name.contains(query) }
                filtered.forEach { element ->
                    val newText =
                        StringBuffer(codeValue.text).replace(codeValue.selection.end, nextDividerIndex, "")
                            .replace(
                                if (prevDividerIndex == 0) 0 else prevDividerIndex + 1,
                                codeValue.selection.end,
                                ""
                            ).insert(
                                if (prevDividerIndex == 0) 0 else prevDividerIndex + 1, element.name
                            ).toString()

                    Text(
                        element.name, color = Color.White, modifier = Modifier.clickable {
                            //isElementInserted = true
                            isCodeCompletionEnabled = false
                            val selectorPosition =
                                (prevDividerIndex + if (prevDividerIndex == 0) 0 else 1) + element.name.length
                            instructionsRequester.requestFocus()
                            codeValue = TextFieldValue(
                                text = newText,
                                selection = TextRange(selectorPosition),
                            )
                        })
                    Divider()
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box {
            BasicTextField(
                value = codeValue,
                textStyle = textStyle,
                onValueChange = {
                    codeValue = it

                    //isElementInserted = false

                    val cursorPos = codeValue.selection.end
                    if (layoutResult != null && cursorPos >= 0 && cursorPos <= codeValue.text.length) {
                        try {
                            val cursorRect = layoutResult!!.getCursorRect(cursorPos)
                            cursorOffsetInTextField = Offset(cursorRect.left, cursorRect.top)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    isCodeCompletionEnabled = true
//                    if(!isFormatting) {
                    try {
                        updateValues()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

//                    }

                }, onTextLayout = { result ->
                    layoutResult = result
                    val cursorPos = codeValue.selection.end
                    if (cursorPos >= 0 && cursorPos <= codeValue.text.length) {
                        val cursorRect = result.getCursorRect(cursorPos)
                        cursorOffsetInTextField = Offset(cursorRect.left, cursorRect.top)
                    }
                },

                modifier = Modifier.fillMaxSize().focusRequester(instructionsRequester)
                    .onGloballyPositioned { coordinates ->
                        textFieldPosition = coordinates.positionInParent()
                        textFieldSize = coordinates.size
                    })

            if (isCodeCompletionEnabled) {
                completeCode()
            }
        }
    }
}