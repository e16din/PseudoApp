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
import androidx.compose.ui.unit.dp
import me.pseudoapp.Element
import me.pseudoapp.currentColor
import me.pseudoapp.layoutRect
import me.pseudoapp.nextColor
import me.pseudoapp.other.Experiments.isDigitsOnly
import me.pseudoapp.other.calcMath
import me.pseudoapp.other.measureTextHeight
import me.pseudoapp.other.positionOf
import kotlin.math.abs

const val unknown = "?"

@Composable
fun InstructionsEditorView(
    elements: SnapshotStateList<Element>,
    newElement: State<Element?>,
) {
    val instructionsRequester = remember { FocusRequester() }
    var codeValue by remember { mutableStateOf(TextFieldValue()) }

//    var isElementInserted by remember { mutableStateOf(false) }
    var isCodeCompletionEnabled by remember { mutableStateOf(false) }
    var textFieldPosition by remember { mutableStateOf(Offset.Zero) } // позиция TextField на экране
    var cursorOffsetInTextField by remember { mutableStateOf(Offset.Zero) } // позиция курсора внутри TextField

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val textStyle = TextStyle.Default

    LaunchedEffect(newElement.value) {
        if (newElement.value == null) return@LaunchedEffect

        val newLine = " = ${newElement.value!!.name}\n"
        val newText = codeValue.text + "\n" + newLine
        codeValue = TextFieldValue(
            text = newText,
            selection = TextRange(newText.length - newLine.length)
        )

        updateValues(codeValue.text, elements)
    }

    @Composable
    fun completeCode() {
        if (codeValue.text.length < 2) {
            return
        }

        val prevDividerIndex = listOf(
            codeValue.text.lastIndexOf("\n", codeValue.selection.end - 1),
            codeValue.text.lastIndexOf(" ", codeValue.selection.end - 1),
            codeValue.text.lastIndexOf("$", codeValue.selection.end - 1),
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
                    if (layoutResult != null && cursorPos > 0 && cursorPos < codeValue.text.length) {
                        try {
                            val cursorRect = layoutResult!!.getCursorRect(cursorPos)
                            cursorOffsetInTextField = Offset(cursorRect.left, cursorRect.top)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    isCodeCompletionEnabled = true

                    try {
                        updateValues(codeValue.text, elements)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

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
                    })

            if (isCodeCompletionEnabled) {
                completeCode()
            }
        }
    }
}


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

val namesMap = mutableMapOf<Int, String>() // <Index, Name>
val ends = listOf(
    "$", " ", "=", "{", "(", ",", ";", "\n", "\t"
)

val emptyPlaces = mutableListOf<Element>()

fun updateValues(code: String, elements: SnapshotStateList<Element>) {

    fun calc(data: String): String {
        return try {
            calcMath(data)
        } catch (e: Exception) {
            e.printStackTrace()
            unknown
        }
    }

    // detect value changed
    val source = StringBuilder(code)

    fun collectGarbage() {
        // сборка мусора :)
        // элементы которые мы удалили в коде - удаляем и на макете
        val removedEntries = namesMap.entries.filter {
            val name = it.value
            !source.contains(name) || name.isBlank()
        }

        for (entry in removedEntries) {
            namesMap.remove(entry.key)
        }
        println("namesMap: ${namesMap}")
        val removed = elements.filter { !namesMap.values.contains(it.name) }

        emptyPlaces.addAll(removed)

        println("elements: ${elements.map { it.name }}")
        println("removed: ${removed.map { it.name }}")
        elements.removeAll(removed)
        println("elements: ${elements.map { it.name }}")
    }

    collectGarbage() // изменили код - собрали мусор

    val lines = source.split("\n")
    lines.forEachIndexed { i, line ->
        // если нашли знак = и справа от него имя,
        // то наполняем словарь тем что слева от =
        val namingOp = " = "
        val index = line.lastIndexOf(namingOp)

        if (index != -1
            && line.trim().length > 1
            && index < line.length - 1
        ) {
            var left = line.substring(0, index)
            val right = line.substring(index + namingOp.length, line.length).trim()

            var totalTextIndex = 0
            var pointer = i - 1
            while (pointer >= 0) {
                totalTextIndex += lines[pointer].length + 1
                pointer--
            }
            totalTextIndex += index + 1

            namesMap.firstNotNullOfOrNull {
                if (it.value == right && it.key != totalTextIndex)
                    it
                else
                    null
            }?.let {
                namesMap.remove(it.key)
            }
            namesMap[totalTextIndex] = right

            collectGarbage() // изменили код - собрали мусор

            if (left.trim() == "}") {
                var closedBracketIndex = 0
                var pointer = i - 1
                while (pointer >= 0) {
                    closedBracketIndex += lines[pointer].length + 1
                    pointer--
                }

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

                left = source.substring(openBracketIndex + 1, closedBracketIndex)
            }

            var calculatedLines = ""
            left.split("\n").forEach { leftLine ->
                val valueLine = StringBuilder(leftLine)

                // подставляем значения по имени
                var startValueIndex = valueLine.indexOf("$")
                var endValueIndex = valueLine.positionOf({
                    ends.contains(it)
                }, startValueIndex + 1)
                if (endValueIndex == -1) {
                    endValueIndex = valueLine.length
                }
                while (startValueIndex != -1) {
                    if (startValueIndex + 1 != endValueIndex) {

                        val value = StringBuilder(valueLine.substring(startValueIndex + 1, endValueIndex))

                        elements.sortedByDescending { it.name.length } // для того чтобы R не подставлялся в Result
                            .forEach {
                                val nameIndex = value.indexOf(it.name)
                                val elementValue = it.value.trim().replace("\n", "")

                                if (nameIndex != -1 && elementValue != unknown) {
                                    value.replace(nameIndex, nameIndex + it.name.length, "")
                                    value.insert(nameIndex, elementValue)
                                }
                            }

                        valueLine.replace(
                            startValueIndex, endValueIndex + 1, value.toString()
                        )
                    }
                    startValueIndex = valueLine.indexOf("$", endValueIndex + 1)
                    endValueIndex = valueLine.positionOf({
                        ends.contains(it)
                    }, endValueIndex + 1)
                    if (endValueIndex == -1) {
                        endValueIndex = valueLine.length
                    }
                }

                // высчитываем и подставляем математические операции
                var startMathIndex = valueLine.indexOf("{", 0)
                var endMathIndex = valueLine.indexOf("}", 0)
                while (startMathIndex != -1 && endMathIndex != -1) {
                    if (startMathIndex + 1 != endMathIndex) {
                        val value = StringBuilder(valueLine.substring(startMathIndex + 1, endMathIndex))

                        elements.sortedByDescending { it.name.length } // для того чтобы R не подставлялся в Result
                            .forEach {
                                val nameIndex = value.indexOf(it.name)
                                val elementValue = it.value.trim().replace("\n", "")
                                if (nameIndex != -1 && elementValue != unknown) {
                                    value.replace(nameIndex, nameIndex + it.name.length, elementValue)
                                }
                            }

                        val calcResult = calc(value.toString())
                        valueLine.replace(
                            startMathIndex, endMathIndex + 1,
                            calcResult
                        )
                    }

                    startMathIndex = valueLine.indexOf("{", endMathIndex + 1)
                    endMathIndex = valueLine.indexOf("}", endMathIndex + 1)
                }

                // высчитываем и подставляем операции со строками/массивами
                // слева направо
                val startArrayOpIndex = valueLine.indexOf("[")
                val endArrayOpIndex = valueLine.indexOf("]")

                val startArrayIndex = endArrayOpIndex + 1
                var endArrayIndex = valueLine.positionOf({
                    ends.contains(it)
                }, startArrayIndex)
                if (endArrayIndex == -1) {
                    endArrayIndex = valueLine.length
                }

                if (startArrayOpIndex != -1 && endArrayOpIndex != -1) {
                    val op = valueLine.substring(startArrayOpIndex + 1, endArrayOpIndex)
                    val value = valueLine.substring(startArrayIndex, endArrayIndex)

                    when {
                        // переворачиваем данные(инвертируем порядок)
//                      [!]abcd
                        op == "!" -> valueLine.replace(startArrayOpIndex, endArrayIndex, value.toString().reversed())

                        // копируем элемент по номеру места
//                      [2]abcd
                        op.isDigitsOnly() -> valueLine.replace(
                            startArrayOpIndex,
                            endArrayIndex,
                            "${value[op.toInt() - 1]}" // счет мест для заполнения начинается с 1-го
                        )

                        !op.startsWith("-") && !op.contains("<-") && op.contains("..") -> {
                            val leftRight = op.split("..")
                            val from = leftRight[0].trim().toInt() - 1
                            val to = leftRight[1].trim().toInt()
                            if (leftRight.size == 2) {
                                valueLine.replace(
                                    startArrayOpIndex,
                                    endArrayIndex,
                                    value.substring(from, to) // счет мест для заполнения начинается с 1-го
                                )
                            }
                        }

                        // удаляем элемент по номеру места
//                      [-2]abcd
                        op.startsWith("-") && op.isDigitsOnly('-') -> valueLine.replace(
                            startArrayOpIndex,
                            endArrayIndex,
                            value.removeRange(
                                abs(op.toInt()) - 1,
                                abs(op.toInt())
                            ) // счет мест для заполнения начинается с 1-го
                        )

                        op.startsWith("-") && !op.contains("<-") && op.contains("..") -> {
                            val leftRight = op.removePrefix("-")
                                .split("..")
                            val from = abs(leftRight[0].trim().toInt()) - 1
                            val to = leftRight[1].trim().toInt()
                            if (leftRight.size == 2) {
                                valueLine.replace(
                                    startArrayOpIndex,
                                    endArrayIndex,
                                    value.removeRange(from, to) // счет мест для заполнения начинается с 1-го
                                )
                            }
                        }

                        // заполняем ячейку элемента данными по номеру места
//                      [2 <- x]abcd
                        op.contains(" <- ") && !op.contains("..") -> {
                            val leftRight = op.split(" <- ", limit = 2)
                            if (leftRight.size == 2) {
                                val i = leftRight[0].trim().toInt() - 1 // счет мест начинается с 1-го
                                var v = leftRight[1].trim()
                                if (v.startsWith("\"") && v.endsWith("\"")) {
                                    v = v.removeSuffix("\"")
                                        .removePrefix("\"")
                                }
                                valueLine.replace(
                                    startArrayOpIndex,
                                    endArrayIndex,
                                    value.replaceRange(i, i + 1, v)
                                )
                            }
                        }

                        op.contains(" <- ") && op.contains("..") -> {

                            val leftRightSet = op.split(" <- ", limit = 2)
                            if (leftRightSet.size == 2) {
                                val leftRightRange = op.split("..")
                                val from = leftRightRange[0].trim().toInt() - 1
                                val to = leftRightRange[1].trim().split(" ").first().toInt()

                                var v = leftRightSet[1].trim()
                                if (v.startsWith("\"") && v.endsWith("\"")) {
                                    v = v.removeSuffix("\"")
                                        .removePrefix("\"")
                                }
                                valueLine.replace(
                                    startArrayOpIndex,
                                    endArrayIndex,
                                    value.replaceRange(from, to, v)
                                )
                            }
                        }
                    }
                }

                calculatedLines += valueLine.toString() + "\n"
            }
            left = calculatedLines


            val elementName = right.trim()
            val elementValue = left
            val elementIndex = elements.indexOfFirst {
                it.name == elementName
            }

            if (elementIndex != -1) {
                if (elements[elementIndex].value != elementValue) {
                    println("e: updated value: $elementName")
                    elements[elementIndex] = elements[elementIndex].copy(value = elementValue, index = totalTextIndex)
                }

            } else {
                // ренэйм элемента,
                //  чтобы при редактировании не создавалось куча новых элементов
                val editedElementIndex = elements.indexOfFirst { it.index == totalTextIndex }

                if (editedElementIndex != -1) {
                    println("e: edited name: $elementName")
                    val editedElement = elements[editedElementIndex]
                    elements[editedElementIndex] =
                        editedElement.copy(name = elementName, value = elementValue)

                } else if (!elementName.isBlank()) {
                    println("e: added new element: $elementName")
                    // добавление новой абстракции


                    val stubElement = emptyPlaces.minByOrNull { it.area.top }
                    var area = stubElement?.area
                    if (area == null) {
                        val nextPlaceIndex = elements.size
                        val x = layoutRect.width - (100f * (nextPlaceIndex / 7 + 1))
                        val row = nextPlaceIndex % 7
                        val y = row * 80f + 10f + 6 * row

                        area = Rect(
                            Offset(x, y),
                            Offset(
                                x + 80f, y + 80f
                            )
                        )

                    } else {
                        emptyPlaces.remove(stubElement)
                    }
                    val abstractElement = Element(
                        name = elementName,
                        value = elementValue,
                        area = area,
                        color = stubElement?.color ?: currentColor.color.also {
                            currentColor = nextColor()
                        },
                        isCircle = false,
                        isAbstract = true,
                        index = totalTextIndex
                    )

                    elements.add(abstractElement)
                }
            }
        }
    }
}