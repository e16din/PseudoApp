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
        get() = size.width + size.height
}

fun Rect.contains(area: Rect): Boolean {
    return this.topLeft.x < area.topLeft.x
            && this.topLeft.y < area.topLeft.y
            && this.bottomRight.x > area.bottomRight.x
            && this.bottomRight.y > area.bottomRight.y
}

fun Rect.contains(dot: Offset): Boolean {
    return this.topLeft.x < dot.x
            && this.topLeft.y < dot.y
            && this.bottomRight.x > dot.x
            && this.bottomRight.y > dot.y
}

fun Rect.intersectWith(rect: Rect): Boolean {
    println("this(${this})")
    println("rect(${rect})")

    return this.contains(rect.bottomRight)
            || this.contains(rect.topLeft)
            || this.contains(Offset(rect.bottomRight.x, rect.topLeft.y))
            || this.contains(Offset(rect.topLeft.x, rect.bottomRight.y))
            //
            || rect.contains(this.bottomRight)
            || rect.contains(this.topLeft)
            || rect.contains(Offset(this.bottomRight.x, this.topLeft.y))
            || rect.contains(Offset(this.topLeft.x, this.bottomRight.y))


}


fun Rect.isInnerOf(area: Rect): Boolean {
    return area.topLeft.x < this.topLeft.x
            && area.topLeft.y < this.topLeft.y
            && area.bottomRight.x > this.bottomRight.x
            && area.bottomRight.y > this.bottomRight.y
}

fun rectOf(list: List<Rect>): Rect {
    val startX = list.minBy { it.topLeft.x }.topLeft.x
    val startY = list.minBy { it.topLeft.y }.topLeft.y
    val endX = list.maxBy { it.bottomRight.x }.bottomRight.x
    val endY = list.maxBy { it.bottomRight.y }.bottomRight.y
    val area = Rect(
        Offset(startX, startY),
        Offset(endX, endY)
    )
    return area
}

fun rectOf(space: Int = 0, vararg list: Rect): Rect {
    val startX = list.minBy { it.topLeft.x }.topLeft.x
    val startY = list.minBy { it.topLeft.y }.topLeft.y
    val endX = list.maxBy { it.bottomRight.x }.bottomRight.x
    val endY = list.maxBy { it.bottomRight.y }.bottomRight.y
    val area = Rect(
        Offset(startX - space, startY - space),
        Offset(endX + space, endY + space)
    )
    return area
}