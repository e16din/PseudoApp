package me.pseudoapp

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color


data class Element(
    var name: String,
    var value: String,
    val area: Rect,
    val color: Color,
    val isCircle: Boolean = true,
    val isFilled: Boolean = true
)