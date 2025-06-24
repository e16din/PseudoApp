package me.pseudoapp.other

import kotlin.math.abs
import kotlin.math.max

fun calculate(data: CharSequence, leftInstruction: String = "", rightInstruction: String = ""): String {
    val op = leftInstruction.ifEmpty { rightInstruction }
    val array = data.toString()
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
        // [x -> a..d ? , ]abcde => Result
        // [x -> {::} ? , ]{abc}de => Result

        // [+x -> 3]12345 => Result
        // [-x -> 1..3]12345 => Result
        // [- -> 3]12345 => Result

        op.contains(" -> ") -> {
            val valueAndRange = op.split(" -> ", limit = 2)
            if (valueAndRange.size > 1) {
                // [{чем заменяем} -> ..{где заменяем} ?{что заменяем} :{разделитель} ]abc => Result
                var value = valueAndRange[0].trim()
                if (value.startsWith("\"") && value.endsWith("\"")) { // without trim()
                    value = value.removeSuffix("\"").removePrefix("\"")
                }
                val whereAndWhatAndDelimiter = valueAndRange[1].trim().split(" ", limit = 3)
                val insertPosition = whereAndWhatAndDelimiter.firstOrNull {
                    it.startsWith("+") && it.split("+")[1].isDigitsOnly()
                } ?: ""
                val where =
                    whereAndWhatAndDelimiter.firstOrNull { it.contains("..") } // range inclusive: a..c
                        ?: whereAndWhatAndDelimiter.firstOrNull { it.contains("::") } // inner content: {.}
                        ?: whereAndWhatAndDelimiter.firstOrNull { it.isDigitsOnly() } // position: 2
                        ?: ""
                val what = whereAndWhatAndDelimiter.firstOrNull { it.startsWith("?") }?.drop(1) ?: ""
                val separator = whereAndWhatAndDelimiter.firstOrNull { it.startsWith(":") }?.drop(1) ?: ""

                println("whereAndWhatAndDelimiter: $whereAndWhatAndDelimiter")
                val rangeStartAndEnd =
                    if (where.contains("..")) where.split("..")
                    else if (where.contains("::")) where.split("::")
                    else listOf("")

                val isWhereContentType = where.contains("::") == true
                println("rangeStartAndEnd: $rangeStartAndEnd")
                var start = 0
                var end = 0
                val isDigitRange = rangeStartAndEnd.all { it.isDigitsOnly() || it.isEmpty() }
                println("isDigitRange: $isDigitRange")
                if (isDigitRange) {
                    start = if (rangeStartAndEnd[0].isEmpty())
                        0
                    else
                        rangeStartAndEnd[0].trim().toInt() - 1   // счет мест начинается с 1-го
                    if (start < 0) {
                        start = 0
                    }

                    end = when {
                        insertPosition.isNotEmpty() && rangeStartAndEnd.size == 1 -> { // is insertion ?
                            array.length
                        }

                        insertPosition.isEmpty() && rangeStartAndEnd.size == 1 -> {
                            start + 1
                        }

                        rangeStartAndEnd.size > 1 && rangeStartAndEnd[1].isEmpty() -> {
                            array.length
                        }

                        else -> {
                            rangeStartAndEnd[1].trim().toIntOrNull()
                                ?: array.length
                        }
                    }
                    if (end > array.length) {
                        end = array.length
                    }
                    if (isWhereContentType) {
                        start += 1
                        end -= 1
                    }

                } else { // isLetter
                    if (rangeStartAndEnd[0].isEmpty()) {
                        start = 0
                    } else {
                        start = array.indexOf(rangeStartAndEnd[0])
                        if (start == -1) {
                            start = 0
                        } else {
                            start += 1
                        }
                    }

                    if (rangeStartAndEnd[1].isEmpty()) {
                        end = array.length
                    } else {
                        end = array.indexOf(rangeStartAndEnd[1], start + 1)
                        if (end == -1) {
                            end = array.length
                        } else {
                            end += 2
                        }
                    }

                    if (isWhereContentType) {
                        start += rangeStartAndEnd[0].length
                        end -= rangeStartAndEnd[1].length
                    }
                }
                println("start: $start, end: $end")

                if (insertPosition.isNotEmpty()) {
                    // добавляем элемент по номеру места (остальные сдвигаются)
                    // [x -> +2]abcd // axbcd
                    // [x -> +2 3..9]abcd // axbcd

                    val i = insertPosition.drop(1).toInt() - 1 // count from 1
                    val value = if (separator.isEmpty())
                        StringBuilder(array.substring(max(0, start - 1), max(0, end - 1))).insert(i, value).toString()
                    else {
                        val source =
                            array.substring(max(0, start - 1), max(0, end - 1)).split(separator).toMutableList()
                        source[i] = value
                        source.joinToString(separator)
                    }

                    val result =
                        array.substring(0, max(0, start - 1)) /*+ "|"*/ +
                                value /*+ "|" */ +
                                array.substring(max(0, end - 1), array.length)

                    return result

                } else {
                    val result = {
                        if (isDigitRange) {
                            val source = if (separator.isEmpty())
                                array.toCharArray().map { it.toString() }.toMutableList()
                            else array.split(separator).toMutableList()
                            println("source a: $source")
                            for (i in start until if (end > source.size) source.size else end) {
                                source[i] = if (what.isEmpty())
                                    value
                                else
                                    source[i].replace(what, value)
                            }
                            source.joinToString(separator)
                        } else {
                            val source = if (separator.isEmpty())
                                array.substring(start - 1, end - 1).toCharArray().map { it.toString() }
                                    .toMutableList()
                            else
                                array.substring(start - 1, end - 1).split(separator).toMutableList()
                            for (i in 0 until source.size) {
                                source[i] = if (what.isEmpty())
                                    value
                                else
                                    source[i].replace(what, value)
                            }
                            array.substring(0, start - 1) /*+ "|" */ +
                                    source.joinToString(separator)/* + "|"*/ +
                                    array.substring(end - 1, array.length)
                        }

                        // [X -> {..} ?a :,]c,a bsdaa{a,s,d,a},dsda => Violet
                    }()
                    return result
                }
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

            return if (position >= substrings.size || position < 0)
                ""
            else {
                reduced
            }
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

            return if (position >= substrings.size || position < 0)
                ""
            else {
                reduced
            }
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

            return if (position >= substrings.size || position < 0) ""
                else substrings[position]
        }

        // сколько элементов до разделителя ?.
        // [A?|.]AABB|ABABA // 2 элемента A
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

        // переворачиваем данные(инвертируем порядок)
        // [!]abcd
        op == "!" -> {
            return array.reversed()
        }

        // копируем элемент по номеру места
        // [2]abcd
        op.isDigitsOnly() -> {
            return "${array[op.toInt() - 1]}" // счет мест для заполнения начинается с 1-го
        }

        // копируем ряд по номеру места
        // [2..5]abcd
        !op.startsWith("-") && !op.contains("->") && op.contains("..") -> {
            val leftRight = op.split("..")
            val from = leftRight[0].trim().toInt() - 1
            val to = leftRight[1].trim().toInt()
            if (leftRight.size == 2) {
                return array.substring(from, to) // счет мест для заполнения начинается с 1-го
            }
        }

        // удаляем элемент по номеру места
        // [-2]abcd
        op.startsWith("-") && op.isDigitsOnly('-') -> {
            //                            / заменить оригинал
            return array.removeRange(
                    abs(op.toInt()) - 1, abs(op.toInt())
                ) // счет мест для заполнения начинается с 1-го
        }

        // удаляем ряд по номеру
        // [-2..5]abcd
        op.startsWith("-") && !op.contains("->") && op.contains("..") -> {
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