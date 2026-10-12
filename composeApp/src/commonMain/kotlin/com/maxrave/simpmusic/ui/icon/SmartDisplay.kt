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
val SimpIcons.SmartDisplay: ImageVector
  get() {
    if (_SmartDisplay != null) {
      return _SmartDisplay!!
    }
    _SmartDisplay =
      ImageVector.Builder(
          name = "SmartDisplay",
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
            moveTo(10.28f, 16f)
            lineToRelative(5.58f, -3.58f)
            quadTo(16.08f, 12.27f, 16.08f, 12f)
            reflectiveQuadTo(15.85f, 11.58f)
            lineTo(10.28f, 8f)
            quadTo(10.03f, 7.82f, 9.76f, 7.97f)
            quadTo(9.5f, 8.13f, 9.5f, 8.42f)
            verticalLineToRelative(7.15f)
            quadToRelative(0f, 0.3f, 0.26f, 0.45f)
            quadTo(10.03f, 16.18f, 10.28f, 16f)
            close()
            moveTo(4f, 20f)
            quadTo(3.18f, 20f, 2.59f, 19.41f)
            reflectiveQuadTo(2f, 18f)
            verticalLineTo(6f)
            quadTo(2f, 5.18f, 2.59f, 4.59f)
            reflectiveQuadTo(4f, 4f)
            horizontalLineTo(20f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(22f, 5.18f, 22f, 6f)
            verticalLineTo(18f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(20f, 20f)
            horizontalLineTo(4f)
            close()
          }
        }
        .build()
    return _SmartDisplay!!
  }

private var _SmartDisplay: ImageVector? = null
