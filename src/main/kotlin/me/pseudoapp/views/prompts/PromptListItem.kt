package me.pseudoapp.views.prompts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.*
import me.pseudoapp.Element

@Composable
fun PromptListItem(
    element: Element,
    onPromptChanged: (String) -> Unit,
    onRemoveClick: () -> Unit,
    canRemove: Boolean = true
) {
    var isLazy by remember { mutableStateOf(false) }
    Column {
        Row {
            Checkbox(
                checked = isLazy,
                onCheckedChange = {
                    isLazy = it
                },
            )
            Text("Lazy")
        }

        PromptItem(element, onPromptChanged, onRemoveClick, canRemove)
    }
}