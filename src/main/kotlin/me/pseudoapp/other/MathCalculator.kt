package me.pseudoapp.other


// NOTE: Сначала добавляем затем называем

fun main() {
    val data = addBrackets("1+1*2*(2+4)*3 + (12+14/2)*2")
    println(data)
    println("============= check: ")
    println((1 + ((((((1 * 2)) * (2 + 4))) * 3)) + (((12 + ((14 / 2))) * 2))))
    println("============= result: ")

    println(calcMath(data))
    println(calcMath("2*(-1)"))
}

// todo: обработать умножение на 2*(-1)
// in: ((2*(-1)))
// out: (((2*)-1.0))
// expected: -2.0
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
                leftIndex = leftIndex - 1
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
                rightIndex = rightIndex + 1
            }
            i = rightIndex + 1
        }
        // NOTE: какой-то нужен нам интерпретатор прям таких штук чтобы они показывали результат на лету типа превью в Compose
        // UPD: Похоже я его и делаю :)
        i++
    }

    return source.toString()
}

fun calcMath(rowExpression: String): String {
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
        "${calc(nextExpressionBlock)}"
    )
    println("result: $result")
    return if (result.indexOf('(') == -1) {
        result
    } else {
        calcMath(result)
    }
}

private fun calc(expression: String): Double {
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
                    println("add: $it")
                    number = ""
                }

                else -> {
                    number = "$number$it"
                    println("number: $number")
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