package me.pseudoapp.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val play: ImageVector
    get() {
        if (_undefined != null) {
            return _undefined!!
        }
        _undefined = ImageVector.Builder(
            name = "Play",
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
                moveTo(5.25f, 5.65273f)
                curveTo(5.25f, 4.797f, 6.1674f, 4.2546f, 6.9172f, 4.667f)
                lineTo(18.4577f, 11.0143f)
                curveTo(19.2349f, 11.4417f, 19.2349f, 12.5584f, 18.4577f, 12.9858f)
                lineTo(6.91716f, 19.3331f)
                curveTo(6.1674f, 19.7455f, 5.25f, 19.203f, 5.25f, 18.3474f)
                verticalLineTo(5.65273f)
                close()
            }
        }.build()
        return _undefined!!
    }

private var _undefined: ImageVector? = null
