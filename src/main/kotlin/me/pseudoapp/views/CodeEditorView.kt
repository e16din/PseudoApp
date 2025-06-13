package me.pseudoapp.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import me.pseudoapp.Element
import me.pseudoapp.other.measureTextHeight

const val unknown = "?"

@Composable
fun CodeEditorView(
    elements: SnapshotStateList<Element>
) {

    val instructionsRequester = remember { FocusRequester() }
    var prevCodeValue by remember { mutableStateOf(TextFieldValue()) }
    var codeValue by remember { mutableStateOf(TextFieldValue()) }
    var stepDelayMsValue by remember { mutableStateOf("") }
    var isPaused by remember { mutableStateOf(false) }
    var isCodeUpdated by remember { mutableStateOf(false) }
    var isNextStepAllowed by remember { mutableStateOf(false) }

    var isCodeCompletionEnabled by remember { mutableStateOf(false) }
    var textFieldPosition by remember { mutableStateOf(Offset.Zero) }
    var cursorOffsetInTextField by remember { mutableStateOf(Offset.Zero) }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val textStyle = TextStyle.Default

    LaunchedEffect(Unit) {
        var code = ""
        elements.forEach { e ->
            code += if (e.isAbstrAction) {
                "${e.text.value} = ${e.name.value}\n"
            } else {
                "${e.result.value} = ${e.name.value}\n\n"
            }
        }

        codeValue = TextFieldValue(code)
    }

    Box {
        BasicTextField(
            value = codeValue,
            textStyle = textStyle,
            onValueChange = {
                prevCodeValue = codeValue
                codeValue = it

                //isElementInserted = false

                if (layoutResult != null) {
                    val cursorPos = codeValue.selection.end
                    try {
                        val cursorRect = layoutResult!!.getCursorRect(cursorPos)
                        cursorOffsetInTextField = Offset(cursorRect.left, cursorRect.top)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                isCodeCompletionEnabled = true

            },
            onTextLayout = { result ->
                layoutResult = result
                val cursorPos = codeValue.selection.end
                if (cursorPos >= 0 && cursorPos <= codeValue.text.length) {
                    val cursorRect = result.getCursorRect(cursorPos)
                    cursorOffsetInTextField = Offset(cursorRect.left, cursorRect.top)
                }
            },

            modifier = Modifier.fillMaxSize().focusRequester(instructionsRequester)
                .onGloballyPositioned { coordinates ->
                    textFieldPosition = coordinates.positionInParent()
                })

        val textHeight = measureTextHeight("Height", textStyle)

        val prevDividerIndex = listOf(
            codeValue.text.lastIndexOf("\n", codeValue.selection.end - 1),
            codeValue.text.lastIndexOf(" ", codeValue.selection.end - 1),
            codeValue.text.lastIndexOf("$", codeValue.selection.end - 1),
            codeValue.text.lastIndexOf(",", codeValue.selection.end - 1),
            codeValue.text.lastIndexOf("{", codeValue.selection.end - 1),
            codeValue.text.lastIndexOf("=", codeValue.selection.end - 1),
        ).filter { it >= 0 }.maxByOrNull { it } ?: -1

        val start = prevDividerIndex + 1
        val query = codeValue.text.substring(
            startIndex = start, endIndex = if (codeValue.selection.end > start) codeValue.selection.end else start
        )
        val x = textFieldPosition.x + cursorOffsetInTextField.x
        val y = textFieldPosition.y + cursorOffsetInTextField.y

        val isCodeCompletionShown = query.length > 1 //&& !isElementInserted
        if (isCodeCompletionShown) {
            val nextDividerIndex = listOf(
                codeValue.text.indexOf("\n", codeValue.selection.end - 1),
                codeValue.text.indexOf(" ", codeValue.selection.end - 1),
                codeValue.text.indexOf(",", codeValue.selection.end - 1),
            ).filter { it >= 0 }.minByOrNull { it } ?: (codeValue.text.length)


            Column(
                Modifier.offset(
                    x = x.dp - 16.dp,
                    y = y.dp + textHeight,
                ).width(200.dp)
                    .background(Color(0xFF141414))

            ) {
                val filtered = elements.filter { it.name.value != query && it.name.value.contains(query) }
                filtered.forEach { element ->
                    val newText =
                        StringBuffer(codeValue.text).replace(codeValue.selection.end, nextDividerIndex, "")
                            .replace(
                                if (prevDividerIndex == 0) 0 else prevDividerIndex + 1,
                                codeValue.selection.end,
                                ""
                            ).insert(
                                if (prevDividerIndex == 0) 0 else prevDividerIndex + 1, element.name.value
                            ).toString()

                    Text(
                        element.name.value,
                        color = Color.White,
                        modifier = Modifier
                            .clickable {
                                //isElementInserted = true
                                isCodeCompletionEnabled = false
                                val selectorPosition =
                                    (prevDividerIndex + if (prevDividerIndex == 0) 0 else 1) + element.name.value.length
                                instructionsRequester.requestFocus()
                                codeValue = TextFieldValue(
                                    text = newText,
                                    selection = TextRange(selectorPosition),
                                )
                            }
                            .padding(horizontal = 8.dp))
                    Divider(color = Color.DarkGray)
                }
            }

            val activeElementIndex = remember { elements.indexOfFirst { it.inProgress.value } }
            if (activeElementIndex != -1) {
                Row(
                    Modifier.offset(
                        x = 0.dp,
                        y = measureTextHeight("Height", textStyle, multiply = activeElementIndex),
                    ).background(Color.Green.copy(alpha = 0.6f))
                ) {
                    Spacer(
                        Modifier
                            .height(textHeight)
                            .fillMaxSize()
                    )
                }
            }


            Row(
                Modifier.offset(
                    x = 0.dp,
                    y = y.dp,
                ).background(Color.LightGray.copy(alpha = 0.3f))
            ) {
                Spacer(
                    Modifier
                        .height(textHeight)
                        .fillMaxSize()
                )
            }
        }

    }
}