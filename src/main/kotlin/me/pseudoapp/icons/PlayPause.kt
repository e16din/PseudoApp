package me.pseudoapp.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val playPause: ImageVector
	get() {
		if (_undefined != null) {
			return _undefined!!
		}
		_undefined = ImageVector.Builder(
            name = "PlayPause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
			path(
    			fill = null,
    			fillAlpha = 1.0f,
    			stroke = SolidColor(Color(0xFF0F172A)),
    			strokeAlpha = 1.0f,
    			strokeLineWidth = 1.5f,
    			strokeLineCap = StrokeCap.Round,
    			strokeLineJoin = StrokeJoin.Round,
    			strokeLineMiter = 1.0f,
    			pathFillType = PathFillType.NonZero
			) {
				moveTo(21f, 7.5f)
				lineTo(21f, 18f)
				moveTo(15f, 7.5f)
				verticalLineTo(18f)
				moveTo(3f, 16.8114f)
				verticalLineTo(8.68858f)
				curveTo(3f, 7.8248f, 3.9332f, 7.2832f, 4.6832f, 7.7118f)
				lineTo(11.7906f, 11.7732f)
				curveTo(12.5464f, 12.2051f, 12.5464f, 13.2949f, 11.7906f, 13.7268f)
				lineTo(4.68316f, 17.7882f)
				curveTo(3.9332f, 18.2168f, 3f, 17.6752f, 3f, 16.8114f)
				close()
			}
		}.build()
		return _undefined!!
	}

private var _undefined: ImageVector? = null
