package me.pseudoapp

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color


data class Element(
    var name: MutableState<String>,
    var condition: MutableState<String>,
    var action: MutableState<String>,
    var value: MutableState<String>,
    val area: Rect,
    val color: Color,
    val isAbstract: Boolean = true, // Circle\Rect
    val isFilled: Boolean = true, // Real\Abstract
//    val createdMs: Long = System.currentTimeMillis(),
    val elements: SnapshotStateList<Element> = mutableStateListOf<Element>()
)