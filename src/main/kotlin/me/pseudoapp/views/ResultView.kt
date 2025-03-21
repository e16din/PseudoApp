package me.pseudoapp.views

import androidx.compose.foundation.layout.width
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.pseudoapp.Element
import me.pseudoapp.Goal
import me.pseudoapp.other.contains

@Composable
fun ResultView(code: String) {
    println("ResultView: $code")
    var codeText by mutableStateOf(code)

    TextField(value = codeText, onValueChange = {
        codeText = it
    }, modifier = Modifier.width(360.dp))
}

fun createScreenCode(goals: SnapshotStateList<Goal>): String {
    var result = "fun "
    result += createCode(goals)
    result += "\n"

    return result
}

fun createCode(goals: List<Goal>): String {
    var result = ""
    val handled = mutableListOf<Goal>()
    goals.forEach { i ->
        if (!handled.contains(i)) {
            val inner = mutableListOf<Goal>()
            goals.forEach { j ->
                if (i.area.contains(j.area)) {
                    inner.add(j)
                    handled.add(j)
                }
            }

            val name = if (i.element.tag != null && i.element.tag != "") i.element.tag else i.element.type.name
            val args = if (inner.isNotEmpty()) createCode(inner) else ""
            if (args.isEmpty()) {
                result += "$name(modifier = Modifier)\n"
            } else {
                result += "$name() {\n" +
                        "$args\n" +
                        "}\n"
            }
            if (i.element.type == Element.Type.CustomView) {
                result += "fun $name($args) {\n" +
                        "}\n\n"
            }
        }
    }
    return result
}