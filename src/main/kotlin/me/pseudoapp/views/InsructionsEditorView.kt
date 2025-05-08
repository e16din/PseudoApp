package me.pseudoapp.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import me.pseudoapp.Element
import me.pseudoapp.other.measureTextHeight

@Composable
fun InsructionsEditorView(
    elements: SnapshotStateList<Element>
) {
    val instructionsRequester = remember { FocusRequester() }
    var codeValue by remember { mutableStateOf(TextFieldValue()) }
//    var isElementInserted by remember { mutableStateOf(false) }
    var isCodeCompletionEnabled by remember { mutableStateOf(false) }
    var textFieldPosition by remember { mutableStateOf(Offset.Zero) } // позиция TextField на экране
    var cursorOffsetInTextField by remember { mutableStateOf(Offset.Zero) } // позиция курсора внутри TextField
    var textFieldSize by remember { mutableStateOf(IntSize.Zero) }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val textStyle = TextStyle.Default

    fun calc(operation: String): String { // simple implementation
        val pair = operation.split("+")

        try {
            return (pair[0].trim().toInt() + pair[1].trim().toInt()).toString()
        } catch (e: Exception) {
            return "Not Implement"
        }
    }

    fun updateValues() {
        // detect value changed
        codeValue.text.split("\n").forEach { line ->
            val startIndex = line.indexOf("(")
            val endIndex = line.indexOf(")")
            if (startIndex != -1 && endIndex != -1 && startIndex + 1 != endIndex) {
                var value = line.substring(startIndex + 1, endIndex)

                var startMathIndex = value.indexOf("{", 0)
                var endMathIndex = value.indexOf("}", 0)
                while (startMathIndex != -1 && endMathIndex != -1) {
                    if (startMathIndex + 1 != endMathIndex) {
                        val builder = StringBuilder(value)
                            .replace(
                                startMathIndex, endMathIndex + 1,
                                calc(value.substring(startMathIndex + 1, endMathIndex))
                            )

                        value = builder.toString()
                    }

                    startMathIndex = value.indexOf("{", endMathIndex + 1)
                    endMathIndex = value.indexOf("}", endMathIndex + 1)
                }

                val prevDividerIndex = listOf(
                    codeValue.text.lastIndexOf("\n", codeValue.selection.end - 1),
                    codeValue.text.lastIndexOf(" ", codeValue.selection.end - 1),
                    codeValue.text.lastIndexOf(",", codeValue.selection.end - 1),
                ).filter { it >= 0 }.maxByOrNull { it } ?: 0
                println("line: $line")
                val name = line.substring(if (prevDividerIndex > startIndex) 0 else prevDividerIndex, startIndex)

                println("value: $value")
                println("name: $name")
                val element = elements.firstOrNull { it.name == name }
                element?.let {
                    elements.remove(element)
                    elements.add(element.copy(value = value))
                }
            }
        }
    }

    // {2+3} # 5
    // {2+3} + 12w + {3*3} # 512w9
    // RedCircle({2+3}) # RedCircle(5)
    // 2+3 # 23

    @Composable
    fun completeCode() {
        val prevDividerIndex = listOf(
            codeValue.text.lastIndexOf("\n", codeValue.selection.end - 1),
            codeValue.text.lastIndexOf(" ", codeValue.selection.end - 1),
            codeValue.text.lastIndexOf(",", codeValue.selection.end - 1),
        ).filter { it >= 0 }.maxByOrNull { it } ?: 0

        val start = prevDividerIndex + 1
        val query = codeValue.text.substring(
            startIndex = start, endIndex = if (codeValue.selection.end > start) codeValue.selection.end else start
        )
        val isCodeCompletionShown = query.length > 1 //&& !isElementInserted
        if (isCodeCompletionShown) {
            val x = textFieldPosition.x + cursorOffsetInTextField.x
            val y = textFieldPosition.y + cursorOffsetInTextField.y

            val nextDividerIndex = listOf(
                codeValue.text.indexOf("\n", codeValue.selection.end - 1),
                codeValue.text.indexOf(" ", codeValue.selection.end - 1),
                codeValue.text.indexOf(",", codeValue.selection.end - 1),
            ).filter { it >= 0 }.minByOrNull { it } ?: (codeValue.text.length)

            Column(
                Modifier.offset(
                    x = x.dp - 16.dp,
                    y = y.dp + measureTextHeight("Height", textStyle),
                ).background(Color(0xFF141414))
                // NOTE: todo: клик показывать не на кнопке а там где нажал, если попадает в кнопку то срабатывает

            ) {


                val filtered = elements.filter { it.name.contains(query) }
                filtered.forEach { element ->
                    val newText =
                        StringBuffer(codeValue.text).replace(codeValue.selection.end, nextDividerIndex, "")
                            .replace(
                                if (prevDividerIndex == 0) 0 else prevDividerIndex + 1,
                                codeValue.selection.end,
                                ""
                            ).insert(
                                if (prevDividerIndex == 0) 0 else prevDividerIndex + 1, element.name
                            ).toString()

                    Text(
                        element.name, color = Color.White, modifier = Modifier.clickable {
                            //isElementInserted = true
                            isCodeCompletionEnabled = false
                            val selectorPosition =
                                (prevDividerIndex + if (prevDividerIndex == 0) 0 else 1) + element.name.length
                            instructionsRequester.requestFocus()
                            codeValue = TextFieldValue(
                                text = newText,
                                selection = TextRange(selectorPosition),
                            )
                        })
                    Divider()
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box {
            BasicTextField(
                value = codeValue,
                textStyle = textStyle,
                onValueChange = {
                    codeValue = it

                    //isElementInserted = false

                    val cursorPos = codeValue.selection.end
                    if (layoutResult != null && cursorPos >= 0 && cursorPos <= codeValue.text.length) {
                        try {
                            val cursorRect = layoutResult!!.getCursorRect(cursorPos)
                            cursorOffsetInTextField = Offset(cursorRect.left, cursorRect.top)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    isCodeCompletionEnabled = true
                    updateValues()

                }, onTextLayout = { result ->
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
                        textFieldSize = coordinates.size
                    })

            if (isCodeCompletionEnabled) {
                completeCode()
            }
        }
    }
}