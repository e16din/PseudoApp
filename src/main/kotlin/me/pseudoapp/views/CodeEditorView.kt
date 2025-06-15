package me.pseudoapp.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
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
    elements: SnapshotStateList<Element>,
    calcState: MutableState<CalcState>
) {

    val instructionsRequester = remember { FocusRequester() }

    var prevAbstractionValues by remember { mutableStateOf(TextFieldValue()) }
    var abstractionsText by remember { mutableStateOf(TextFieldValue()) }

    var resultsText by remember { mutableStateOf(TextFieldValue()) }

    var isCodeUpdated by remember { mutableStateOf(false) }
    var isNextStepAllowed by remember { mutableStateOf(false) }

    var isAbstractionCalled by remember { mutableStateOf(false) }

    var isCodeCompletionEnabled by remember { mutableStateOf(false) }
    var abstracTextFieldPosition by remember { mutableStateOf(Offset.Zero) }
    var resultTextFieldPosition by remember { mutableStateOf(Offset.Zero) }
    var cursorOffsetInTextField by remember { mutableStateOf(Offset.Zero) }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val textStyle = TextStyle.Default


    fun updateTextFields() {
        var abstrActions = ""
        var results = ""
        elements.forEach { e ->
            if (e.isAbstrAction) {
                abstrActions += "${e.text.value} = ${e.name.value}\n"
            } else {
                results += "${e.result.value} = ${e.name.value}\n"
            }
        }

        abstractionsText = TextFieldValue(abstrActions)
        resultsText = TextFieldValue(results)
    }

    if (calcState.value == CalcState.Paused) {
        LaunchedEffect(Unit) {
            updateTextFields()
        }
    } else {

        LaunchedEffect(isAbstractionCalled) {
            if (!isAbstractionCalled) {
                return@LaunchedEffect
            }

            updateTextFields()

            isAbstractionCalled = false
        }
    }

//    LaunchedEffect(Unit) {
//        var abstrActions = ""
//        var results = ""
//        elements.forEach { e ->
//            if (e.isAbstrAction) {
//                abstrActions += "${e.text.value} = ${e.name.value}\n"
//            } else {
//                results += "${e.result.value} = ${e.name.value}\n"
//            }
//        }
//
//        abstractionsText = TextFieldValue(abstrActions)
//        resultsText = TextFieldValue(results)
//    }

    Row(
        Modifier.border(
            1.dp,
            color = Color.DarkGray,
            shape = CutCornerShape(4.dp)
        )
    ) {
        Box(
            Modifier.width(240.dp)
                .padding(8.dp)
        ) {
            BasicTextField(
                value = resultsText,
                textStyle = textStyle,
                onValueChange = {
                    calcState.value = CalcState.Paused
                    resultsText = it
                },
                onTextLayout = { result ->
                    layoutResult = result
                    val cursorPos = resultsText.selection.end
                    if (cursorPos >= 0 && cursorPos <= resultsText.text.length) {
                        val cursorRect = result.getCursorRect(cursorPos)
                        cursorOffsetInTextField = Offset(cursorRect.left, cursorRect.top)
                    }
                },

                modifier = Modifier.fillMaxSize().focusRequester(instructionsRequester)
                    .onGloballyPositioned { coordinates ->
                        resultTextFieldPosition = coordinates.positionInParent()
                    })
        }

        Spacer(Modifier.fillMaxHeight().width(1.dp).background(Color.DarkGray))

        Box(
            Modifier.weight(1f)
                .padding(8.dp)
        ) {
            BasicTextField(
                value = abstractionsText,
                textStyle = textStyle,
                onValueChange = {
                    calcState.value = CalcState.Paused

                    prevAbstractionValues = abstractionsText
                    abstractionsText = it

                    //isElementInserted = false

                    if (layoutResult != null) {
                        val cursorPos = abstractionsText.selection.end
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
                    val cursorPos = abstractionsText.selection.end
                    if (cursorPos >= 0 && cursorPos <= abstractionsText.text.length) {
                        val cursorRect = result.getCursorRect(cursorPos)
                        cursorOffsetInTextField = Offset(cursorRect.left, cursorRect.top)
                    }
                },

                modifier = Modifier.fillMaxSize().focusRequester(instructionsRequester)
                    .onGloballyPositioned { coordinates ->
                        abstracTextFieldPosition = coordinates.positionInParent()
                    })

            val textHeight = measureTextHeight("Height", textStyle)

            val prevDividerIndex = listOf(
                abstractionsText.text.lastIndexOf("\n", abstractionsText.selection.end - 1),
                abstractionsText.text.lastIndexOf(" ", abstractionsText.selection.end - 1),
                abstractionsText.text.lastIndexOf("$", abstractionsText.selection.end - 1),
                abstractionsText.text.lastIndexOf(",", abstractionsText.selection.end - 1),
                abstractionsText.text.lastIndexOf("{", abstractionsText.selection.end - 1),
                abstractionsText.text.lastIndexOf("=", abstractionsText.selection.end - 1),
            ).filter { it >= 0 }.maxByOrNull { it } ?: -1

            val start = prevDividerIndex + 1
            val query = abstractionsText.text.substring(
                startIndex = start,
                endIndex = if (abstractionsText.selection.end > start) abstractionsText.selection.end else start
            )
            val x = abstracTextFieldPosition.x + cursorOffsetInTextField.x
            val y = abstracTextFieldPosition.y + cursorOffsetInTextField.y

            val isCodeCompletionShown = query.length > 1 //&& !isElementInserted
            if (isCodeCompletionShown) {
                val nextDividerIndex = listOf(
                    abstractionsText.text.indexOf("\n", abstractionsText.selection.end - 1),
                    abstractionsText.text.indexOf(" ", abstractionsText.selection.end - 1),
                    abstractionsText.text.indexOf(",", abstractionsText.selection.end - 1),
                ).filter { it >= 0 }.minByOrNull { it } ?: (abstractionsText.text.length)


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
                            StringBuffer(abstractionsText.text).replace(
                                abstractionsText.selection.end,
                                nextDividerIndex,
                                ""
                            )
                                .replace(
                                    if (prevDividerIndex == 0) 0 else prevDividerIndex + 1,
                                    abstractionsText.selection.end,
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
                                    abstractionsText = TextFieldValue(
                                        text = newText,
                                        selection = TextRange(selectorPosition),
                                    )
                                }
                                .padding(horizontal = 8.dp))
                        Divider(color = Color.DarkGray)
                    }
                }
            }


            var totalLinesCount = 0
            var inProgressElement = elements
                .filter { it.isAbstrAction }
                .firstOrNull { element ->
                    element.inProgress.value.also {
                        totalLinesCount += element.text.value.count { it == '\n' } + 1
                    }
                }

            if (inProgressElement != null) {
                val elementLinesCount = inProgressElement!!.text.value.count { it == '\n' } + 1
                Row(
                    Modifier.offset(
                        x = 0.dp,
                        y = measureTextHeight("Height", textStyle, multiply = totalLinesCount - elementLinesCount),
                    ).background(Color.Green.copy(alpha = 0.3f))
                ) {
                    Spacer(
                        Modifier
                            .height(textHeight * elementLinesCount)
                            .fillMaxSize()
                    )
                }
                if (calcState.value == CalcState.InProgress) {
                    isAbstractionCalled = true
                }
            }


/// менять содержимое элемента при редактировании
//            / вынести цикл из Compose функции
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