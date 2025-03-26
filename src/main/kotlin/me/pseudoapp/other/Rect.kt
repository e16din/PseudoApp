package me.pseudoapp.other

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

data class Rect(
    var topLeft: Offset = Offset.Zero,
    var bottomRight: Offset = Offset.Zero
) {
    val size: Size
        get() = Size(bottomRight.x - topLeft.x, bottomRight.y - topLeft.y)
    val count
        get() = size.width+size.height
}

fun Rect.contains(area: Rect): Boolean {
    return this.topLeft.x < area.topLeft.x
            && this.topLeft.y < area.topLeft.y
            && this.bottomRight.x > area.bottomRight.x
            && this.bottomRight.y > area.bottomRight.y
}

fun Rect.isInnerOf(area: Rect): Boolean {
    return area.topLeft.x < this.topLeft.x
            && area.topLeft.y < this.topLeft.y
            && area.bottomRight.x > this.bottomRight.x
            && area.bottomRight.y > this.bottomRight.y
}