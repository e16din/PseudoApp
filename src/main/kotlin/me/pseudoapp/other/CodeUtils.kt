package me.pseudoapp.other

import me.pseudoapp.Element

var screensCount = 0
var customElementsCount = 0
var tabsDeep = 0

var handledContent = mutableListOf<Element>()

fun createContainerCode(elements: List<Element>, name: String): String {
    var result = ""

    tabsDeep = 1

    val tabs = tabs(tabsDeep)
    val content = createContentCode(elements)

    result += "fun $name() {\n" +
            "$tabs$content\n" +
            "}\n\n"


    handledContent.clear()

    return result
}

fun Element.innerGoals(elements: List<Element>): List<Element> {
    val inner = mutableListOf<Element>()
    elements.forEach { j ->
        if (this.area.contains(j.area)) {
            inner.add(j)
        }
    }
    return inner
}

class Node(val nodes: MutableList<Node> = mutableListOf())

fun createContentCode(source: List<Element>): String {
    // NOTE: Обход дерева - это линия, список.
    // Сортировкой элементов можно получить последовательность обхода дерева
    // не преобразуя список в дерево
    val elements = source.sortedWith(Comparator { t, t2 ->
        if (t.area.topLeft.y < t2.area.topLeft.y
            && t.area.topLeft.x < t2.area.topLeft.x
        ) {
            -1
        } else {
            1
        }
    })

    var result = ""
    for (element in elements) {
        if (!handledContent.contains(element)) {
            val name = element.type.name
            val tabs = tabs(tabsDeep)
            val inner = element.innerGoals(elements)
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

    return result
}

fun tabs(deep: Int): String {
    var result = ""
    repeat(deep) {
        result += "\t"
    }
    return result
}

