package me.pseudoapp.other

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import me.pseudoapp.Element
import me.pseudoapp.findContainerRectOf
import me.pseudoapp.findInner


fun createContainerCode(elements: List<Element>, name: String): String {
    var result = ""
    if (elements.isEmpty()) {
        return result
    }
    val source = elements.toMutableList()
    if (source.size > 1) {
//        addContainers(source.first().inner, source)
    }

    val tabsDeep = 1
    val content = createContentCode(source.first().inner, source, tabsDeep + 1)
    result += "fun $name() {\n" +
            "${tabs(tabsDeep)}Column {\n" +
            "$content\n" +
            "${tabs(tabsDeep)}}\n" +
            "}\n"

    return result
}

fun addContainers(inner: MutableList<Element>, all: MutableList<Element>) {
    println("addContainers(${inner.map { it.type }})")

    if (inner.isEmpty()) {
        return
    }

    val rootColumnInner = mutableListOf<Element>()
    val rootColumnArea = rectOf(list = inner.map { it.area })
    val rootElementColumn = Element(
        area = rootColumnArea,
        color = Color.Cyan,
        type = Element.Type.Column,
        prompt = mutableStateOf(""),
        inner = rootColumnInner
    )
    // разобьем все на строки(встретим боксы добавим),
    // внутри разобьем все на столбцы(встретим боксы добавим),
    // для элементов столбцов снова вызовем addContainer
    val rows = sortedRows(inner)
    if (rows.isEmpty()) {
        return
    }

    for (row in rows) {
        if (row.isEmpty()) {
            continue
        }

        val columns = sortedColumns(row)
        if (columns.isEmpty()) {
            continue
        }

        val rowInner = mutableListOf<Element>()
        val rowArea = rectOf(list = row.map { it.area })
        val elementRow = Element(
            area = rowArea,
            color = Color.Magenta,
            type = Element.Type.Row,
            prompt = mutableStateOf(""),
            inner = rowInner
        )

        for (column in columns) {
            if (column.isEmpty()) {
                continue
            }

            if (column.size == 1) {
                removeFromAllInner(all, column)
//                all.forEach { element ->
//                    element.inner.removeAll(column)
//                }
                rowInner.add(column.first())
                println("debug1: add single column!")

                column.forEach {
                    val nextInner = it.findInner(all)
                    addContainers(nextInner, inner)
                }
                continue
            }

            val columnArea = rectOf(list = column.map { it.area })
            val elementColumn = Element(
                area = columnArea,
                color = Color.Cyan,
                type = Element.Type.Column,
                prompt = mutableStateOf(""),
                inner = column.toMutableList()
            )
            all.forEach {
                it.inner.removeAll(column)
            }
            rowInner.add(elementColumn)
            println("debug1: add column!")

            column.forEach {
                val nextInner = it.findInner(all)
                addContainers(nextInner, inner)
            }
        }

        removeFromAllInner(all, elementRow.inner)

        all.add(elementRow)
        rootColumnInner.add(elementRow)
        println("debug1: add row!")
        val container = all.findContainerRectOf(elementRow)
        container?.let {
            container.inner.add(elementRow)

            when (container.type) {
                Element.Type.Row -> container.inner.sortBy { it.area.topLeft.x }
                Element.Type.Column -> container.inner.sortBy { it.area.topLeft.y }
                else -> {}
            }
        }
    }

    removeFromAllInner(all, rootColumnInner)
    all.add(rootElementColumn)

    println("debug1: add root column!")
    val container = all.findContainerRectOf(rootElementColumn)
    container?.let {
        container.inner.add(rootElementColumn)

        when (container.type) {
            Element.Type.Row -> container.inner.sortBy { it.area.topLeft.x }
            Element.Type.Column -> container.inner.sortBy { it.area.topLeft.y }
            else -> {}
        }
    }
//    }
}

private fun removeFromAllInner(
    all: MutableList<Element>,
    removed: List<Element>
) {
    all.forEach {
        removeFromAllInner(it.inner, removed)
        it.inner.removeAll(removed)
    }
}




fun isBox(element0: Element, element1: Element): Boolean {
    return element0.area.intersectWith(element1.area)
}

fun createContentCode(inner: List<Element>, all: List<Element>, tabsDeep: Int): String {
//    println("createContentCode(${inner.map { it.type }})")
    var result = ""

    if (inner.isEmpty()) {
        return result
    }


    inner.forEach { element ->

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

        result += getCode(element, tabsDeep)

//        if (row.size < 2) {
//            val element = row.first()
//
//
//        } else { // Row
////            if (row.size == 2 && row[0].area.intersectWith(row[1].area)) {
//            for (element in row) {
//                result += getCode(element, tabsDeep + 1)
//            }
////            } else {
////                result += "${tabs(tabsDeep)}Row(modifier = Modifier) {\n"
////                for (element in row) {
////                    println("generation3: ${element}")
////                    result += getCode(element, tabsDeep + 1)
////                }
////                result += "${tabs(tabsDeep)}}\n"
////            }
//        }
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
        val row = content.filter {
            it.area.topLeft.y < min.area.bottomRight.y
        }.sortedBy { it.area.topLeft.x }
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
        val column = content.filter {
            it.area.topLeft.x < min.area.bottomRight.x
        }.sortedBy { it.area.topLeft.y }.toMutableList()
        content.removeAll(column)
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

