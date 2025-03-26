package me.pseudoapp.other

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.util.fastAny
import me.pseudoapp.Element
import me.pseudoapp.views.nextColor

var screensCount = 0
var customElementsCount = 0
var tabsDeep = 0

var handledContent = mutableListOf<Element>()


fun createContainerCode(elements: List<Element>, name: String): String {
    var result = ""
    if (elements.isEmpty()) {
        return result
    }

    tabsDeep = 1

    val tabs = tabs(tabsDeep)
    val content = createContentCode(elements.first().inner)

    result += "fun $name() {\n" +
            "$tabs$content\n" +
            "}\n\n"


    handledContent.clear()

    return result
}

fun Element.inner(elements: List<Element>): List<Element> {
    val result = mutableListOf<Element>()
    elements.forEach { el ->
        if(this.area.contains(el.area) && !result.any { it.area.contains(el.area)}){
            result.add(el)
        }
    }
    return result
}

class Node(val nodes: MutableList<Node> = mutableListOf())

fun createContentCode(source: List<Element>): String {
    if (source.isEmpty()) {
        return ""
    }

    val elements = source

    var result = ""
    var i = 0
    for (element in elements) {
        if (!handledContent.contains(element)) {
            val name = element.type.name + i
            i++
            val tabs = tabs(tabsDeep)
            val inner = sortedElements(element.inner(elements))
            result +=

                if (inner.isEmpty()) {
                    "$tabs$name(modifier = Modifier)\n"
                } else {
                    when (element.type) {
                        Element.Type.Text,
                        Element.Type.TextField,
                        Element.Type.Icon,
                        Element.Type.Coil -> {
                            "${tabs}Box(modifier = Modifier) {\n" +
                                    "$tabs$name(modifier = Modifier)\n" +
                                    createContentCode(inner) +
                                    "}\n"
                        }

                        Element.Type.Button,
                        Element.Type.Box,
                        Element.Type.Row,
                        Element.Type.Column -> {
                            "$tabs$name(modifier = Modifier) {\n" +
                                    createContentCode(inner) +
                                    "}\n"
                        }
                    }
                }
            handledContent.addAll(inner)
        }
    }

    source.forEach {
        val inner = it.inner(elements)
        it.inner = inner
        println(inner)
        createContentCode(inner)
    }

    return result
}

private fun sortedElements(source: List<Element>): MutableList<Element> {
    return mutableListOf<Element>().apply {
        sortedRows(source).forEach {
            addAll(it)
        }
    }
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

