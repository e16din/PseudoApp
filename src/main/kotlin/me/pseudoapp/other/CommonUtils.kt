package me.pseudoapp.other

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

object SizeObserver {
    private val _oneDp = MutableStateFlow(1f)
    val oneDP = _oneDp.asStateFlow()
    val oneDpValue get() = _oneDp.value

    @Composable
    operator fun invoke() {
        Box(
            modifier = Modifier
                .size(10.dp)
                .onSizeChanged { _oneDp.value = it.width / 10f }
        )
    }
}

fun Float.convertToPx(): Float = (this / SizeObserver.oneDpValue)