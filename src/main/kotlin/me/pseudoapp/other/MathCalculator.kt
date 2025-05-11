package me.pseudoapp.other


// NOTE: Сначала добавляем затем называем

fun main() {
    val data = addBrackets("1+1*2*(2+4)*3 + (12+14/2)*2")
    println(data)
    println("============= check: ")
    println((1 + ((((((1 * 2)) * (2 + 4))) * 3)) + (((12 + ((14 / 2))) * 2))))
    println("============= result: ")

//    println(calcMath(data)
//    println(calcMath(addBrackets("(1+2*2+(1-2*2+1+(1-5*2+(-4+2*5))))"))
//    println(calcMath(addBrackets("2*2*(1-2)")))
    println(calcMath("2*2*(1-2*(3+(48-51)))"))
//    println(calcMath(addBrackets("1+1*2*(2+4)*3+(12+14/2)*2"))
}

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

fun calcMath(expression: String): String {
    var result = calcInBrackets(addBrackets(expression))
    if (result == "-0.0") {
        result = "0"
    }

    return result.removeSuffix(".0")
}

fun calcInBrackets(rowExpression: String): String {
    if (rowExpression.isEmpty()) {
        return rowExpression
    }

    val startIndex = rowExpression.lastIndexOf('(') + 1
    val endIndex = rowExpression.indexOf(')', startIndex = startIndex)

    val nextExpressionBlock = rowExpression.substring(startIndex, endIndex)

    val result = rowExpression.replaceRange(
        startIndex - 1,
        endIndex + 1,
        "${calcOperation(nextExpressionBlock)}"
    )

    return if (result.indexOf('(') == -1) {
        result
    } else {
        calcInBrackets(result)
    }
}

val allOps = listOf('*', '/', '%', '+', '-')
private fun calcOperation(expression: String): Double {
    val atoms = mutableListOf<String>()

    var number = ""
    expression
        .replace("L", "")
        .replace("l", "")
        .forEachIndexed() { i, it ->
            when {
                // в этих случаях начинаем формировать новое атом-число
                it == '*' || it == '/' ||
                        (i != 0 && !allOps.contains(expression[i - 1]) && (it == '+' || it == '-')) -> {
                    atoms.add(number.trim())
                    atoms.add("$it")
                    number = ""
                }

                // иначе продолжаем наращивать текущее
                else -> {
                    number += it
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
        if (atoms[2] == "-") {
            action(operation, atoms[0].toDouble(), "${atoms[2]}${atoms[3]}".toDouble())
        } else {
            action(operation, atoms[0].toDouble(), atoms[2].toDouble())
        }
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