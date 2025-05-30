package me.pseudoapp

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color


data class Element(
    var name: String,
    var condition: String,
    var action: String,// value
    var result: String,
    val area: Rect,
    val color: Color,
    val isAbstract: Boolean = true, // Circle\Rect
    val isFilled: Boolean = true, // Real\Abstract
    val createdMs: Long = System.currentTimeMillis()
)