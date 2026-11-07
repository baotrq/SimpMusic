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
val SimpIcons.Key: ImageVector
  get() {
    if (_Key != null) {
      return _Key!!
    }
    _Key =
      ImageVector.Builder(
          name = "Key",
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
            moveTo(4.88f, 14.13f)
            quadTo(4f, 13.25f, 4f, 12f)
            reflectiveQuadTo(4.88f, 9.88f)
            reflectiveQuadTo(7f, 9f)
            reflectiveQuadTo(9.13f, 9.88f)
            reflectiveQuadTo(10f, 12f)
            reflectiveQuadTo(9.13f, 14.13f)
            reflectiveQuadTo(7f, 15f)
            reflectiveQuadTo(4.88f, 14.13f)
            close()
            moveTo(7f, 18f)
            quadToRelative(1.93f, 0f, 3.48f, -1.1f)
            reflectiveQuadTo(12.65f, 14f)
            horizontalLineTo(13f)
            lineToRelative(1.3f, 1.3f)
            quadToRelative(0.15f, 0.15f, 0.32f, 0.21f)
            reflectiveQuadTo(15f, 15.58f)
            reflectiveQuadToRelative(0.38f, -0.06f)
            reflectiveQuadTo(15.7f, 15.3f)
            lineTo(17f, 14f)
            lineToRelative(1.75f, 1.38f)
            quadToRelative(0.15f, 0.13f, 0.34f, 0.19f)
            reflectiveQuadToRelative(0.39f, 0.04f)
            reflectiveQuadToRelative(0.36f, -0.11f)
            reflectiveQuadToRelative(0.29f, -0.24f)
            lineToRelative(2.25f, -2.58f)
            quadToRelative(0.13f, -0.15f, 0.19f, -0.32f)
            reflectiveQuadToRelative(0.06f, -0.38f)
            quadToRelative(0f, -0.2f, -0.07f, -0.36f)
            reflectiveQuadToRelative(-0.2f, -0.29f)
            lineTo(21.33f, 10.3f)
            quadTo(21.18f, 10.15f, 20.99f, 10.07f)
            reflectiveQuadTo(20.6f, 10f)
            horizontalLineTo(12.65f)
            quadTo(12.05f, 8.3f, 10.54f, 7.15f)
            reflectiveQuadTo(7f, 6f)
            quadTo(4.5f, 6f, 2.75f, 7.75f)
            reflectiveQuadTo(1f, 12f)
            reflectiveQuadToRelative(1.75f, 4.25f)
            reflectiveQuadTo(7f, 18f)
            close()
          }
        }
        .buildSimpIcon()
    return _Key!!
  }

private var _Key: ImageVector? = null
