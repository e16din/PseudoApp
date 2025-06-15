package me.pseudoapp

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color


data class Element(
    var name: MutableState<String>,
    var inProgress: MutableState<Boolean> = mutableStateOf(false),
    var text: MutableState<String>,// ? :a + 2 = 10 + 2
    var result: MutableState<String>,
    var area: MutableState<Rect>,
    val color: Color,
    val isAbstrAction: Boolean = true, // or Result // Circle/Rect
    val isFilled: Boolean = true,
//    val createdMs: Long = System.currentTimeMillis(),
    val elements: SnapshotStateList<Element> = mutableStateListOf<Element>()
)