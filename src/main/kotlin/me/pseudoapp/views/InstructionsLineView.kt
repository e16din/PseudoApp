package me.pseudoapp.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
    @Composable
    fun buttons(position: Int, withCondition: Boolean = true) {
        Row(Modifier.padding(top = 8.dp, start = if (withCondition) 0.dp else 24.dp)) {
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
                                type = Instruction.Type.NameValue,
                                position = instructions.size,
                                inCondition = !withCondition
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
                                type = Instruction.Type.ResetValue,
                                position = instructions.size,
                                inCondition = !withCondition
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
                                    type = Instruction.Type.StartCondition,
                                    position = instructions.size
                                )
                            )
                            instructions.add(
                                position + 1,
                                Instruction(
                                    text = endCondition,
                                    type = Instruction.Type.EndCondition,
                                    position = instructions.size
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
                Column(Modifier.padding(bottom = 8.dp, start = if (instruction.inCondition) 24.dp else 0.dp)) {
                    val color = when (instruction.type) {
                        Instruction.Type.NameValue -> RainbowColor.Blue.color
                        Instruction.Type.ResetValue -> RainbowColor.Red.color
                        Instruction.Type.StartCondition -> RainbowColor.Violet.color
                        else -> throw IllegalStateException()
                    }

                    Row {

                        Text(
                            "+",
                            color = Color.White,
                            style = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.LightGray.copy(alpha = 0.5f))
//                                .padding(end = 4.dp)
                                .width(16.dp)
                                .clickable {
                                    //add buttons
                                }
                        )

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

                        fun reformat() {
                            var inCondition = false
                            for (index in 0 until instructions.size) {
                                val it = instructions[index]
                                if (it.type == Instruction.Type.EndCondition) {
                                    inCondition = false
                                    continue
                                } else if (it.type == Instruction.Type.StartCondition) {
                                    inCondition = true
                                    continue
                                }

                                instructions[index] = it.copy(inCondition = inCondition)
                            }

                        }

                        IconButton(
                            onClick = {
                                instructions.removeAt(i)
                                if (i != instructions.size) {
                                    instructions[i] = instructions[i].copy(position = i)
                                }
                                instructions.add(i + 1, instruction.copy(position = i + 1))
                                reformat()
                            },
                            content = {
                                Icon(Icons.Default.KeyboardArrowDown, "Down")
                            }
                        )
                        IconButton(
                            onClick = {
                                instructions.removeAt(i)
                                if (i != instructions.size) {
                                    instructions[i] = instructions[i].copy(position = i)
                                }
                                instructions.add(i - 1, instruction.copy(position = i - 1))
                                reformat()
                            },
                            content = {
                                Icon(Icons.Default.KeyboardArrowUp, "Up")
                            }
                        )
                        Spacer(Modifier.width(24.dp))

                        IconButton(
                            onClick = {
                                instructions.removeAt(i)
                                if (i != instructions.size) {
                                    instructions[i] = instructions[i].copy(position = i)
                                }

                                if (instruction.type == Instruction.Type.StartCondition) {
                                    for (index in i until instructions.size) {
                                        if (instructions[index].type == Instruction.Type.EndCondition) {
                                            instructions.removeAt(index)
                                            if(index != instructions.size) {
                                                instructions[index] = instructions[index].copy(position = index)
                                            }
                                            break
                                        }
                                    }
                                }

                                reformat()
                            },
                            content = {
                                Icon(Icons.Default.Delete, "Delete")
                            }
                        )
                    }

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