package me.pseudoapp.views

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.github.murzagalin.evaluator.Evaluator
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.pseudoapp.Element
import me.pseudoapp.other.firstContained
import me.pseudoapp.other.isDigitsOnly
import me.pseudoapp.other.positionOf
import kotlin.math.abs
import kotlin.math.max

val mathEvaluator = Evaluator()
var activeElement: Element? = null

val questionOp = "? "

val setOp = "=> "
val againOp = "=^ "
val doneOp = "=done"
val pauseOp = "=pause"
val stepDelayOp = "=stepDelay"

val sizeOp = ":size"
val nameOp = ":name"
val colorOp = ":color"

val xOp = ":x"
val yOp = ":y"

val widthOp = ":width"
val heightOp = ":height"

// 0x123242 => :color
// Result => :name

//    a < b ? a + b => i
//    a < b ? [i]abc => i
//    a < b ? ^ i
//    a < b ? :pause
//    a < b ? :done

// condition ? action (|any op| name)
// condition ? action (|any op| name)
// [check] ? [calc data] = [update element]
//  a < b ? a + b => i
//  a < b ? =^ i
//  a < b ? =pause
//  a < b ? =done


// [check] ? [calc data] = ^ [update element]
// [check] ? = ^ [update element]


val calcContext = Dispatchers.Default + SupervisorJob() + CoroutineExceptionHandler { c, t ->
    println("error:")
    t.printStackTrace()
}
val calcScope = CoroutineScope(calcContext)

suspend fun calcInstructions(
    elements: SnapshotStateList<Element>,
    starters: SnapshotStateList<Element?>,
    element: Element,
): CalcState {
    // program is done ? see: doneOp
    activeElement?.inProgress?.value = false
    activeElement = element
    activeElement?.inProgress?.value = true
    println("active: action: ${activeElement?.text?.value} | value: ${activeElement?.result?.value}")

    if (!element.isAbstract) {
        return CalcState.InProgress
    }

    element.elements.forEach { e ->
        calcInstructions(e.elements, starters, e)
    }

    fun insertValues(a: CharSequence): CharSequence {
        var action = StringBuilder(a)
        //  Step: подставляем значения внутренних элементов
        var index = action.indexOf(":")
        var endIndex = action.indexOf(" ", index)

        while (index != -1 && endIndex != -1 && index + 1 != endIndex) {
            val a = action.substring(index + 1, endIndex)
            val b = element.elements.firstOrNull { it.name.value == a }?.result?.value

            b?.let {
                action.replace(index, endIndex, b)
            }
            index = action.indexOf(":", endIndex)
            endIndex = action.indexOf(" ", index)
        }

        //  Step: подставляем значения соседних элементов
        elements.forEach { e ->
            val name = e.name.value
            if (!name.isEmpty()) {
                index = action.indexOf(name)
                endIndex = action.indexOf(" ", index)
                while (index != -1 && endIndex != -1 && index != endIndex && endIndex - index == name.length) {
                    action.substring(index, endIndex)
                    val b = elements.firstOrNull { it.name.value == name }?.result?.value

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
        while (startArrayOpIndex != -1 && endArrayOpIndex != -1) {
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

        element.result.value = action.toString()

        // Step: Подставляем размеры массивов/строк
        var sizeEndIndex = action.indexOf(sizeOp)
        var sizeStartIndex = action.positionOf(" ", sizeEndIndex, fromEndToStart = true)
        sizeStartIndex = max(sizeStartIndex, 0)


        while (sizeStartIndex != -1 && sizeEndIndex != -1) {
            val value = action.substring(sizeStartIndex, sizeEndIndex)

            action.replace(sizeEndIndex, sizeEndIndex + sizeOp.length, "")
            action.replace(sizeStartIndex, sizeEndIndex, value.length.toString())

            sizeEndIndex = action.indexOf(sizeOp, sizeEndIndex + 1)
            sizeStartIndex = action.positionOf(" ", sizeEndIndex, fromEndToStart = true)
            sizeStartIndex = max(sizeStartIndex, 0)
        }

        println("result action: $action")
        return action
    }


    var singleAction = StringBuilder(insertValues(element.text.value))

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

    suspend fun cycleAndSetValues(action: CharSequence): CalcState {
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
            when {
                action.contains(setOp) -> {
                    val leftRight = action.split(setOp)
                    var value = leftRight[0].trim()

                    calcScope.launch {// to go next after any exception
                        try { // if it is math operation then:
                            value = mathEvaluator.evaluateDouble(value).toString().removeSuffix(".0")
                        } catch (e: IllegalArgumentException) {
                            e.printStackTrace()
                        }
                    }

                    val name = leftRight[1].trim()

                    println("setOp | => | : name: $name | value: $value")

                    if (name.isNotBlank()) {
                        elements.firstOrNull { it.name.value == name }?.let {
                            it.result.value = value
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
            sourceAction.indexOf("?") + 1, sourceAction.lastIndexOf(".")
        ).split("\n").drop(1).dropLast(1)

        lines.forEach { lineAction ->
            println(">>>>>>>>>>>>lineAction: $lineAction")

            val action = insertValues(lineAction)
            val result = cycleAndSetValues(action)

            if (result == CalcState.Done || result == CalcState.Paused) {
                calcResult = result
            }
        }

    } else {
        println(">>>>>>>>>>>>singleAction: $singleAction")
        calcResult = cycleAndSetValues(singleAction)
    }

    return calcResult
}


