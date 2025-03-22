package me.pseudoapp.other

import androidx.compose.runtime.snapshots.SnapshotStateList
import me.pseudoapp.Element
import me.pseudoapp.Goal


fun createScreenCode(goals: SnapshotStateList<Goal>): String {
    var result = createCode(goals)
    result += "\n"

    goals.forEach { goal ->
        if (goal.element.type == Element.Type.CustomView) {
            val inner = mutableListOf<Goal>()
            goals.forEach { j ->
                if (goal.area.contains(j.area)) {
                    inner.add(j)
                }
            }

            val content = createCode(inner)
            val name = if (goal.element.tag != null && goal.element.tag != "")
                goal.element.tag
            else
                goal.element.type.name
            val customViewsCount = goals.count { it.element.type == Element.Type.CustomView }
            result += "fun $name$customViewsCount() {\n" +
                    "$content\n" +
                    "}\n\n"
        }
    }

    return result
}

fun createCode(goals: List<Goal>): String {
    var result = ""
    val handled = mutableListOf<Goal>()
    goals.forEach { goal ->
        if (!handled.contains(goal)) {
            val inner = mutableListOf<Goal>()
            goals.forEach { j ->
                if (goal.area.contains(j.area)) {
                    inner.add(j)
                    handled.add(j)
                }
            }

            val name = if (goal.element.tag != null && goal.element.tag != "")
                goal.element.tag
            else
                goal.element.type.name
            val content = if (inner.isNotEmpty())
                createCode(inner)
            else
                ""

            when (goal.element.type) {
                Element.Type.Screen -> {
                    result += "fun $name() {\n" +
                            "$content\n" +
                            "}\n\n"
                }

                Element.Type.CustomView -> {
                    val customViewsCount = goals.count { it.element.type == Element.Type.CustomView }
                    result += "\n$name$customViewsCount()\n\n"
                }

                else -> {
                    result += if (content.isEmpty()) {
                        "$name(modifier = Modifier)\n"
                    } else {
                        "$name(modifier = Modifier) {\n" +
                                "$content\n" +
                                "}\n"
                    }
                }
            }
        }
    }
    return result
}