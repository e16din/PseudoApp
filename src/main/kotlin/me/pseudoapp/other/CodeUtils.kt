package me.pseudoapp.other

import me.pseudoapp.Element


fun createContainerCode(elements: List<Element>, name: String): String {
    var result = ""
    if (elements.isEmpty()) {
        return result
    }
    val source = elements.toMutableList()

    val tabsDeep = 1
    val content = createContentCode(source.first().inner, source, tabsDeep + 1)
    result += "fun $name() {\n" +
            "${tabs(tabsDeep)}Column {\n" +
            "$content\n" +
            "${tabs(tabsDeep)}}\n" +
            "}\n"

    return result
}

fun createContentCode(inner: List<Element>, all: List<Element>, tabsDeep: Int): String {
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
                "${tabs(tabsDeep)}$name(modifier = Modifier) {\n" +
                        createContentCode(element.inner, all, tabsDeep + 1) +
                        "\n"
            }
        }

        if (element.prompt.value.isNotEmpty()) {
            result += "${tabs(tabsDeep)}// ${element.prompt.value}\n"
        }
        result += getCode(element, tabsDeep)
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

