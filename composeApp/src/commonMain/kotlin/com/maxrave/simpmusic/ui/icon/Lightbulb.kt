package com.maxrave.simpmusic.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
val SimpIcons.Lightbulb: ImageVector
  get() {
    if (_Lightbulb != null) {
      return _Lightbulb!!
    }
    _Lightbulb =
      ImageVector.Builder(
          name = "Lightbulb",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(10.59f, 21.41f)
            quadTo(10f, 20.83f, 10f, 20f)
            horizontalLineToRelative(4f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(12f, 22f)
            reflectiveQuadTo(10.59f, 21.41f)
            close()
            moveTo(9f, 19f)
            quadTo(8.58f, 19f, 8.29f, 18.71f)
            quadTo(8f, 18.43f, 8f, 18f)
            reflectiveQuadTo(8.29f, 17.29f)
            quadTo(8.58f, 17f, 9f, 17f)
            horizontalLineToRelative(6f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(16f, 18f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(15f, 19f)
            horizontalLineTo(9f)
            close()
            moveTo(8.25f, 16f)
            quadTo(6.53f, 14.98f, 5.51f, 13.25f)
            quadTo(4.5f, 11.52f, 4.5f, 9.5f)
            quadTo(4.5f, 6.38f, 6.69f, 4.19f)
            reflectiveQuadTo(12f, 2f)
            reflectiveQuadToRelative(5.31f, 2.19f)
            reflectiveQuadTo(19.5f, 9.5f)
            quadToRelative(0f, 2.02f, -1.01f, 3.75f)
            reflectiveQuadTo(15.75f, 16f)
            horizontalLineTo(8.25f)
            close()
          }
        }
        .buildSimpIcon()
    return _Lightbulb!!
  }

private var _Lightbulb: ImageVector? = null
