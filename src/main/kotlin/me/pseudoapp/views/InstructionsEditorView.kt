package me.pseudoapp.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
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
import kotlinx.coroutines.delay
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
    newElement: MutableState<Element?>,
) {
    val instructionsRequester = remember { FocusRequester() }
    var prevCodeValue by remember { mutableStateOf(TextFieldValue()) }
    var codeValue by remember { mutableStateOf(TextFieldValue()) }
    var isPaused by remember { mutableStateOf(false) }

    var isCodeCompletionEnabled by remember { mutableStateOf(false) }
    var textFieldPosition by remember { mutableStateOf(Offset.Zero) }
    var cursorOffsetInTextField by remember { mutableStateOf(Offset.Zero) }

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

        updateValues(
            codeValue.text,
            codeValue.selection.end,
            elements,
            newElement.value
        ) { _, _ ->
            // do nothing
        }
        newElement.value = null
    }

    LaunchedEffect(codeValue) {
        if (!isPaused) {
            delay(210)
            try {
                if (prevCodeValue.text != codeValue.text) {
                    updateValues(
                        codeValue.text,
                        codeValue.selection.end,
                        elements,
                        newElement.value
                    ) { position, newCode ->
                        codeValue = codeValue.copy(text = newCode, selection = TextRange(position))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
                    prevCodeValue = codeValue
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

            Row(Modifier.align(Alignment.BottomEnd)) {
                IconToggleButton(
                    isPaused,
                    onCheckedChange = {
                        isPaused = it
                        if (!isPaused) {
                            try {
                                updateValues(
                                    codeValue.text,
                                    codeValue.selection.end,
                                    elements,
                                    newElement.value
                                ) { _, _ ->
                                    // do nothing
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    content = {
                        if (isPaused)
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


val ends = listOf(
    "$", " ", "+", "-", "*", "/", "%", "=", "{", "(", ",", ";", "\n", "\t", "}", ")", "[", "]"
)

val emptyAbstractPlaces = mutableListOf<Element>()

private fun String.firstContained(items: List<String>): String {
    items.forEach {
        if (this.contains(it)) return it
    }

    return ""
}

fun updateValues(
    code: String,
    cursorPosition: Int,
    elements: SnapshotStateList<Element>,
    newElement: Element?,
    onCodeUpdate: (Int, String) -> Unit
) {

    fun calc(data: String): String {
        return try {
            calcMath(data)
        } catch (e: Exception) {
            e.printStackTrace()
            unknown
        }
    }

    val namesMap = mutableMapOf<Int, String>() // <Index, Name>
    // detect value changed
    val source = StringBuilder(code)

    fun collectGarbage() {
        // сборка мусора :)
        // элементы которые мы удалили в коде - удаляем и на макете
        val removedEntries = namesMap.entries.filter {
            val name = it.value
            !source.contains(name) || name.isBlank()
        }
        println("namesMap a: ${namesMap}")
        for (entry in removedEntries) {
            namesMap.remove(entry.key)
        }
        println("namesMap b: ${namesMap}")
        val removed = elements.filter {
            it.name != newElement?.name
                    && !namesMap.values.contains(it.name)
        }

        emptyAbstractPlaces.addAll(removed.filter { it.isAbstract })

        println("elements: ${elements.map { it.name }}")
        println("removed: ${removed.map { it.name }}")
        elements.removeAll(removed)
        println("elements: ${elements.map { it.name }}")
    }

    collectGarbage() // изменили код - собрали мусор

    //  Step: обрабатываем каждую линию

    val lines = source.split("\n")
    lines.forEachIndexed { i, it ->
        println("it 1: $it")
        val namingOp = " = "

        var line = it

        var totalTextIndex = 0
        var pointer = i - 1
        while (pointer >= 0) {
            totalTextIndex += lines[pointer].length + 1
            pointer--
        }


        fun calculateValues(data: String): String {
            //  Step: Обрабатываем значения (то что слева от = )
            var calculatedLines = ""
            data.split("\n").forEach { leftLine ->
                val valueLine = StringBuilder(leftLine)

                //  Step: подставляем значения по имени
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
                            startValueIndex, endValueIndex, value.toString()
                        )
                    }
                    startValueIndex = valueLine.indexOf("$", endValueIndex)
                    endValueIndex = valueLine.positionOf({
                        ends.contains(it)
                    }, startValueIndex + 1)
                    if (endValueIndex == -1) {
                        endValueIndex = valueLine.length
                    }
                }

                //  Step: высчитываем и подставляем математические операции
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

                //  Step: высчитываем и подставляем операции со строками/массивами
                // слева направо

                var startArrayOpIndex = valueLine.indexOf("[")
                var endArrayOpIndex = valueLine.indexOf("]")
                while (startArrayOpIndex != -1) {
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
                            op == "!" -> valueLine.replace(
                                startArrayOpIndex,
                                endArrayIndex,
                                value.toString().reversed()
                            )

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

                    startArrayOpIndex = valueLine.indexOf("[", endArrayOpIndex + 1)
                    endArrayOpIndex = valueLine.indexOf("]", endArrayOpIndex + 1)
                }

                calculatedLines += valueLine.toString() + "\n"
            }

            return calculatedLines
        }

        // Step: Условный оператор // 1 == 1 ? true : false
        val booleanOps = listOf(
            " == ", " != ", " < ", " > ", " <= ", " >= "
        )

        val conditionOp = line.firstContained(booleanOps)
        println("conditionOp: $conditionOp")
        if (conditionOp.isNotEmpty()) {
            val conditionStartIndex = 0 //totalTextIndex + 1

            val yesOp = " ? "
            val noOp = " : "
            val endOp = "."
            val yesIndex = line.indexOf(yesOp, conditionStartIndex)
            val noIndex = line.indexOf(noOp, yesIndex)
            val endIndex = line.indexOf(endOp, noIndex)


            var condition = line.substring(conditionStartIndex, yesIndex)
            condition = calculateValues(condition)

            println("condition: $condition")
            val yesValue = line.substring(yesIndex + yesOp.length, if (noIndex == -1) endIndex else noIndex)
            println("yesValue: $yesValue")
//            calculateValues(yesValue)
//            if(yesValue.contains("\n")) { // если линий больше 1-й
//
//            }

            val noValue = if (noIndex != -1) line.substring(noIndex + noOp.length, endIndex) else ""
            println("noValue: $noValue")
//            calculateValues(noValue)
//            if(noValue.contains("\n")) { // если линий больше 1-й
//
//            }

            val leftRight = condition.split(conditionOp)
            when (conditionOp) {
                " != " -> {
                    val l = leftRight[0].trim()
                    val r = leftRight[1].trim()

                    println("l = |$l|, r = |$r|")

                    val yes = l != r
                    line = if (yes) yesValue else noValue + ""
                }

                " == " -> {
                    val l = leftRight[0].trim()
                    val r = leftRight[1].trim()

                    println("l = |$l|, r = |$r|")

                    val yes = l == r
                    line = if (yes) yesValue else noValue + ""
                }

                " > " -> {
                    val l = leftRight[0].trim()
                    val r = leftRight[1].trim()

                    if (l.isDigitsOnly('.', '-') && r.isDigitsOnly('.', '-')) {
                        line = if (l.toInt() > r.toInt()) yesValue else noValue
                    } else {
                        line = if (l.length > r.length) yesValue else noValue
                    }
                }

                " < " -> {
//                    condition: 1 < [1]12945
                    println("<<<<<<<<<<<")
                    val l = leftRight[0].trim()
                    val r = leftRight[1].trim()

                    if (l.isDigitsOnly('.', '-') && r.isDigitsOnly('.', '-')) {
                        println("a l = |$l|, r = |$r|")
                        line = if (l.toInt() < r.toInt()) yesValue else noValue
                    } else {
                        println("b l = |$l|, r = |$r|")
                        line = if (l.length < r.length) yesValue else noValue
                    }
                }
            }
        }
        println("line 0: $line")

        val recalculatingOp = " => "
        val recalculatingOpIndex = line.lastIndexOf(recalculatingOp)

        // Step: если нашли знак = и справа от него имя,
        // то наполняем словарь тем что слева от =


        val namingOpIndex = line.lastIndexOf(namingOp)

        var calcOp = ""
        var calcOpIndex = -1
        if (namingOpIndex != -1) {
            calcOp = namingOp
            calcOpIndex = namingOpIndex

        } else if (recalculatingOpIndex != -1) {
            calcOp = recalculatingOp
            calcOpIndex = recalculatingOpIndex
        }

        println("line 1: $line")
        if (line.trim().length > 1
            && calcOpIndex != -1
            && calcOpIndex + calcOp.length != line.length
        ) {
            println("line 2: $line")
            println("line: $line")
            //  Step: Обрабатываем именование
            // если слева однострочное значение

            var left = line.substring(0, calcOpIndex)
            val right = line.substring(calcOpIndex + calcOp.length, line.length).trim()

            totalTextIndex += calcOpIndex + 1
            // если слева многострочное значение
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

            fun isLastRecalculation(): Boolean {
                val nextRecalculatingIndex =
                    source.indexOf(recalculatingOp + right, totalTextIndex + recalculatingOp.length)
                val isLastRecalculation = nextRecalculatingIndex == -1
                return true //isLastRecalculation
            }


            if (calcOp != recalculatingOp || isLastRecalculation()) {
                left = calculateValues(left)

                // Step: обновляем элементы
                val elementName = right.trim()
                val elementValue = left.replace("\n", "")
                val elementIndex = elements.indexOfFirst {
                    it.name == elementName
                }

                println("elementValue: $elementValue")
                val isNaming = namingOpIndex != -1
                val isRecalculating = recalculatingOpIndex != -1

                if (isNaming) {
                    // Step: Обновляем элементы
                    if (elementIndex != -1) {
                        if (elements[elementIndex].value != elementValue) {
                            println("e: updated value: $elementName")
                            elements[elementIndex] =
                                elements[elementIndex].copy(value = elementValue, index = totalTextIndex)
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


                            val stubElement = emptyAbstractPlaces.minByOrNull { it.area.top }
                            var area = stubElement?.area
                            if (area == null) {
                                val nextPlaceIndex = elements.count { it.isAbstract }
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
                                emptyAbstractPlaces.remove(stubElement)
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

                } else if (isRecalculating) {
                    // Step: Переписываем исходное значение в коде и пересчитывваем
                    val firstNamingIndex = source.indexOf(namingOp + elementName)
                    var firstValueStartIndex = source.positionOf(
                        {
                            it == "\n"
                        },
                        startIndex = firstNamingIndex,
                        fromEndToStart = true
                    ) + 1
                    if (firstValueStartIndex == -1) {
                        firstValueStartIndex = 0
                    }

//                    val firstValue = source.substring(firstValueStartIndex, firstNamingIndex)
                    source.replace(firstValueStartIndex, firstNamingIndex, elementValue)

                    val newPosition = cursorPosition + source.length - code.length
                    onCodeUpdate(newPosition, source.toString())
                }
            }
        }
    }
}