package me.pseudoapp.views

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.github.murzagalin.evaluator.Evaluator
import me.pseudoapp.Element
import me.pseudoapp.other.firstContained
import me.pseudoapp.other.isDigitsOnly
import me.pseudoapp.other.positionOf
import me.pseudoapp.other.split
import kotlin.math.abs


val mathEvaluator = Evaluator()
var activeElement: Element? = null

val questionOp = "? "

val setOp = "=> "
val againOp = "=^ "
val doneOp = "=done"
val pauseOp = "=pause"
val stepDelayOp = "=stepDelay"

val innerValueDelimiter = ":"

val nameOp = ":name"
val colorOp = ":color"

val xOp = ":x"
val yOp = ":y"

val widthOp = ":w"
val heightOp = ":h"

val unknownResult = "?"


fun calcInstructions(
    element: Element,
    elements: SnapshotStateList<Element>,
    starters: SnapshotStateList<Element?>,
    stepDelayMsValue: MutableState<Long>
): CalcState {
    // program is done ? see: doneOp
    activeElement?.inProgress?.value = false
    activeElement = element
    activeElement?.inProgress?.value = true
    println("active: action: ${activeElement?.text?.value} | value: ${activeElement?.result?.value}")

    if (!element.isAbstrAction) {
        return CalcState.InProgress
    }

    element.elements.forEach { e ->
        calcInstructions(e, e.elements, starters, stepDelayMsValue)
    }

    fun doInsertAndCalcValues(a: CharSequence): CharSequence {
        val action = StringBuilder(a)
        val opIndex = action.indexOf(" =")

        //  Step: подставляем значения внутренних элементов
        var index = action.indexOf(innerValueDelimiter)
        var endIndex = action.indexOf(" ", index)

        while (index < opIndex && index != -1 && endIndex != -1 && index + 1 != endIndex) {
            val a = action.substring(index + 1, endIndex)
            val b = element.elements.firstOrNull { it.name.value == a }?.result?.value

            b?.let {
                action.replace(index, endIndex, b)
            }
            index = action.indexOf(innerValueDelimiter, endIndex)
            endIndex = action.indexOf(" ", index)
        }

        //  Step: подставляем значения соседних элементов
        elements.forEach { e ->
            val name = e.name.value
            if (!name.isEmpty()) {
                index = action.indexOf(name)
//                index = max(0, action.positionOf(" ", startIndex = index, fromEndToStart = true))
                endIndex = action.indexOfAny(charArrayOf(' ', '['), index)
                val b = elements.firstOrNull { it.name.value == name }?.result?.value
                while (index < opIndex && index != -1 && endIndex != -1 && index != endIndex && endIndex - index == name.length) {
                    val replaced = action.substring(index, endIndex)
                    println("replaced: $replaced")

                    b?.let {
                        action.replace(index, endIndex, b)
                    }
                    index = action.indexOf(name, endIndex)
//                    index = max(0, action.positionOf(" ", startIndex = index, fromEndToStart = true))
                    endIndex = action.indexOf(" ", index)
                }
            }
        }

        //  Step: высчитываем и подставляем операции со строками/массивами
        // слева направо
        var startArrayOpIndex = action.indexOf("[")
        var endArrayOpIndex = action.indexOf("]")

        val isLeftSide = startArrayOpIndex == 0

        while (startArrayOpIndex < opIndex && startArrayOpIndex != -1 && endArrayOpIndex != -1) {
            val startArrayIndex = endArrayOpIndex + 1
            var endArrayIndex = if (isLeftSide)
                action.positionOf(
                    { it == "=" },
                    startIndex = endArrayOpIndex
                ) - 1 // [?.]abc| =|> q
            else
                action.positionOf(
                    { it == "[" },
                    startIndex = endArrayOpIndex
                )

            val startReplacedIndex = if (isLeftSide) startArrayOpIndex else 0

            if (endArrayIndex == -1) {
                endArrayIndex = action.length
            }

// благодарность дается даром

            val op = action.substring(
                startArrayOpIndex + 1,
                endArrayOpIndex
            )
            val array = (if (isLeftSide)
                action.split("]")[1] // [op]array
            else
                action.split("[")[0]) // array[op]
                .split(" =")[0]

            println("[arrays] op: $op | array: $array")


            // [temp -> +3] // вставить на 3 место, остальные сдвинуть
            // [temp <- 3] // переместить в temp


            // [temp .->. ab] // заменить все ab содержимым temp . до .
            // [temp 2->10 ab] // заменить  ab содержимым temp от 2й до 10й ячейки
            // [temp 0->3] // заполнить все до ячейки 3 содержимым temp

            // [temp <- 3:, ] // переместить в temp
            // [temp -> 3:, ] // заполнить ячейку 3 содержимым temp
            // [temp -> +3:, ] // вставить на 3 место, остальные сдвинуть


            // ===
            // [3] // копировать ячейку 3
            // [3..4]

            // [2:,] // копировать ячейку 2 с разделителем

            // [-3] // копировать все за исключением ячейки 3
            // [-3..4]
            // [-2:,] // копировать все за исключением ячейки 2 с разделителем

            // ===
            // [a?;] // кол-во до разделителя или 0
            // [a^|] // индекс до разделителя или 0


            when {
                // заполнить ячейку 3 содержимым x
                // Case: [x ->3]abc
                // Case: [x 1->3]abc


                // [x -> ?a :, ]a, a, c => Result
                // [x -> 1..2 ?a :|]a|b|c => Result

                // [x -> {..} ? , ]{abc}de => Result

                op.contains(" -> ") -> {
                    val valueAndRange = op.split(" -> ", limit = 2)
                    if (valueAndRange.size > 1) {
                        // [{чем заменяем} -> ..{где заменяем} ?{что заменяем} :{разделитель} ]abc => Result
                        var value = valueAndRange[0].trim()
                        if (value.startsWith("\"") && value.endsWith("\"")) { // without trim()
                            value = value.removeSuffix("\"").removePrefix("\"")
                        }
                        val whereAndWhatAndDelimiter = valueAndRange[1].trim().split(" ", limit = 3)
                        val where = whereAndWhatAndDelimiter.firstOrNull { it.contains("..") }
                            ?: whereAndWhatAndDelimiter.firstOrNull { it.isDigitsOnly() }
                        val what = whereAndWhatAndDelimiter.firstOrNull { it.startsWith("?") }?.drop(1) ?: ""
                        val separator = whereAndWhatAndDelimiter.firstOrNull { it.startsWith(":") }?.drop(1) ?: ""

                        println("whereAndWhatAndDelimiter: $whereAndWhatAndDelimiter")
                        val rangeStartAndEnd = where?.split("..") ?: listOf("")
                        println("rangeStartAndEnd: $rangeStartAndEnd")

//                        if(isDigit) {
                            var start = if (rangeStartAndEnd[0].isEmpty())
                                0
                            else
                                rangeStartAndEnd[0].trim().toInt() - 1   // счет мест начинается с 1-го
                            if (start < 0) {
                                start = 0
                            }


                            var end = when {
                                rangeStartAndEnd[0].isEmpty() -> {
                                    array.length
                                }

                                rangeStartAndEnd.size == 1 -> {
                                    start + 1
                                }

                                else -> {
                                    rangeStartAndEnd[1].trim().toIntOrNull()
                                        ?: array.length
                                }
                            }
                            if (end > array.length) {
                                end = array.length
                            }
//                        } else {
//                            isLetter
//                        }
                        println("start: $start, end: $end")

                        val source = array.split(separator).toMutableList()
                        val result = if (what.isEmpty()) {
                            array.replaceRange(
                                start + if (separator.isEmpty() || start == 0) 0 else (start) * separator.length,
                                end + if (separator.isEmpty()) 0 else (end - 1) * separator.length,
                                value
                            )
                        } else {
                            for (i in start until if (end > source.size) source.size else end) {
                                source[i] = source[i].replace(what, value)
                            }
                            source.joinToString(separator)
                        }

                        action.replace(startReplacedIndex, endArrayIndex, result)
                    }
                }

                // переместить подстроку по номеру между разделителями
                // [temp <- 3:|]1|2|3,|abc|5 // temp == 3, | source == 1|2|abc|5
                // [temp <- 2]1|2|3 // temp == |, | source == 12|3
                // [temp <- 2..4]1|2|3 // temp == |2|, | source == 13
                op.contains("<-") -> {

                    val lr = op.split(":")
                    var position = abs(lr[0].toInt())
                    val delimiter = lr[1]
                    position -= if (delimiter.isEmpty()) 0 else 1

                    val substrings = array.split(delimiter)
                    val reduced = substrings.reduceIndexed { i, a, b ->
                        a + if (i == position) "" else delimiter + b
                    }

                    println("reduced: $reduced")

                    action.replace(
                        startReplacedIndex, endArrayIndex,
                        if (position >= substrings.size || position < 0)
                            ""
                        else {
                            reduced
                        }
                    )
                }

                // удалить

                // копировать

                // удалить подстроку по номеру между разделителями
                // [-2:, ]1, 2, 3, 4, 5 // 1, 3, 4, 5
                // [-3:|]AA|BB|A,|BAB,|A // AA|BB|BAB,|A
                op.startsWith("-") && op.contains(":") -> {
                    val lr = op.split(":")
                    var position = abs(lr[0].toInt())
                    val delimiter = lr[1]
                    position -= if (delimiter.isEmpty()) 0 else 1

                    val substrings = array.split(delimiter)
                    val reduced = substrings.reduceIndexed { i, a, b ->
                        a + if (i == position) "" else delimiter + b
                    }

                    println("reduced: $reduced")

                    action.replace(
                        startReplacedIndex, endArrayIndex,
                        if (position >= substrings.size || position < 0)
                            ""
                        else {
                            reduced
                        }
                    )
                }

                // копировать подстроку по номеру между разделителями
                // [2:, ]AA, BB|, A, BAB, A // BB|
                // [3:|]AA|BB|A,|BAB,|A // A,
                op.contains(":") -> {
                    val lr = op.split(":")
                    var position = lr[0].toInt()
                    val delimiter = lr[1]
                    position -= if (delimiter.isEmpty()) 0 else 1

                    val substrings = array.split(delimiter)

                    action.replace(
                        startReplacedIndex, endArrayIndex,
                        if (position >= substrings.size || position < 0) ""
                        else substrings[position]
                    )
                }

                // сколько элементов до разделителя ?.
                // [A?|.]AABB|ABABA // 2 элемента A
                // [?.]AABB|ABABA // 10 элементов всего
                op.contains("?") && op.endsWith(".") -> {
                    val lr = op.split("?")
                    val query = lr[0]
                    val delimiter = lr[1].trimEnd('.')
                    if (query.isEmpty() && delimiter.isEmpty()) {
                        action.replace(
                            startReplacedIndex, endArrayIndex, array.length.toString()
                        )
                    } else {
                        var count = 0
                        var index = array.indexOf(query)
                        val delimiterIndex = array.indexOf(delimiter)
                        while (index != -1 && (index < delimiterIndex || delimiterIndex == -1)) {
                            count += 1
                            index = array.indexOf(query, index + 1)
                        }

                        println("action a: $action")
                        action.replace(
                            startReplacedIndex, endArrayIndex, count.toString()
                        )
                        println("action b: $action")
                    }
                }

                // переворачиваем данные(инвертируем порядок)
                // [!]abcd
                op == "!" -> {
                    action.replace(
                        startReplacedIndex, endArrayIndex, array.reversed()
                    )
                }

                // копируем элемент по номеру места
                // [2]abcd
                op.isDigitsOnly() -> {
                    action.replace(
                        startReplacedIndex,
                        endArrayIndex,
                        "${array[op.toInt() - 1]}" // счет мест для заполнения начинается с 1-го
                    )
                }

                // копируем ряд по номеру места
                // [2..5]abcd
                !op.startsWith("-") && !op.contains("->") && op.contains("..") -> {
                    val leftRight = op.split("..")
                    val from = leftRight[0].trim().toInt() - 1
                    val to = leftRight[1].trim().toInt()
                    if (leftRight.size == 2) {
                        action.replace(
                            startReplacedIndex,
                            endArrayIndex,
                            array.substring(from, to) // счет мест для заполнения начинается с 1-го
                        )
                    }
                }

                // удаляем элемент по номеру места
                // [-2]abcd
                op.startsWith("-") && op.isDigitsOnly('-') -> {
                    //                            / заменить оригинал
                    action.replace(
                        startReplacedIndex, endArrayIndex, array.removeRange(
                            abs(op.toInt()) - 1, abs(op.toInt())
                        ) // счет мест для заполнения начинается с 1-го
                    )
                }

                // добавляем элемент по номеру места (остальные сдвигаются)
                // [x -> +2]abcd // axbcd
                // todo: [x -> +2:, ]a, b, c, d // a, x, b, c, d
                op.contains(" -> +") && !op.contains("..") -> {
                    val leftRight = op.split(" -> +")
                    val v = leftRight[0].trim()
                    val i = leftRight[1].trim().toInt() - 1 // счет начинается с 1-го
                    action.replace(
                        startReplacedIndex, endArrayIndex, StringBuilder(array).insert(i, v).toString()
                    )
                }

                // удаляем ряд по номеру
                // [-2..5]abcd
                op.startsWith("-") && !op.contains("->") && op.contains("..") -> {
                    val leftRight = op.removePrefix("-").split("..")
                    val from = abs(leftRight[0].trim().toInt()) - 1
                    val to = leftRight[1].trim().toInt()
                    if (leftRight.size == 2) {
                        action.replace(
                            startReplacedIndex,
                            endArrayIndex,
                            array.removeRange(from, to) // счет мест для заполнения начинается с 1-го
                        )
                    }
                }
            }

            startArrayOpIndex = action.indexOf("[", endArrayOpIndex + 1)
            endArrayOpIndex = action.indexOf("]", endArrayOpIndex + 1)
        }

        fun replaceOp(action: StringBuilder, op: String, insertion: (value: String) -> String) {
            var sizeOpEndIndex = action.indexOf(op)
            var sizeOpStartIndex = action.positionOf(" ", sizeOpEndIndex, fromEndToStart = true) + 1

            while (sizeOpEndIndex < opIndex && sizeOpStartIndex != -1 && sizeOpEndIndex != -1) {
                val value = action.substring(sizeOpStartIndex, sizeOpEndIndex)

                action.replace(sizeOpEndIndex, sizeOpEndIndex + op.length, "")
                action.replace(sizeOpStartIndex, sizeOpEndIndex, insertion(value))

                sizeOpEndIndex = action.indexOf(op, sizeOpEndIndex + 1)
                sizeOpStartIndex = action.positionOf(" ", sizeOpEndIndex, fromEndToStart = true) + 1
            }
        }

        // Step: Подставляем цвета элементов
        replaceOp(action, colorOp) { value ->
            val e = elements.firstOrNull { it.name.value == value }
            return@replaceOp e?.let { e.color.value.toString() } ?: unknownResult //todo: print hex value
        }

        // Step: Подставляем ширину элементов
        replaceOp(action, widthOp) { value ->
            val e = elements.firstOrNull { it.name.value == value }
            return@replaceOp e?.let { e.area.value.width.toString() } ?: unknownResult
        }

        // Step: Подставляем высоту элементов
        replaceOp(action, heightOp) { value ->
            val e = elements.firstOrNull { it.name.value == value }
            return@replaceOp e?.let { e.area.value.height.toString() } ?: unknownResult
        }

        // Step: Подставляем x элементов
        replaceOp(action, xOp) { value ->
            val e = elements.firstOrNull { it.name.value == value }
            return@replaceOp e?.let { e.area.value.left.toString() } ?: unknownResult
        }

        // Step: Подставляем y элементов
        replaceOp(action, yOp) { value ->
            val e = elements.firstOrNull { it.name.value == value }
            return@replaceOp e?.let { e.area.value.top.toString() } ?: unknownResult
        }

        // Step: Подставляем stepDelay элементов
        replaceOp(action, stepDelayOp) { value ->
            val e = elements.firstOrNull { it.name.value == value }
            return@replaceOp e?.let { stepDelayMsValue.value.toString() } ?: unknownResult
        }

        element.result.value = action.toString()
        println("result action: $action")

        return action
    }

    var singleAction = StringBuilder(doInsertAndCalcValues(element.text.value))
    println("singleAction: $singleAction")

    var needToDo = true
    val questionLeftRight = singleAction.split(questionOp, false, 2)
    if (!singleAction.startsWith("[$questionOp")
        && (singleAction.contains(questionOp) || singleAction.contains("?\n"))
    ) {
        val condition = questionLeftRight[0]
        val right = questionLeftRight.getOrNull(1) ?: ""
        singleAction = StringBuilder(right)

        if (condition.isBlank()) {
            needToDo = true

        } else {

            val booleanOps = listOf(
                " == ", " != ", " < ", " > ", " <= ", " >= "
            )

            val conditionOp = condition.firstContained(booleanOps)

            val leftRight = condition.split(conditionOp)
            when (conditionOp) {
                " != " -> {
                    val l = leftRight[0].trim()
                    val r = leftRight[1].trim()

                    println("?: |$l| $conditionOp |$r|")

                    needToDo = l != r
                }

                " == " -> {
                    val l = leftRight[0].trim()
                    val r = leftRight[1].trim()

                    println("?: |$l| $conditionOp |$r|")

                    needToDo = l == r
                }

                " > " -> {
                    val l = leftRight[0].trim()
                    val r = leftRight[1].trim()

                    println("?: |$l| $conditionOp |$r|")

                    if (l.isDigitsOnly('.', '-') && r.isDigitsOnly('.', '-')) {
                        needToDo = l.toDouble() > r.toDouble()
                    } else {
                        needToDo = false
                    }
                }

                " >= " -> {
                    val l = leftRight[0].trim()
                    val r = leftRight[1].trim()

                    println("?: |$l| $conditionOp |$r|")

                    if (l.isDigitsOnly('.', '-') && r.isDigitsOnly('.', '-')) {
                        needToDo = l.toDouble() >= r.toDouble()
                    } else {
                        needToDo = false
                    }
                }

                " < " -> {
//                    condition: 1 < [1]12945
                    val l = leftRight[0].trim()
                    val r = leftRight[1].trim()

                    println("?: |$l| $conditionOp |$r|")

                    if (l.isDigitsOnly('.', '-') && r.isDigitsOnly('.', '-')) {
                        needToDo = l.toDouble() < r.toDouble()
                    } else {
                        needToDo = false
                    }
                }

                " <= " -> {
//                    condition: 1 < [1]12945
                    val l = leftRight[0].trim()
                    val r = leftRight[1].trim()

                    println("?: |$l| $conditionOp |$r|")

                    if (l.isDigitsOnly('.', '-') && r.isDigitsOnly('.', '-')) {
                        needToDo = l.toDouble() <= r.toDouble()
                    } else {
                        needToDo = false
                    }
                }
            }
        }
    }

    println("need to do: $needToDo")

    fun doUpdateOps(action: CharSequence): CalcState {
        var result = CalcState.InProgress
        // ^ cycleA
        if (action.contains(againOp)) {
            val leftRight = action.split(againOp)
            val startName = leftRight[1].trim()

            println("^: startName: $startName")
            val starter = elements.lastOrNull { it.name.value == startName }

            println("^: starter: ${starters.lastOrNull()?.name}")
            starters.removeLastOrNull()
            println("^: starter after pop: ${starters.lastOrNull()?.name}")
            if (needToDo) {
                if (startName.isNotBlank()) {
                    starters.add(starter)
                    println("^: starter after add: ${starters.lastOrNull()?.name}")
                }
            }

            result = CalcState.InProgress
        }

        // i + 1 => i
        if (needToDo) {
            var index1 = action.indexOf(setOp)
            var index2 = action.indexOf(setOp, index1 + 1)
            var res = StringBuilder(action)
            val opStub = "%=%>"
            while (index1 != -1 && index2 != -1) {
                res.replace(index1, index1 + setOp.length, opStub)
                index1 = action.indexOf(setOp, index2 + 1)
                index2 = action.indexOf(setOp, index1 + 1)
            }
            val action = res.toString()
            when {
                action.contains(setOp) -> {
                    val leftRight = action.split(setOp)
                    println("leftRight: ${leftRight}")

                    var value = leftRight[0].trim().replace(opStub, setOp)

                    if (value.indexOfAny(charArrayOf('+', '-', '*', '/', '%')) != -1 && value.isDigitsOnly(
                            ' ',
                            '.',
                            '(',
                            ')',
                            '+',
                            '-',
                            '*',
                            '/',
                            '%'
                        )
                    ) {
                        value = mathEvaluator.evaluateDouble(value).toString().removeSuffix(".0")
                    }

                    val name = leftRight[1].trim()

                    println("setOp | => | : name: $name | value: $value")

                    if (name.isNotBlank()) {
                        when {
                            name.contains(innerValueDelimiter) -> {
                                val nameValue = name.split(innerValueDelimiter)
                                println("nameValue: ${nameValue}")
                                elements.firstOrNull { it.name.value == nameValue[0] }?.let {
                                    val name = nameValue[1]
                                    if (name.isNotBlank()) {
                                        val op = innerValueDelimiter + name
                                        when (op) {
                                            nameOp -> {
                                                it.name.value = value
                                            }

                                            xOp -> {
                                                it.area.value = it.area.value.copy(left = value.toFloat())
                                            }

                                            yOp -> {
                                                it.area.value = it.area.value.copy(top = value.toFloat())
                                            }

                                            widthOp -> {
                                                val right = it.area.value.left + value.toFloat()
                                                it.area.value = it.area.value.copy(right = right)
                                            }

                                            heightOp -> {
                                                val bottom = it.area.value.top + value.toFloat()
                                                it.area.value = it.area.value.copy(bottom = bottom)
                                            }
                                        }
                                    }
                                }
                            }

                            else -> {
                                elements.firstOrNull { it.name.value == name }?.let {
                                    if (it.isAbstrAction) {
                                        it.text.value = value
                                    } else {
                                        it.result.value = value
                                    }
                                }
                            }
                        }

                    }

                    result = CalcState.InProgress
                }

                action.contains(pauseOp) -> {
                    result = CalcState.Paused
                }

                action.contains(doneOp) -> {
                    result = CalcState.Done
                }
            }
        }

        println("result:$result")
        return result
    }

    var calcResult = CalcState.InProgress

    val sourceAction = element.text.value
    println("sourceAction: $sourceAction")
    if (sourceAction.trim().contains(questionOp.trim()) && sourceAction.endsWith(".")) {
        val indexOfQuestion = sourceAction.indexOf(questionOp.trim())
        val lines = sourceAction.substring(
            if (indexOfQuestion + 1 == sourceAction.length) indexOfQuestion else indexOfQuestion + 1,
            sourceAction.lastIndexOf(".")
        ).split("\n").drop(1).dropLast(1)

        lines.forEach { lineAction ->
            println(">>>>>>>>>>>>lineAction: $lineAction")

            val action = doInsertAndCalcValues(lineAction)
            val result = doUpdateOps(action)

            if (result == CalcState.Done || result == CalcState.Paused) {
                calcResult = result
            }
        }

    } else {
        println(">>>>>>>>>>>>singleAction: $singleAction")
        calcResult = doUpdateOps(singleAction)
    }

    return calcResult
}


