package me.pseudoapp.views.prompts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.pseudoapp.Goal

@Composable
fun PromptItem(
    goal: Goal,
    onPromptChanged: (String) -> Unit,
    onRemoveClick: () -> Unit,
    canRemove: Boolean = true
) {
    val elementName = goal.element.name
    Text(elementName)
    Box {
        TextField(
            value = goal.prompt.value,
            label = { Text("Prompt") },
            onValueChange = { value ->
                goal.prompt.value = value
                onPromptChanged(value)
            },
            modifier = Modifier.padding(vertical = 8.dp)
        )
        if (canRemove) {
            IconButton(onClick = onRemoveClick, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Default.Close, "clear")
            }
        }
    }
}