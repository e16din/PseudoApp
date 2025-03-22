package me.pseudoapp.other

import me.pseudoapp.Element
import me.pseudoapp.Goal

var screensCount = 0
var customElementsCount = 0
var tabsDeep = 0

var handledContent = mutableListOf<Goal>()

fun createContainerCode(goals: List<Goal>): String {
    var result = ""

    val containers = goals.filter {
        it.element.type == Element.Type.Screen
                || it.element.type == Element.Type.CustomView
    }.reversed() // NOTE: to correct check a handledContent items

    containers.forEach { goal ->
        tabsDeep = 1
        val name = goal.element.name
        val tabs = tabs(tabsDeep)

        val inner = goal.innerGoals(goals)
        val content = createContentCode(inner)

        result += "fun $name() {\n" +
                "$tabs$content\n" +
                "}\n\n"
    }

    handledContent.clear()

    return result
}

fun Goal.innerGoals(goals: List<Goal>): List<Goal> {
    val inner = mutableListOf<Goal>()
    goals.forEach { j ->
        if (this.area.contains(j.area)) {
            inner.add(j)
        }
    }
    return inner
}

fun createContentCode(goals: List<Goal>): String {
    var result = ""
    for (goal in goals) {
        if (handledContent.contains(goal)) {
            continue
        }
        handledContent.add(goal)

        val name = goal.element.name
        val tabs = tabs(tabsDeep)

        result += when (goal.element.type) {
            Element.Type.CustomView,
            Element.Type.Screen -> {
                "\n$tabs$name()\n\n"
            }

            else -> {
                val inner = goal.innerGoals(goals)
                if (inner.isEmpty()) {
                    "$tabs$name(modifier = Modifier)\n"
                } else {
                    "$tabs$name(modifier = Modifier) {\n" +
                            createContentCode(inner) +
                            "}\n"
                }
            }
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

