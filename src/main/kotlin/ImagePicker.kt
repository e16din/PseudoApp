

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.FileDialog
import java.awt.Frame
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class ImagePicker {
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
}