package me.pseudoapp.views

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.github.murzagalin.evaluator.Evaluator
import me.pseudoapp.Element
import me.pseudoapp.other.firstContained
import me.pseudoapp.other.isDigitsOnly
import me.pseudoapp.other.positionOf
import me.pseudoapp.other.split


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

            val resultString = when {
                // копируем элемент по номеру места
                // [2]abcd
                op.isDigitsOnly() -> {
                    println("calcTrueLangInstructions: ${op}")

                    // return:
                    array[op.toInt() - 1].toString() // счет мест для заполнения начинается с 1-го
                }

                // копируем ряд по номеру места
                // [2..5]123456
                !op.startsWith("-") && !op.contains("->") && op.contains("..") -> {
                    println("calcTrueLangInstructions: ${op}")

                    val leftRight = op.split("..")

                    var from = if (leftRight[0].trim().isEmpty()) {
                        0
                    } else {
                        leftRight[0].trim().toInt() - 1
                    }

                    if (from < 0) {
                        from = 0
                    }
                    var to = if (leftRight[1].trim().isEmpty()) {
                        array.length - 1
                    } else {
                        leftRight[1].trim().toInt() - 1
                    }
                    if (to > array.length - 1) {
                        to = array.length - 1
                    }
                    to += 1

                    // return:
                    array.substring(from, to) // счет мест для заполнения начинается с 1-го
                }

                else -> {
                    println("calcMrGreenLangInstructions: ${op}")

                    // return:
                    calcMrGreenLangInstructions(array, op)
                }
            }

            println("resultString a: $resultString")
            println("action range: ${action.substring(startReplacedIndex, endArrayIndex)}")
            action.replace(startReplacedIndex, endArrayIndex, resultString)

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

    println(">>>>>>>>>>>>calcResult: $calcResult")
    return calcResult
}


