package me.pseudoapp.other


// NOTE: Сначала добавляем затем называем

fun main() {
    val data = addBrackets("1+1*2*(2+4)*3 + (12+14/2)*2")
    println(data)
    println("============= check: ")
    println((1 + ((((((1 * 2)) * (2 + 4))) * 3)) + (((12 + ((14 / 2))) * 2))))
    println("============= result: ")

//    println(calcMath(data))
    println(calcMath("(1+2*2+(1-2*2+1+(1-5*2+(-4+2*5))))"))
//    println(calcMath("2*2*(1-2)"))
//    println(calcMath("2*2*(1-2*(3+(48-51)))"))
//    println(calcMath("1+1*2*(2+4)*3+(12+14/2)*2"))
}//2*(-2-4)


fun CharSequence.positionOf(
    data: String,
    startIndex: Int = 0,
//    endIndex: Int = 0,
    fromEndToStart: Boolean = false
): Int {
    if (fromEndToStart) {
        val partSize = data.length
        var partIndex = startIndex - partSize
        while (partIndex > if (partSize == 1) 0 else 1) { // обрабатываем и если нечетное
            val part = this.substring(partIndex, partIndex + partSize)
//            println("part: $part")
            if (part == data) {
                return partIndex
            }

            partIndex -= partSize
        }

        return -1

    } else {
        return this.indexOf(data, startIndex)
    }
}

fun CharSequence.positionOf(
    check: (String) -> Boolean,
    startIndex: Int = 0,
    fromEndToStart: Boolean = false
): Int {
    val partSize = 1
    if (fromEndToStart) {
        var partIndex = startIndex - partSize
        while (partIndex > if (partSize == 1) 0 else 1) {
            val part = this.substring(partIndex, partIndex + partSize)
//            println("part: $part")
            if (check(part)) {
                return partIndex
            }

            partIndex -= partSize
        }

        return -1

    } else { // this, data
        var partIndex = startIndex
        while (partIndex + partSize <= this.length - if (partSize == 1) 0 else 1) {
            val part = this.substring(partIndex, partIndex + partSize)
//            println("part: $part")
            if (check(part)) {
                return partIndex
            }

            partIndex += partSize
        }

        return -1
    }
}

fun calcMath(rowExpression: String): String {

    val result = StringBuilder(
        "(${rowExpression.replace(" ", "")})"
    )
    val digitsWithDot = "0123456789."

    // 1. Сначала высчитываем то что в скобках
    val ops = listOf('-', '+')
    while (true) {
        // идём вперед к первой закрывающей - это значит самая глубокая вложенность
        val endIndex = result.positionOf(")")
        if (endIndex == -1) {
            break
        }
        // идём назад до ближайшей открывающей
        val startIndex = result.positionOf(
            "(",
            startIndex = endIndex,
            fromEndToStart = true
        )
        println("rowExpression: $rowExpression")
        println("startIndex: $startIndex")
        println("endIndex: $endIndex")

        // 2. Вычисляем сложные операции (*, /, % и т.п.)

        listOf('*', '/', '%').forEach { op ->
            println("x100")
            while (true) {
                println("x200")
                val endIndex0 = result.positionOf(")")
                val opIndex = result.substring(0, endIndex0).positionOf(
                    "$op",
                    startIndex + 1
                )
                println("300")
//                println("opIndex: ${opIndex}")
                if (opIndex == -1) {
                    break
                }

                var startIndex1 = result.positionOf(
                    { !digitsWithDot.contains(it) },
                    opIndex,
                    fromEndToStart = true
                )
                if (startIndex1 == -1) {
                    startIndex1 = 0
                }
                val endIndex1 = opIndex

                println("startIndex2: ${startIndex1}")
                println("endIndex2: ${endIndex1}")
                var expression = result.substring(startIndex1 + 1, endIndex1) + op

                val startIndex2 = opIndex + 1

                println("result...: ${result}")
                println("opIndex + 1: ${opIndex + 1}")
                var endIndex2 = result.positionOf(
                    {
                        println(it)
                        !digitsWithDot.contains(it)
                    }, opIndex + 1
                )
                println("endIndex2: $endIndex2")
                if (endIndex2 == -1) {
                    println("q .")
                    endIndex2 = result.length
                }
//                println("startIndex3: ${startIndex2}")
//                println("endIndex3: ${endIndex2}")
                expression += result.substring(startIndex2, endIndex2)

                println("expression: ${expression}")
                val calcResult = calcOperation(expression)
                println("calcResult: ${calcResult}")
                println("result a: ${result}")
                result.replace(startIndex1 + 1, endIndex2, "${calcResult}")
                println("result b: ${result}")
//                println("temp: ${result.substring(startIndex1 + 1, endIndex2)}")

            }
        }


        println("x0")
        // 3. Вычисляем простые(бинарные) операции (+, -)
        println("result c: ${result}")
        println("x1")
        val end1 = result.positionOf(")")
        var start1 = result.positionOf(
            "(",
            startIndex = end1,
            fromEndToStart = true
        )
        if (start1 == -1) {
            start1 = 0
        }
        println("x2")
        println("start1; $start1")
        println("x3")
//        val end1 = result.positionOf(")")
        val blockExpression = result.substring(start1 + 1, end1)
        println("blockExpression: $blockExpression")
        val calcResult = calcOperation(blockExpression.toString())
        println("calcOperation: $calcResult")
        result.replace(start1 + 1, end1, calcResult.toString())
        println("result: $result")
        println("x4")
        // 3. Убираем скобки
        val end2 = result.positionOf(")")
        var start2 = result.positionOf(
            "(",
            startIndex = end2,
            fromEndToStart = true
        )
        if (start2 == -1) {
            start2 = 0
        }

        println("x5")
        result.replace(start2, start2 + 1, "")
        println("x6")
        val end3 = result.positionOf(")")
        println("subs x: ${result.substring(end3, end3 + 1)}")
        result.replace(end3, end3 + 1, "")
        println("result x: $result")

        // 2. Затем переносим операцию за скобки
//        2*(-1) -> -2*(1) встречая * двигается дальше
//        2/(-1) -> -2/(1) встречая / двигается дальше
//        2%(-1) -> -2%(1) встречая % двигается дальше

//        2+(-1) -> 2-(1) встречая + остается собой
//        2-(-1) -> 2+(1) встречая - меняется на противоположное

        // NOTE: по сути + и - это true и false
        // и следовательно подчиняются булевой логике при переносе знака
        // и остаются собой при арифметических операциях-контейнерах
        // (которые раскладывают число по контейнерам(умножают, делят и т.д.))

        if (ops.contains(calcResult.toString()[0])) { // если + или - то переносим знак
            val op = calcResult.toString()[0]
            val notOp = ops.filter { it != op }
            while (true) {
                val index = result.positionOf("$op", start2)
                println("op: $op")
                println("start: $start2")
                println("index: $index")
                if (index == -1) {
                    break
                }

                // переносим знак до тех пор пока не встретим другой boolean-знак(+/-)
                result.replace(index, index + 1, "") // удаляем знак
//                println("result2: ${result}")
                for (i in index downTo 0) {
                    val c = result[i]

                    when (c) {
                        '+' -> {
//                            println("c: ${c} | ${result.substring(i, i + 1)} | $op")
                            result.replace(i, i + 1, "$op") // оставляем знак
                            println("result3: ${result}")
                            break
                        }

                        '-' -> result.replace(i, i + 1, "$notOp") // меняем знак
                    }
                    if (i == 0 && !ops.contains(c)) {
                        result.insert(0, op)
                    }
                }
            }
        }
    }

    return result.toString()
}

// todo: обработать умножение на 2*(-1)
// in: ((2*(-1)))
// out: (((2*)-1.0))
// expected: ((2*(-1)))
private fun addBrackets(data: String): String {
    val source = if (data.first() != '(') {
        StringBuilder("($data)")
    } else {
        StringBuilder(data)
    }

    var i = 0
    val highOperationsSet = setOf('*', '/')
    val lowOperationsSet = setOf('+', '-')

    var bracketsCount = 0
    while (i < source.length - 1) {
        if (highOperationsSet.contains(source[i])) {
            bracketsCount = 0
            var leftIndex = i
            while (true) {
                if (bracketsCount == 0
                    && (source[leftIndex - 1] == '('
                            || lowOperationsSet.contains(source[leftIndex - 1])
                            || highOperationsSet.contains(source[leftIndex - 1])
                            )
                ) {
                    source.insert(leftIndex, '(')
                    i++
                    break
                }
                if (source[leftIndex - 1] == ')') {
                    bracketsCount += 1
                } else if (source[leftIndex - 1] == '(') {
                    bracketsCount -= 1
                }
                leftIndex -= 1
            }

            bracketsCount = 0
            var rightIndex = i
            while (true) {
                if (bracketsCount == 0 &&
                    (source[rightIndex + 1] == ')'
                            || lowOperationsSet.contains(source[rightIndex + 1])
                            || highOperationsSet.contains(source[rightIndex + 1])
                            )
                ) {
                    source.insert(rightIndex + 1, ')')
                    break
                }
                if (source[rightIndex + 1] == '(') {
                    bracketsCount += 1
                } else if (source[rightIndex + 1] == ')') {
                    bracketsCount -= 1
                }
                rightIndex += 1
            }
            i = rightIndex + 1
        }
        // NOTE: какой-то нужен нам интерпретатор прям таких штук чтобы они показывали результат на лету типа превью в Compose
        // UPD: Похоже я его и делаю :)
        i++
    }

    return source.toString()
}

fun calcMath2(rowExpression: String): String {
    if (rowExpression.isEmpty()) {
        return rowExpression
    }

    val withBrackets = addBrackets(rowExpression)
    println("withBrackets: ${withBrackets}")
    val startIndex = withBrackets.lastIndexOf('(') + 1
    val endIndex = withBrackets.indexOf(')', startIndex = startIndex)

    val nextExpressionBlock = withBrackets.substring(startIndex, endIndex)
    println("nextExpressionBlock: ${nextExpressionBlock}")

    val result = withBrackets.replaceRange(
        startIndex - 1,
        endIndex + 1,
        "${calcOperation(nextExpressionBlock)}"
    )
    println("result: $result")
    return if (result.indexOf('(') == -1) {
        result
    } else {
        calcMath(result)
    }
}

private fun calcOperation(expression: String): Double {
    val atoms = mutableListOf<String>()

    var number = ""
    expression
        .replace("L", "")
        .replace("l", "")
        .forEachIndexed() { i, it ->
            when {
                i == 0 && it == '-' -> {
                    number = "$it"
                }

                it == '*' || it == '/' || it == '+' || it == '-' -> {
                    atoms.add(number.trim())
                    atoms.add("$it")
//                    println("add: $it")
                    number = ""
                }

                else -> {
                    number += it
//                    println("number: $number")
                }
            }
        }
    atoms.add(number.trim())
    var result = 0.0

    if (atoms.size == 1) {
        return atoms[0].toDouble()
    }

    val operation = atoms[1]
    fun action(operation: String, a: Double, b: Double): Double = when {
        operation == "*" -> a * b
        operation == "/" -> a / b
        operation == "+" -> a + b
        operation == "-" -> a - b
        operation.isDigitsOnly('.') -> result
        else -> throw IllegalArgumentException(operation)
    }

    println(atoms)
    result = if (atoms[0] == "") {
        "${atoms[1]}${atoms[2]}".toDouble()
    } else {
        action(operation, atoms[0].toDouble(), atoms[2].toDouble())
    }

    for (i in 2 until atoms.size) {
        when (val next = atoms[i]) {
            "*", "/", "+", "-" -> {
                result = action(next, result, atoms[i + 1].toDouble())
            }
        }

    }

    return result
}

fun String.isDigitsOnly(vararg allowedChars: Char): Boolean {
    val len = this.length
    var cp: Int
    var i = 0
    while (i < len) {
        cp = Character.codePointAt(this, i)
        if (!Character.isDigit(cp) && !allowedChars.contains(this[i])) {
            return false
        }
        i += Character.charCount(cp)
    }
    return true
}