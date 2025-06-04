package me.pseudoapp.views

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.github.murzagalin.evaluator.Evaluator
import me.pseudoapp.Element
import me.pseudoapp.other.firstContained
import me.pseudoapp.other.isDigitsOnly
import me.pseudoapp.other.positionOf
import kotlin.math.abs

val mathEvaluator = Evaluator()
var activeElement: Element? = null
fun calcInstructions(elements: SnapshotStateList<Element>, element: Element): Boolean {
    if (!element.isAbstract) {
//        activeElement?.inProgress?.value = false
        return false
    }

    activeElement?.inProgress?.value = false
    activeElement = element
    activeElement?.inProgress?.value = true

    var workDone = false
    element.elements.sortedBy { it.area.top }
        .forEach {
            workDone = calcInstructions(elements, it)
            if (workDone) {
                return true
            }
        }

    var action = StringBuilder(element.action.value)

    //  Step: подставляем значения соседних элементов
    var index = action.indexOf(":")
    var endIndex = action.indexOf(" ", index)
    while (index != -1 && endIndex != -1 && index + 1 != endIndex) {
        val a = action.substring(index + 1, endIndex)
        val b = element.elements.firstOrNull { it.name.value == a }?.value?.value

        b?.let {
            action.replace(index, endIndex, b)
        }
        index = action.indexOf(":", endIndex)
        endIndex = action.indexOf(" ", index)
    }

    //  Step: подставляем значения внутренних элементов
    elements.forEach { e ->
        val name = e.name.value
        if (!name.isEmpty()) {
            index = action.indexOf(name)
            endIndex = action.indexOf(" ", index)
            while (index != -1 && endIndex != -1 && index != endIndex && endIndex - index == name.length) {
                val a = action.substring(index, endIndex)
                val b = elements.firstOrNull { it.name.value == name }?.value?.value

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
            },
            startIndex = endArrayOpIndex
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
                    startArrayOpIndex,
                    endArrayIndex,
                    array.reversed()
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
                    startArrayOpIndex,
                    endArrayIndex,
                    array.removeRange(
                        abs(op.toInt()) - 1,
                        abs(op.toInt())
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
                    startArrayOpIndex,
                    endArrayIndex,
                    StringBuilder(array).insert(i, v).toString()
                )
            }

            // удаляем ряд по номеру места
//                      [-2..5]abcd
            op.startsWith("-") && !op.contains("->") && op.contains("..") -> {
                val leftRight = op.removePrefix("-")
                    .split("..")
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
                        v = v.removeSuffix("\"")
                            .removePrefix("\"")
                    }
                    action.replace(
                        startArrayOpIndex,
                        endArrayIndex,
                        array.replaceRange(i, i + 1, v)
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
                        v = v.removeSuffix("\"")
                            .removePrefix("\"")
                    }
                    action.replace(
                        startArrayOpIndex,
                        endArrayIndex,
                        array.replaceRange(from, to, v)
                    )
                }
            }
        }

        startArrayOpIndex = action.indexOf("[", endArrayOpIndex + 1)
        endArrayOpIndex = action.indexOf("]", endArrayOpIndex + 1)
    }

    element.value.value = action.toString()

    // 0x123242 => :color
    // Result => :name
    // :size

    val questionOp = " ? "
    var needToDo = true
    if (action.contains(questionOp)) {
        val condition = action.split(questionOp)[0]
        action = StringBuilder(action.split(questionOp)[1])

        val booleanOps = listOf(
            " == ", " != ", " < ", " > ", " <= ", " >= "
        )


        val conditionOp = condition.firstContained(booleanOps)

        val leftRight = condition.split(conditionOp)
        when (conditionOp) {
            " != " -> {
                val l = leftRight[0].trim()
                val r = leftRight[1].trim()

                println("l = |$l|, r = |$r|")

                needToDo = l != r
            }

            " == " -> {
                val l = leftRight[0].trim()
                val r = leftRight[1].trim()

                println("l = |$l|, r = |$r|")

                needToDo = l == r
            }

            " > " -> {
                val l = leftRight[0].trim()
                val r = leftRight[1].trim()

                if (l.isDigitsOnly('.', '-') && r.isDigitsOnly('.', '-')) {
                    needToDo = l.toDouble() > r.toDouble()
                } else {
                    needToDo = false
                }
            }

            " >= " -> {
                val l = leftRight[0].trim()
                val r = leftRight[1].trim()

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

                if (l.isDigitsOnly('.', '-') && r.isDigitsOnly('.', '-')) {
                    println("a l = |$l|, r = |$r|")
                    needToDo = l.toDouble() < r.toDouble()
                } else {
                    println("b l = |$l|, r = |$r|")
                    needToDo = false
                }
            }

            " <= " -> {
//                    condition: 1 < [1]12945
                val l = leftRight[0].trim()
                val r = leftRight[1].trim()

                if (l.isDigitsOnly('.', '-') && r.isDigitsOnly('.', '-')) {
                    println("a l = |$l|, r = |$r|")
                    needToDo = l.toDouble() <= r.toDouble()
                } else {
                    println("b l = |$l|, r = |$r|")
                    needToDo = false
                }
            }
        }
    }

    if (needToDo) {
        val resetOp = " => "

        when {
            // # i + 1 => i
            action.contains(resetOp) -> {
                val leftRight = action.split(resetOp)
                var value = leftRight[0].trim()

                try {
                    value = mathEvaluator.evaluateDouble(leftRight[0].trim())
                        .toString()
                        .removeSuffix(".0")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val name = leftRight[1].trim()

                elements.forEachIndexed { i, it ->
                    if (!name.isEmpty() && it.name.value == name) {
                        elements[i].value.value = value
                    }
                }

                return true
            }
        }
    }

    activeElement?.inProgress?.value = false

    return false
}

