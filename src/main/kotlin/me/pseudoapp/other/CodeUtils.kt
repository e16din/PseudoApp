package me.pseudoapp.other

import me.pseudoapp.Element

var screensCount = 0
var customElementsCount = 0


fun createContainerCode(elements: List<Element>, name: String): String {
    var result = ""
    if (elements.isEmpty()) {
        return result
    }
    val tabsDeep = 1
    val content = createContentCode(elements.first().inner, elements, tabsDeep + 1)

    result += "fun $name() {\n" +
            "${tabs(tabsDeep)}Column {\n" +
            "$content\n" +
            "${tabs(tabsDeep)}}\n" +
            "}\n"

    return result
}

fun Element.inner(elements: List<Element>): List<Element> {
    val result = mutableListOf<Element>()
    elements.forEach { el ->
        if (this.area.contains(el.area) && !result.any { it.area.contains(el.area) }) {
            result.add(el)
        }
    }
    return result
}

fun createContentCode(source: List<Element>, all: List<Element>, tabsDeep: Int): String {
    var result = ""

    if (source.isEmpty()) {
        return result
    }

    val rows = sortedRows(source)


    rows.forEach {

        fun getCode(element: Element, i: Int, tabsDeep: Int): String {
            val name = element.type.name

            return if (element.inner.isEmpty()) {
                "${tabs(tabsDeep)}$name(modifier = Modifier) // $i \n"
            } else {
                when (element.type) {
                    Element.Type.Text,
                    Element.Type.TextField,
                    Element.Type.Icon,
                    Element.Type.Coil -> {

                        "${tabs(tabsDeep)}Box(modifier = Modifier) {\n" +
                                "$${tabs(tabsDeep + 1)}$name(modifier = Modifier)\n" +
                                createContentCode(element.inner, all, tabsDeep + 1) +
                                "}\n"
                    }

                    Element.Type.Button,
                    Element.Type.Box,
                    Element.Type.Row,
                    Element.Type.Column -> {
                        "${tabs(tabsDeep)}$name(modifier = Modifier) {\n" +
                                createContentCode(element.inner, all, tabsDeep + 1) +
                                "}\n"
                    }
                }
            }
        }

        val elements = it
        if (elements.size < 2) {
            result += getCode(elements.first(), 0, tabsDeep)

        } else if (elements.size == 2 &&
            (elements[0].area.intersectWith(elements[1].area) && !elements[0].area.contains(elements[1].area))
        ) { // Box
            result += "${tabs(tabsDeep)}Box(modifier = Modifier) {\n"
            for ((i, element) in elements.withIndex()) {
                result += getCode(element, i, tabsDeep + 1)
            }
            result += "${tabs(tabsDeep)}}\n"

        } else { // Row
            result += "${tabs(tabsDeep)}Row(modifier = Modifier) {\n"
            for ((i, element) in elements.withIndex()) {
                result += getCode(element, i, tabsDeep + 1)
            }
            result += "${tabs(tabsDeep)}}\n"
        }
    }

    return result
}

fun sortedRows(source: List<Element>): List<List<Element>> {
    val content = source.toMutableList()
    val result = mutableListOf<List<Element>>()

    while (content.isNotEmpty()) {
        val min = content.minBy { it.area.topLeft.y }
        val row = content.filter { it.area.topLeft.y < min.area.bottomRight.y }
            .sortedBy { it.area.topLeft.x }
        content.removeAll(row)
        result.add(row)
        row.forEach {
            print("addRow: ")
            print("${it.type}, ")
        }
        println()
    }

    return result
}

fun tabs(deep: Int): String {
    var result = ""
    repeat(deep) {
        result += "\t"
    }
    return result
}

