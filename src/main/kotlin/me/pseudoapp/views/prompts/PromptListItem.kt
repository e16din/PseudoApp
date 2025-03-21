package me.pseudoapp.views.prompts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.*
import me.pseudoapp.Goal

@Composable
fun PromptListItem(
    goal: Goal,
    onPromptChanged: (String) -> Unit,
    onRemoveClick: () -> Unit,
    canRemove: Boolean = true
) {
    var isHorizontal by remember { mutableStateOf(false) }
    var isLazy by remember { mutableStateOf(false) }
    Column {
        Row {
            Checkbox(
                checked = isHorizontal,
                onCheckedChange = {
                    isHorizontal = it
                },
            )
            Text("Horizontal")
        }

        Row {
            Checkbox(
                checked = isLazy,
                onCheckedChange = {
                    isLazy = it
                },
            )
            Text("Lazy")
        }

        PromptItem(goal, onPromptChanged, onRemoveClick, canRemove)
    }
}