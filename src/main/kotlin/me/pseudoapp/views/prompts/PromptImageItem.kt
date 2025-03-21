package me.pseudoapp.views.prompts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.*
import androidx.compose.runtime.*
import me.pseudoapp.Goal

@Composable
fun PromptImageItem(
    goal: Goal,
    onPromptChanged: (String) -> Unit,
    onRemoveClick: () -> Unit,
    canRemove: Boolean = true
) {
    var isLoadable by remember { mutableStateOf(false) }

    Column {
        Row {
            Checkbox(
                checked = isLoadable,
                onCheckedChange = {
                    isLoadable = it
                },
            )
            Text("Loadable")
        }

        PromptItem(goal,onPromptChanged, onRemoveClick, canRemove)
    }
}