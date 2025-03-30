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
    if (source.size > 1) {
        addContainers(source.first().inner, source)

//        addRowsTo(source.first().inner, source)
//        addColumnsTo(source.first().inner, source)
//        addBoxesTo(source.first().inner, source)
    }

//    val rows = sortedLines(elements.first().inner)
//    addBoxes(rows, source)

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

fun addBoxesTo(inner: MutableList<Element>, all: MutableList<Element>) {
//    там где есть пересечения любых
//            и там где есть перекрытие не-контейнеров
    val lines = sortedBoxes(inner)
    for (line in lines) {
        if (line.size < 2) {
            continue
        }
        val area = createContainerFor(line)
        val element = Element(
            area = area,
            color = Color.Magenta,
            type = Element.Type.Box,
            prompt = mutableStateOf(""),
            inner = line.toMutableList()
        )
        all.forEach {
            it.inner.removeAll(line)
        }
        all.add(element)
        println("debug1: add box!")

        val container = all.findContainerOf(element)
        container?.let {
            container.inner.add(element)

            when (container.type) {
                Element.Type.Row -> container.inner.sortBy { it.area.topLeft.x }
                Element.Type.Column -> container.inner.sortBy { it.area.topLeft.y }
                else -> {}
            }
        }

        line.forEach {
            addBoxesTo(it.inner, all)
        }
    }
}

fun addColumnsTo(inner: MutableList<Element>, all: MutableList<Element>) {
    val lines = sortedColumns(inner)
    for (line in lines) {
        if (line.size < 2) {
            continue
        }

        val area = createContainerFor(line)
        val element = Element(
            area = area,
            color = Color.DarkGray,
            type = Element.Type.Column,
            prompt = mutableStateOf(""),
            inner = line.toMutableList()
        )
        all.forEach {
            it.inner.removeAll(line)
        }
        all.add(element)
        println("debug1: add column!")

        val container = all.findContainerOf(element)
        container?.let {
            container.inner.add(element)

            when (container.type) {
                Element.Type.Row -> container.inner.sortBy { it.area.topLeft.x }
                Element.Type.Column -> container.inner.sortBy { it.area.topLeft.y }
                else -> {}
            }
        }

        line.forEach {
            addColumnsTo(it.inner, all)
        }
    }
}

private fun createContainerFor(line: List<Element>): Rect {
    val startX = line.minBy { it.area.topLeft.x }.area.topLeft.x
    val startY = line.minBy { it.area.topLeft.y }.area.topLeft.y
    val endX = line.maxBy { it.area.bottomRight.x }.area.bottomRight.x
    val endY = line.maxBy { it.area.bottomRight.y }.area.bottomRight.y
    val area = Rect(
        Offset(startX, startY),
        Offset(endX, endY)
    )
    return area
}

fun addContainers(inner: MutableList<Element>, all: MutableList<Element>) {
    val lines = sortedColumns(inner)
    if (lines.isEmpty()) {
        return
    }

    val rowInner = mutableListOf<Element>()
    val startX = lines.first().minBy { it.area.topLeft.x }.area.topLeft.x
    val startY = lines.first().minBy { it.area.topLeft.y }.area.topLeft.y
    val endX = lines.last().maxBy { it.area.bottomRight.x }.area.bottomRight.x
    val endY = lines.last().maxBy { it.area.bottomRight.y }.area.bottomRight.y
    val rowArea = Rect(
        Offset(startX, startY),
        Offset(endX, endY)
    )
    val elementRow = Element(
        area = rowArea,
        color = Color.Magenta,
        type = Element.Type.Row,
        prompt = mutableStateOf(""),
        inner = rowInner
    )

    for (line in lines) {
        if (line.isEmpty()) { // if empty or single than do nothing
            continue
        } // else if 2 or more than add a Column

        if (line.size == 1) {
            all.forEach {
                it.inner.removeAll(line)
            }
            rowInner.add(line.first())
            continue
        }

        val columnArea = createContainerFor(line)
        val elementColumn = Element(
            area = columnArea,
            color = Color.Cyan,
            type = Element.Type.Column,
            prompt = mutableStateOf(""),
            inner = line.toMutableList()
        )
        all.forEach {
            it.inner.removeAll(line)
        }
        rowInner.add(elementColumn)
        println("debug1: add row!")

        line.forEach {
            addContainers(it.inner, all)
        }
    }

    all.add(elementRow)
    val container = all.findContainerOf(elementRow)
    container?.let {
        container.inner.add(elementRow)

        when (container.type) {
            Element.Type.Row -> container.inner.sortBy { it.area.topLeft.x }
            Element.Type.Column -> container.inner.sortBy { it.area.topLeft.y }
            else -> {}
        }
    }
}

fun addRowsTo(inner: MutableList<Element>, all: MutableList<Element>) {
    val lines = sortedRows(inner)
    for (line in lines) {
        if (line.size < 2) {
            continue
        }

//        if(line[])

        val area = createContainerFor(line)
        val element = Element(
            area = area,
            color = Color.Cyan,
            type = Element.Type.Row,
            prompt = mutableStateOf(""),
            inner = line.toMutableList()
        )
        all.forEach {
            it.inner.removeAll(line)
        }
        all.add(element)
        println("debug1: add row!")

        val container = all.findContainerOf(element)
        container?.let {
            container.inner.add(element)

            when (container.type) {
                Element.Type.Row -> container.inner.sortBy { it.area.topLeft.x }
                Element.Type.Column -> container.inner.sortBy { it.area.topLeft.y }
                else -> {}
            }
        }

        line.forEach {
            addRowsTo(it.inner, all)
        }
    }
}

private fun List<Element>.findContainerOf(
    element: Element
): Element? {
    println("debug: ${this.size}")
    println("debug: ${this}")
    return if (this.size <= 2) {
        null
    } else {
        val filtered = this.filter { it.area.contains(element.area) && it.isContainer() }
        if (filtered.isEmpty()) {
            return null
        } else {
            filtered.minBy {
                it.area.topLeft.x - element.area.topLeft.x
            }
        }
    }
}

fun isBox(element0: Element, element1: Element): Boolean {
    return element0.area.intersectWith(element1.area)
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

    val rows = sortedRows(source)


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
                    Element.Type.Coil,
//                    Element.Type.Coil -> {
//                        "${tabs(tabsDeep)}Box(modifier = Modifier) {\n" +
//                                "${tabs(tabsDeep + 1)}$name(modifier = Modifier)\n" +
//                                createContentCode(element.inner, all, tabsDeep + 1) +
//                                "${tabs(tabsDeep)}}\n"
//                    }

                    Element.Type.Button,
                    Element.Type.Box,
                    Element.Type.Row,
                    Element.Type.Column -> {
                        "${tabs(tabsDeep)}$name(modifier = Modifier) {\n" +
                                createContentCode(element.inner, all, tabsDeep + 1) +
                                "${tabs(tabsDeep)}}\n"
                    }
                }
            }
        }

        if (row.size < 2) {
            val element = row.first()
            println("generation1: ${element}")
            result += getCode(element, tabsDeep)

        } else { // Row
//            if (row.size == 2 && row[0].area.intersectWith(row[1].area)) {
            for (element in row) {
                println("generation2: ${element}")
                result += getCode(element, tabsDeep + 1)
            }
//            } else {
//                result += "${tabs(tabsDeep)}Row(modifier = Modifier) {\n"
//                for (element in row) {
//                    println("generation3: ${element}")
//                    result += getCode(element, tabsDeep + 1)
//                }
//                result += "${tabs(tabsDeep)}}\n"
//            }
        }
    }

    return result
}

fun sortedBoxes(source: List<Element>): List<List<Element>> {
    val result = mutableListOf<MutableList<Element>>()
    val rows = sortedRows(source)

    rows.forEach { row ->
        for (j in row.indices) {
            if (j == 0) {
                continue
            }

            val pair = mutableListOf(row[j - 1], row[j])
            val max = pair.maxBy { it.area.count }
            val min = pair.minBy { it.area.count }
            if (
                isBox(row[j - 1], row[j]) && !max.area.contains(min.area)
//                ||
//                (!max.isContainer() && max.inner(row).size == 1 && max.area.contains(min.area))
            ) {
                result.add(pair)
            }
        }
    }

    return rows
}

fun sortedRows(source: List<Element>): List<List<Element>> {
    val content = source.toMutableList()
    val result = mutableListOf<MutableList<Element>>()

    while (content.isNotEmpty()) {
        val min = content.minBy { it.area.topLeft.y }
        val row = content.filter { it.area.topLeft.y < min.area.bottomRight.y }
            .sortedBy { it.area.topLeft.x }
        content.removeAll(row)
        result.add(row.toMutableList())
    }

    return result
}

fun sortedColumns(source: List<Element>): List<List<Element>> {
    val content = source.toMutableList()
    val result = mutableListOf<MutableList<Element>>()

    while (content.isNotEmpty()) {
        val min = content.minBy { it.area.topLeft.x }
        var exclude = emptyList<Element>()
        if(content.size>=2) {
            val filtered = content.filter { it.area.topLeft.x > min.area.bottomRight.x }
            if(filtered.isNotEmpty()) {
                val min2 = filtered.minBy { it.area.topLeft.x }
                println("debug3: ${min.area}")
                println("debug31: ${min2.area}")

                exclude = content.filter {
                    it.area.bottomRight.x > min2.area.topLeft.x
                }
            }
        }

        val column = content.filter {
            it.area.topLeft.x < min.area.bottomRight.x
        }.sortedBy { it.area.topLeft.y }.toMutableList()


        content.removeAll(column)

        column.removeAll(exclude)
        println("debug32: ${exclude.map { it.area }}")
        println("debug33: ${column.map { it.area }}")
        result.add(column.toMutableList())
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

