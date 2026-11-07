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
val SimpIcons.CompareArrows: ImageVector
  get() {
    if (_CompareArrows != null) {
      return _CompareArrows!!
    }
    _CompareArrows =
      ImageVector.Builder(
          name = "CompareArrows",
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
            moveTo(9.18f, 16f)
            horizontalLineTo(3f)
            quadTo(2.58f, 16f, 2.29f, 15.71f)
            reflectiveQuadTo(2f, 15f)
            reflectiveQuadTo(2.29f, 14.29f)
            reflectiveQuadTo(3f, 14f)
            horizontalLineTo(9.18f)
            lineTo(7.3f, 12.13f)
            quadTo(7.03f, 11.85f, 7.03f, 11.44f)
            quadToRelative(0f, -0.41f, 0.28f, -0.71f)
            quadToRelative(0.3f, -0.3f, 0.71f, -0.3f)
            reflectiveQuadToRelative(0.71f, 0.3f)
            lineTo(12.3f, 14.3f)
            quadToRelative(0.15f, 0.15f, 0.21f, 0.33f)
            reflectiveQuadTo(12.58f, 15f)
            reflectiveQuadToRelative(-0.06f, 0.38f)
            reflectiveQuadTo(12.3f, 15.7f)
            lineTo(8.7f, 19.3f)
            quadTo(8.4f, 19.6f, 8f, 19.59f)
            quadTo(7.6f, 19.58f, 7.3f, 19.27f)
            quadTo(7.03f, 18.98f, 7.01f, 18.58f)
            quadTo(7f, 18.18f, 7.3f, 17.88f)
            lineTo(9.18f, 16f)
            close()
            moveToRelative(5.65f, -6f)
            lineToRelative(1.87f, 1.88f)
            quadToRelative(0.28f, 0.27f, 0.28f, 0.69f)
            reflectiveQuadTo(16.7f, 13.27f)
            quadToRelative(-0.3f, 0.3f, -0.71f, 0.3f)
            reflectiveQuadToRelative(-0.71f, -0.3f)
            lineTo(11.7f, 9.7f)
            quadTo(11.55f, 9.55f, 11.49f, 9.38f)
            reflectiveQuadTo(11.43f, 9f)
            reflectiveQuadTo(11.49f, 8.63f)
            reflectiveQuadTo(11.7f, 8.3f)
            lineTo(15.3f, 4.7f)
            quadTo(15.6f, 4.4f, 16f, 4.41f)
            reflectiveQuadToRelative(0.7f, 0.31f)
            quadToRelative(0.28f, 0.3f, 0.29f, 0.7f)
            quadTo(17f, 5.82f, 16.7f, 6.13f)
            lineTo(14.83f, 8f)
            horizontalLineTo(21f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(22f, 9f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(21f, 10f)
            horizontalLineTo(14.83f)
            close()
          }
        }
        .buildSimpIcon()
    return _CompareArrows!!
  }

private var _CompareArrows: ImageVector? = null
