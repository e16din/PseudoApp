package me.pseudoapp.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.pseudoapp.Instruction
import me.pseudoapp.RainbowColor

@Composable
fun InstructionsLineView(
    instructions: SnapshotStateList<Instruction>
) {
    instructions.add(
        Instruction(
            text = " = ",
            type = Instruction.Type.NameValue
        )
    )
    instructions.add(
        Instruction(
            text = " => ",
            type = Instruction.Type.ResetValue
        )
    )

    instructions.add(
        Instruction(
            text = " < ",
            type = Instruction.Type.StartCondition
        )
    )

    instructions.add(
        Instruction(
            text = ".",
            type = Instruction.Type.EndCondition
        )
    )

    @Composable
    fun buttons(position: Int, withCondition: Boolean = true) {
        Row {
            val nameValue = " = "
            Text(
                nameValue,
                color = Color.White,
                style = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(RainbowColor.Blue.color)
                    .padding(end = 4.dp)
                    .width(36.dp)
                    .clickable {
                        instructions.add(
                            position,
                            Instruction(
                                text = nameValue,
                                type = Instruction.Type.NameValue
                            )
                        )
                    }
            )

            Spacer(Modifier.width(8.dp))

            val resetValue = " => "
            Text(
                resetValue,
                color = Color.White,
                style = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(RainbowColor.Red.color)
                    .padding(end = 4.dp)
                    .width(36.dp)
                    .clickable {
                        instructions.add(
                            position,
                            Instruction(
                                text = resetValue,
                                type = Instruction.Type.ResetValue
                            )
                        )
                    }
            )

            Spacer(Modifier.width(8.dp))

            if (withCondition) {
                val startCondition = " ? "
                val endCondition = "."
                Text(
                    startCondition,
                    color = Color.White,
                    style = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(RainbowColor.Violet.color)
                        .padding(end = 4.dp)
                        .width(36.dp)
                        .clickable {
                            instructions.add(
                                position,
                                Instruction(
                                    text = startCondition,
                                    type = Instruction.Type.StartCondition
                                )
                            )
                            instructions.add(
                                position + 1,
                                Instruction(
                                    text = endCondition,
                                    type = Instruction.Type.EndCondition
                                )
                            )
                        }
                )
            }
        }
    }

    LazyColumn(
        Modifier.padding(8.dp),
    ) {


        itemsIndexed(instructions) { i, instruction ->
            if (instruction.type == Instruction.Type.EndCondition) {
                Text(
                    instruction.text,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .dashedBorder(
                            RainbowColor.Violet.color,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(start = 4.dp)
                        .width(24.dp)
                )
            } else {
                Column(Modifier.padding(bottom = 8.dp)) {
                    val color = when (instruction.type) {
                        Instruction.Type.NameValue -> RainbowColor.Blue.color
                        Instruction.Type.ResetValue -> RainbowColor.Red.color
                        Instruction.Type.StartCondition -> RainbowColor.Violet.color
                        else -> throw IllegalStateException()
                    }

                    BasicTextField(
                        instruction.text,
                        onValueChange = {
                            instructions[i] = instruction.copy(text = it)
                        },
                        textStyle = TextStyle.Default.copy(
                            textAlign = TextAlign.Start,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier
                            .dashedBorder(
                                color,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(4.dp)
                            .width(240.dp)
                    )

                    if (instruction.type == Instruction.Type.StartCondition) {
                        var endIndex = 0
                        for (index in i until instructions.size) {
                            val it = instructions[index]
                            if (it.type == Instruction.Type.EndCondition) {
                                endIndex = index
                                break
                            }
                        }
                        buttons(endIndex, withCondition = false)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(12.dp))

            buttons(instructions.size)
        }

    }
}