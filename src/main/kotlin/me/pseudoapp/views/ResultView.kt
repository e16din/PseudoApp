package me.pseudoapp.views

import androidx.compose.foundation.layout.width
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ResultView(code: String) {
    var codeText by mutableStateOf(code)

    TextField(value = codeText, onValueChange = {
        codeText = it
    }, modifier = Modifier.width(360.dp))
}

