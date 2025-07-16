package me.pseudoapp.views

import me.pseudoapp.other.isDigitsOnly
import kotlin.math.abs
import kotlin.math.max

enum class StringOperationType {
    Set, Insert, Delete
}

// NOTE:
// Изначально я делал это как часть языка True для работы со строками/массивами
// затем это выросло в полноценный язык Mr. Green который может использоваться сам по себе либо как часть True,
// в True же я оставил элементарные операции [1], [1..9]

// todo: Сделать библиотеку для работы с массивами, списками и строками,
//  на языке С и\или Rust и обертку для Kotlin
fun calcMrGreenLangInstructions(
    data: CharSequence,
    leftInstruction: String = "",
    rightInstruction: String = ""
): String {
    val op = leftInstruction.ifEmpty { rightInstruction }
    val array = data.toString()
    // [temp -> +3] // вставить на 3 место, остальные сдвинуть
    // [temp <- 3] // переместить в temp


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

        // todo
        // переместить 'a' из этого элемента в элемент Box
        // [-1..3 -> ?a 3:|]1a|a2|a3a,|abc|5

        // переместить 'a' из Box в этот элемент
        // [+a :Box -> 1..3 :|]1a|a2|a3a,|abc|5

        // копировать 'a' из :symbols:letters в этот элемент
        // [a :symbols:letters:latin -> 1 :,]1|2|3,|abc|5

        // [:count ?a]Box == 1

        // todo
        // сколько элементов до разделителя ?.
        // [A? :.]AABB|ABABA // 2 элемента A
        // [?.]AABB|ABABA // 10 элементов всего
        op.contains("?") && op.endsWith(".") -> {
            val lr = op.split("?")
            val query = lr[0]
            val delimiter = lr[1].trimEnd('.')
            if (query.isEmpty() && delimiter.isEmpty()) {
                return array.length.toString()

            } else {
                var count = 0
                var index = array.indexOf(query)
                val delimiterIndex = array.indexOf(delimiter)
                while (index != -1 && (index < delimiterIndex || delimiterIndex == -1)) {
                    count += 1
                    index = array.indexOf(query, index + 1)
                }

                return count.toString()
            }
        }

        // заполнить ячейку 3 содержимым x
        // Case: [x ->3]abc
        // Case: [x 1->3]abc


        // [x -> ?a :, ]a, a, c => Result
        // [x -> 1..2 ?a :|]a|b|c => Result
        // [x -> a..d ? , ]abcde => Result
        // [x -> {::} ? , ]{abc}de => Result

        // [:count -> {::} ? , ]{abc}de => Count
        // [:c -> {::} ? , ]{abc}de => Count
        // [:index -> {::} ? , ]{abc}de => Index
        // [:i -> {::} ? , ]{abc}de => Index

        // [+x -> 3]12345 => Result
        // [-x -> 1..3]12345 => Result
        // [- -> 3]12345 => Result

        op.contains(" -> ") -> {
            val valueAndRange = op.split(" -> ", limit = 2)
            if (valueAndRange.size > 1) {
                // [{чем заменяем} -> ..{где заменяем} ?{что заменяем} :{разделитель} ]abc => Result
                var value = valueAndRange[0].trim()
                var opType = StringOperationType.Set

                if (value.startsWith("+")) {
                    opType = StringOperationType.Insert
//                  value = value.drop(1)

                } else if (value.startsWith("-")) {
                    opType = StringOperationType.Delete
//                  value = value.drop(1)
                }

                if (opType == StringOperationType.Insert) {
                    value = value.drop(1) // remove +
                }

                if (value.startsWith("\"") && value.endsWith("\"")) { // without trim()
                    value = value.removeSuffix("\"").removePrefix("\"")
                }
                val positionAndAreaAndWhatAndDelimiter = valueAndRange[1].trim().split(" ", limit = 3)

                val position =
                    positionAndAreaAndWhatAndDelimiter.firstOrNull { it.contains("..") } // range inclusive: a..c
                        ?: positionAndAreaAndWhatAndDelimiter.firstOrNull { it.contains("::") } // inner content: {.}
                        ?: positionAndAreaAndWhatAndDelimiter.firstOrNull { it.isDigitsOnly() } // position: 2
                        ?: ""
                val what = positionAndAreaAndWhatAndDelimiter.firstOrNull { it.startsWith("?") }?.drop(1) ?: ""
                val separator = positionAndAreaAndWhatAndDelimiter.firstOrNull { it.startsWith(":") }?.drop(1) ?: ""

                println("whereAndWhatAndDelimiter: $positionAndAreaAndWhatAndDelimiter")
                var positionRangeStart = ""
                var positionRangeEnd = ""
                var isSinglePositionRange = false
                if (position.contains("..")) {
                    positionRangeStart = position.substringBefore("..")
                    positionRangeEnd = position.substringAfter("..")
                } else if (position.contains("::")) {
                    positionRangeStart = position.substringBefore("::")
                    positionRangeEnd = position.substringAfter("::")
                } else if(position.isDigitsOnly()) {
                    positionRangeStart = position
                    isSinglePositionRange = true
                }

                val isRangeInnerContentType = position.contains("::")
                println("positionRangeStart: $positionRangeStart | positionRangeEnd: $positionRangeEnd")
                var start = 0
                var end = 0
                val isDigitRange = listOf(positionRangeStart, positionRangeEnd).all { it.isDigitsOnly() || it.isEmpty() }
                println("isDigitRange: $isDigitRange")
                if (isDigitRange) {
                    start = if (positionRangeStart.isEmpty())
                        0
                    else
                        positionRangeStart.trim().toInt() - 1   // счет мест начинается с 1-го

                    if (start < 0) {
                        start = 0
                    }

                    end = when {
                        positionRangeStart.isEmpty() -> {
                            println("end: 0")
                            0
                        }
                        positionRangeEnd.isEmpty() -> {
                            println("end: 1")
                            array.length-1
                        }

                        isSinglePositionRange -> {
                            println("end: 2")
                            start + 1
                        }

                        else -> {
                            println("end: 4")
                            positionRangeEnd.trim().toIntOrNull() ?: array.length
                        }
                    }
                    if (end > array.length) {
                        end = array.length
                    }
                    if (isRangeInnerContentType) {
                        start += 1
                        end -= 1
                    }

                } else { // isLetter
                    if (positionRangeEnd.isEmpty()) {
                        start = 0
                    } else {
                        start = array.indexOf(positionRangeStart)
                        if (start == -1) {
                            start = 0
                        } else {
                            start += 1
                        }
                    }

                    if (positionRangeEnd.isEmpty()) {
                        end = array.length
                    } else {
                        end = array.indexOf(positionRangeEnd, start + 1)
                        if (end == -1) {
                            end = array.length
                        } else {
                            end += 2
                        }
                    }

                    if (isRangeInnerContentType) {
                        start += positionRangeStart.length
                        end -= positionRangeEnd.length
                    }
                }
                println("start: $start, end: $end")

                if (opType == StringOperationType.Insert) {
                    // добавляем элемент по номеру места (остальные сдвигаются)
                    // [+| -> 2]abcd // a|bcd
                    // [+| -> 2 ^2..4]abcd // ab|cd

                    val i = start
                    val value = if (separator.isEmpty()) StringBuilder(
                        array.substring(
                            max(0, start - 1),
                            max(0, end - 1)
                        )
                    ).insert(i, value).toString()
                    else {
                        val source =
                            array.substring(
                                max(0, start - 1),
                                max(0, end - 1)
                            ).split(separator).toMutableList()
                        source[i] = value
                        source.joinToString(separator)
                    }

                    val result = array.substring(0, max(0, start - 1)) /*+ "|"*/ + value /*+ "|" */ + array.substring(
                        max(
                            0,
                            end - 1
                        ), array.length
                    )

                    return result

                } else {
                    val result = {
                        if (isDigitRange) {
                            val source =
                                if (separator.isEmpty()) array.toCharArray().map { it.toString() }.toMutableList()
                                else array.split(separator).toMutableList()
                            println("source a: $source")
                            for (i in start until if (end > source.size) source.size else end) {
                                source[i] = if (what.isEmpty()) value
                                else source[i].replace(what, value)
                            }
                            source.joinToString(separator)
                        } else {
                            val source = if (separator.isEmpty()) array.substring(start - 1, end - 1).toCharArray()
                                .map { it.toString() }.toMutableList()
                            else array.substring(start - 1, end - 1).split(separator).toMutableList()
                            for (i in 0 until source.size) {
                                source[i] = if (what.isEmpty()) value
                                else source[i].replace(what, value)
                            }
                            array.substring(
                                0,
                                start - 1
                            ) /*+ "|" */ + source.joinToString(separator)/* + "|"*/ + array.substring(
                                end - 1,
                                array.length
                            )
                        }

                        // [X -> {..} ?a :,]c,a bsdaa{a,s,d,a},dsda => Violet
                    }()
                    return result
                }
            }
        }

        // переворачиваем данные(инвертируем порядок)
        // [!]abcd
        op == "!" -> {
            return array.reversed()
        }

        // удаляем элемент по номеру места
        // [-2]abcd
        op.startsWith("-") && !op.contains("->") && op.isDigitsOnly('-') -> {
            //                            / заменить оригинал
            return array.removeRange(
                abs(op.toInt()) - 1, abs(op.toInt())
            ) // счет мест для заполнения начинается с 1-го
        }

        // удаляем ряд по номеру
        // [-2..5]abcd
        op.startsWith("-") && !op.contains("->") && !op.contains("->") && op.contains("..") -> {
            val leftRight = op.removePrefix("-").split("..")
            val from = abs(leftRight[0].trim().toInt()) - 1
            val to = leftRight[1].trim().toInt()
            if (leftRight.size == 2) {
                return array.removeRange(from, to) // счет мест для заполнения начинается с 1-го
            }
        }
    }

    return "?"
}