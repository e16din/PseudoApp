package me.pseudoapp.other

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import me.pseudoapp.Element


fun createContainerCode(elements: List<Element>, name: String): String {
    var result = ""
    if (elements.isEmpty()) {
        return result
    }
    val source = elements.toMutableList()
    fun isBox(element0: Element, element1: Element): Boolean {
        return element0.area.intersectWith(element1.area)
    }

    fun addBoxes(rows: List<List<Element>>) {
        rows.forEachIndexed { i, row ->
            for (j in row.indices) {
                if (j > 0 && isBox(row[j - 1], row[j])) {
                    val inner = mutableListOf(row[j - 1], row[j])
                    val minX = inner.minBy { it.area.topLeft.x }.area.topLeft.x
                    val minY = inner.minBy { it.area.topLeft.y }.area.topLeft.y
                    val maxX = inner.maxBy { it.area.bottomRight.x }.area.bottomRight.x
                    val maxY = inner.maxBy { it.area.bottomRight.y }.area.bottomRight.y

                    source.forEach {
                        it.inner.removeAll(inner)
                    }
                    val boxElement = Element(
                        area = Rect(
                            Offset(minX, minY),
                            Offset(maxX, maxY)
                        ),
                        color = Color.LightGray,
                        type = Element.Type.Box,
                        prompt = mutableStateOf(""),
                        inner = inner
                    )
                    println("Add New Box: ${boxElement}")
                    source.filter { it.area.contains(boxElement.area) }.minBy {
                        it.area.topLeft.x - boxElement.area.topLeft.x
                    }.inner.add(boxElement)
                    source.add(boxElement)
                }

                addBoxes(
                    sortedLines(row[j].inner)
                )
            }
        }
    }

    val rows = sortedLines(elements.first().inner)
    addBoxes(rows)

    val tabsDeep = 1
    val content = createContentCode(source.first().inner, source, tabsDeep + 1)
// todo: before add boxes
    result += "fun $name() {\n" +
            "${tabs(tabsDeep)}Column {\n" +
            "$content\n" +
            "${tabs(tabsDeep)}}\n" +
            "}\n"

    return result
}

fun Element.inner(elements: List<Element>): MutableList<Element> {
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

    val rows = sortedLines(source)


    rows.forEachIndexed { i, row ->

        fun getCode(element: Element, tabsDeep: Int): String {
            val name = element.type.name

            return if (element.inner.isEmpty()) {
                "${tabs(tabsDeep)}$name(modifier = Modifier)\n"
            } else {
                when (element.type) {

                    Element.Type.Text,
                    Element.Type.TextField,
                    Element.Type.Icon,
                    Element.Type.Coil -> {
                        "${tabs(tabsDeep)}Box(modifier = Modifier) {\n" +
                                "${tabs(tabsDeep + 1)}$name(modifier = Modifier)\n" +
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

                    else -> ""
                }
            }
        }

        if (row.size < 2) {
            val element = row.first()
            println("generation1: ${element}")
            result += getCode(element, tabsDeep)

        } else { // Row
            if (row.size == 2 && row[0].area.intersectWith(row[1].area)) {
                for (element in row) {
                    println("generation2: ${element}")
                    result += getCode(element, tabsDeep + 1)
                }
            } else {
                result += "${tabs(tabsDeep)}Row(modifier = Modifier) {\n"
                for (element in row) {
                    println("generation3: ${element}")
                    result += getCode(element, tabsDeep + 1)
                }
                result += "${tabs(tabsDeep)}}\n"
            }
        }
    }

    return result
}

fun sortedLines(source: List<Element>): List<List<Element>> {
    val content = source.toMutableList()
    val result = mutableListOf<List<Element>>()

    while (content.isNotEmpty()) {
        val min = content.minBy { it.area.topLeft.y }
        val row = content.filter { it.area.topLeft.y < min.area.bottomRight.y }
            .sortedBy { it.area.topLeft.x }
        content.removeAll(row)
        result.add(row)
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

