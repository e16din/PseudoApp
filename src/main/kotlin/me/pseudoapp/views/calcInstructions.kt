package me.pseudoapp.views

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.github.murzagalin.evaluator.Evaluator
import me.pseudoapp.Element
import me.pseudoapp.other.firstContained
import me.pseudoapp.other.isDigitsOnly
import me.pseudoapp.other.positionOf
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

val sizeOp = ":size"
val nameOp = ":name"
val colorOp = ":color"

val xOp = ":x"
val yOp = ":y"

val widthOp = ":width"
val heightOp = ":height"

val unknownResult = "?"


fun calcInstructions(
    element: Element,
    elements: SnapshotStateList<Element>,
    starters: SnapshotStateList<Element?>,
    stepDelayMsValue: MutableState<Long>,
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
        var action = StringBuilder(a)
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
                endIndex = action.indexOf(" ", index)
                val b = elements.firstOrNull { it.name.value == name }?.result?.value
                while (index < opIndex && index != -1 && endIndex != -1 && index != endIndex && endIndex - index == name.length) {
                    val replaced = action.substring(index, endIndex)
                    println("replaced: $replaced")

                    b?.let {
                        action.replace(index, endIndex, b)
                    }
                    index = action.indexOf(name, endIndex)
                    endIndex = action.indexOf(" ", index)
                }
            }
        }

        //  Step: высчитываем и подставляем операции со строками/массивами
        // слева направо
        var startArrayOpIndex = action.indexOf("[")
        var endArrayOpIndex = action.indexOf("]")
        while (startArrayOpIndex < opIndex && startArrayOpIndex != -1 && endArrayOpIndex != -1) {
            val startArrayIndex = endArrayOpIndex + 1
            var endArrayIndex = action.positionOf(
                {
                    it == " " || it == "["
                }, startIndex = endArrayOpIndex
            )
            if (endArrayIndex == -1) {
                endArrayIndex = action.length
            }

            val op = action.substring(startArrayOpIndex + 1, endArrayOpIndex)
            val array = action.substring(startArrayIndex, endArrayIndex) // [op]array


            when {
                // переворачиваем данные(инвертируем порядок)
                //                      [!]abcd
                op == "!" -> {
                    action.replace(
                        startArrayOpIndex, endArrayIndex, array.reversed()
                    )
                }

                // копируем элемент по номеру места
                //                      [2]abcd
                op.isDigitsOnly() -> {
                    action.replace(
                        startArrayOpIndex,
                        endArrayIndex,
                        "${array[op.toInt() - 1]}" // счет мест для заполнения начинается с 1-го
                    )
                }

                // копируем ряд по номеру места
                //                      [2..5]abcd
                !op.startsWith("-") && !op.contains("->") && op.contains("..") -> {
                    val leftRight = op.split("..")
                    val from = leftRight[0].trim().toInt() - 1
                    val to = leftRight[1].trim().toInt()
                    if (leftRight.size == 2) {
                        action.replace(
                            startArrayOpIndex,
                            endArrayIndex,
                            array.substring(from, to) // счет мест для заполнения начинается с 1-го
                        )
                    }
                }

                // удаляем элемент по номеру места
                //                      [-2]abcd
                op.startsWith("-") && op.isDigitsOnly('-') -> {
                    //                            / заменить оригинал
                    action.replace(
                        startArrayOpIndex, endArrayIndex, array.removeRange(
                            abs(op.toInt()) - 1, abs(op.toInt())
                        ) // счет мест для заполнения начинается с 1-го
                    )
                }

                // добавляем элемент по номеру места (остальные сдвигаются)
                //                      [x -> +2]abcd
                op.contains(" -> +") && !op.contains("..") -> {
                    val leftRight = op.split(" -> +")
                    val v = leftRight[0].trim()
                    val i = leftRight[1].trim().toInt() - 1 // счет начинается с 1-го
                    action.replace(
                        startArrayOpIndex, endArrayIndex, StringBuilder(array).insert(i, v).toString()
                    )
                }

                // удаляем ряд по номеру места
                //                      [-2..5]abcd
                op.startsWith("-") && !op.contains("->") && op.contains("..") -> {
                    val leftRight = op.removePrefix("-").split("..")
                    val from = abs(leftRight[0].trim().toInt()) - 1
                    val to = leftRight[1].trim().toInt()
                    if (leftRight.size == 2) {
                        action.replace(
                            startArrayOpIndex,
                            endArrayIndex,
                            array.removeRange(from, to) // счет мест для заполнения начинается с 1-го
                        )
                    }
                }

                // заполняем ячейку элемента данными по номеру места
                //                      [x -> 2]abcd
                op.contains(" -> ") && !op.contains("..") -> {
                    val leftRight = op.split(" -> ", limit = 2)
                    if (leftRight.size == 2) {
                        var v = leftRight[0].trim()
                        val i = leftRight[1].trim().toInt() - 1 // счет мест начинается с 1-го
                        if (v.startsWith("\"") && v.endsWith("\"")) {
                            v = v.removeSuffix("\"").removePrefix("\"")
                        }
                        action.replace(
                            startArrayOpIndex, endArrayIndex, array.replaceRange(i, i + 1, v)
                        )
                    }
                }

                // заполняем ряд данными
                //                      [x -> 1..3]abcd
                op.contains(" -> ") && op.contains("..") -> {
                    val leftRightSet = op.split(" -> ", limit = 2)
                    if (leftRightSet.size == 2) {
                        val leftRightRange = op.split("..")
                        val from = leftRightRange[0].trim().toInt() - 1
                        val to = leftRightRange[1].trim().split(" ").first().toInt()

                        var v = leftRightSet[0].trim()
                        if (v.startsWith("\"") && v.endsWith("\"")) {
                            v = v.removeSuffix("\"").removePrefix("\"")
                        }
                        action.replace(
                            startArrayOpIndex, endArrayIndex, array.replaceRange(from, to, v)
                        )
                    }
                }
            }

            startArrayOpIndex = action.indexOf("[", endArrayOpIndex + 1)
            endArrayOpIndex = action.indexOf("]", endArrayOpIndex + 1)
        }

        fun replaceOp(op: String, insertion: (value: String) -> String) {
            var sizeEndIndex = action.indexOf(op)
            var sizeStartIndex = action.positionOf(" ", sizeEndIndex, fromEndToStart = true) + 1

            while (sizeStartIndex != -1 && sizeEndIndex != -1) {
                val value = action.substring(sizeStartIndex, sizeEndIndex)

                action.replace(sizeEndIndex, sizeEndIndex + op.length, "")
                action.replace(sizeStartIndex, sizeEndIndex, insertion(value))

                sizeEndIndex = action.indexOf(sizeOp, sizeEndIndex + 1)
                sizeStartIndex = action.positionOf(" ", sizeEndIndex, fromEndToStart = true) + 1
            }
        }

        // Step: Подставляем размеры массивов/строк
        replaceOp(sizeOp) { value ->
            return@replaceOp value.length.toString()
        }

        // Step: Подставляем цвета элементов
        replaceOp(colorOp) { value ->
            val e = elements.firstOrNull { it.name.value == value }
            return@replaceOp e?.let { e.color.value.toString() } ?: unknownResult //todo: print hex value
        }

        // Step: Подставляем ширину элементов
        replaceOp(widthOp) { value ->
            val e = elements.firstOrNull { it.name.value == value }
            return@replaceOp e?.let { e.area.value.width.toString() } ?: unknownResult
        }

        // Step: Подставляем высоту элементов
        replaceOp(heightOp) { value ->
            val e = elements.firstOrNull { it.name.value == value }
            return@replaceOp e?.let { e.area.value.height.toString() } ?: unknownResult
        }

        // Step: Подставляем x элементов
        replaceOp(xOp) { value ->
            val e = elements.firstOrNull { it.name.value == value }
            return@replaceOp e?.let { e.area.value.left.toString() } ?: unknownResult
        }

        // Step: Подставляем y элементов
        replaceOp(yOp) { value ->
            val e = elements.firstOrNull { it.name.value == value }
            return@replaceOp e?.let { e.area.value.top.toString() } ?: unknownResult
        }

        // Step: Подставляем stepDelay элементов
        replaceOp(stepDelayOp) { value ->
            val e = elements.firstOrNull { it.name.value == value }
            return@replaceOp e?.let { stepDelayMsValue.value.toString() } ?: unknownResult
        }

        element.result.value = action.toString()
        println("result action: $action")

        return action
    }


    var singleAction = StringBuilder(doInsertAndCalcValues(element.text.value))

    var needToDo = true
    val questionLeftRight = singleAction.split(questionOp)
    if (singleAction.contains(questionOp)) {
        val condition = questionLeftRight[0]
        singleAction = StringBuilder(questionLeftRight[1])

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
                    var value = leftRight[0].trim().replace(opStub, setOp)

                    if (value.indexOfAny(charArrayOf('+', '-', '*', '/', '%')) != -1 && value.isDigitsOnly(' ', '.', '(', ')', '+', '-', '*', '/', '%')) {
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
        val lines = sourceAction.substring(
            sourceAction.indexOf(unknownResult) + 1, sourceAction.lastIndexOf(".")
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


