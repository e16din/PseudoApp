package me.pseudoapp.other

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun copyToClipboard(text: String) {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(StringSelection(text), null)
}

fun pickImage(onResult: (ImageBitmap?) -> Unit) {
    val fileDialog = FileDialog(Frame(), "Выберите изображение", FileDialog.LOAD)
    fileDialog.isVisible = true
    val filePath = fileDialog.directory + fileDialog.file
    println(filePath)
    if (filePath.isNotEmpty()) {
        val image: BufferedImage = ImageIO.read(File(filePath))
        onResult(image.toComposeImageBitmap())
    } else {
        onResult(null)
    }
}

@Composable
fun measureTextWidth(text: String, style: TextStyle = TextStyle.Default): Dp {
    val textMeasurer = rememberTextMeasurer()
    val widthInPixels = textMeasurer.measure(text, style).size.width
    return with(LocalDensity.current) { widthInPixels.toDp() }
}


@Composable
fun measureTextHeight(text: String, style: TextStyle = TextStyle.Default, multiply: Int = 1): Dp {
    val textMeasurer = rememberTextMeasurer()
    val widthInPixels = textMeasurer.measure(text, style).size.height * multiply
    return with(LocalDensity.current) { widthInPixels.toDp() }
}

@Composable
fun Float.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }


@Composable
fun Dp.dpToPx() = with(LocalDensity.current) { this@dpToPx.toPx() }
