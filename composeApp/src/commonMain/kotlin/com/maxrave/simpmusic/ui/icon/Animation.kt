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
val SimpIcons.Animation: ImageVector
  get() {
    if (_Animation != null) {
      return _Animation!!
    }
    _Animation =
      ImageVector.Builder(
          name = "Animation",
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
            moveTo(9f, 22f)
            quadTo(7.55f, 22f, 6.28f, 21.45f)
            reflectiveQuadTo(4.05f, 19.95f)
            reflectiveQuadTo(2.55f, 17.73f)
            reflectiveQuadTo(2f, 15f)
            quadTo(2f, 12.98f, 3.05f, 11.3f)
            quadTo(4.1f, 9.63f, 5.8f, 8.75f)
            quadTo(6.3f, 7.77f, 7.04f, 7.04f)
            reflectiveQuadTo(8.75f, 5.8f)
            quadTo(9.58f, 4.1f, 11.28f, 3.05f)
            reflectiveQuadTo(15f, 2f)
            quadToRelative(1.45f, 0f, 2.73f, 0.55f)
            reflectiveQuadToRelative(2.23f, 1.5f)
            reflectiveQuadToRelative(1.5f, 2.23f)
            reflectiveQuadTo(22f, 9f)
            quadToRelative(0f, 2.13f, -1.05f, 3.75f)
            reflectiveQuadToRelative(-2.75f, 2.5f)
            quadToRelative(-0.5f, 0.98f, -1.24f, 1.71f)
            reflectiveQuadTo(15.25f, 18.2f)
            quadToRelative(-0.88f, 1.7f, -2.55f, 2.75f)
            reflectiveQuadTo(9f, 22f)
            close()
            moveTo(9f, 20f)
            quadToRelative(0.83f, 0f, 1.59f, -0.25f)
            reflectiveQuadTo(12f, 19f)
            quadTo(10.55f, 19f, 9.28f, 18.45f)
            reflectiveQuadTo(7.05f, 16.95f)
            reflectiveQuadTo(5.55f, 14.73f)
            reflectiveQuadTo(5f, 12f)
            quadTo(4.5f, 12.65f, 4.25f, 13.41f)
            reflectiveQuadTo(4f, 15f)
            quadToRelative(0f, 1.05f, 0.4f, 1.95f)
            reflectiveQuadToRelative(1.08f, 1.57f)
            reflectiveQuadTo(7.05f, 19.6f)
            reflectiveQuadTo(9f, 20f)
            close()
            moveToRelative(3f, -3f)
            quadToRelative(0.83f, 0f, 1.61f, -0.25f)
            reflectiveQuadTo(15.05f, 16f)
            quadTo(13.58f, 16f, 12.3f, 15.44f)
            reflectiveQuadTo(10.08f, 13.93f)
            quadTo(9.13f, 12.98f, 8.56f, 11.7f)
            reflectiveQuadTo(8f, 8.95f)
            quadTo(7.5f, 9.6f, 7.25f, 10.39f)
            reflectiveQuadTo(7f, 12f)
            quadToRelative(0f, 1.05f, 0.39f, 1.95f)
            reflectiveQuadToRelative(1.09f, 1.58f)
            quadToRelative(0.68f, 0.7f, 1.58f, 1.09f)
            reflectiveQuadTo(12f, 17f)
            close()
          }
        }
        .buildSimpIcon()
    return _Animation!!
  }

private var _Animation: ImageVector? = null
