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
val SimpIcons.SurroundSound: ImageVector
  get() {
    if (_SurroundSound != null) {
      return _SurroundSound!!
    }
    _SurroundSound =
      ImageVector.Builder(
          name = "SurroundSound",
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
            moveTo(14.13f, 14.13f)
            quadTo(15f, 13.25f, 15f, 12f)
            reflectiveQuadTo(14.13f, 9.88f)
            reflectiveQuadTo(12f, 9f)
            reflectiveQuadTo(9.88f, 9.88f)
            reflectiveQuadTo(9f, 12f)
            reflectiveQuadToRelative(0.88f, 2.13f)
            reflectiveQuadTo(12f, 15f)
            reflectiveQuadToRelative(2.13f, -0.88f)
            close()
            moveTo(17f, 12f)
            quadToRelative(0f, 0.75f, -0.2f, 1.45f)
            reflectiveQuadToRelative(-0.63f, 1.33f)
            quadToRelative(-0.22f, 0.35f, -0.21f, 0.77f)
            quadToRelative(0.01f, 0.43f, 0.29f, 0.7f)
            quadToRelative(0.3f, 0.3f, 0.7f, 0.29f)
            quadToRelative(0.4f, -0.01f, 0.65f, -0.34f)
            quadToRelative(0.68f, -0.9f, 1.04f, -1.97f)
            reflectiveQuadTo(19f, 12f)
            reflectiveQuadTo(18.64f, 9.77f)
            quadTo(18.28f, 8.7f, 17.6f, 7.8f)
            quadTo(17.35f, 7.47f, 16.95f, 7.46f)
            reflectiveQuadToRelative(-0.7f, 0.29f)
            quadToRelative(-0.27f, 0.27f, -0.29f, 0.7f)
            reflectiveQuadToRelative(0.21f, 0.78f)
            quadToRelative(0.43f, 0.63f, 0.63f, 1.32f)
            reflectiveQuadTo(17f, 12f)
            close()
            moveTo(7.2f, 10.55f)
            quadTo(7.4f, 9.85f, 7.83f, 9.23f)
            quadTo(8.05f, 8.88f, 8.04f, 8.45f)
            quadTo(8.03f, 8.02f, 7.75f, 7.75f)
            quadTo(7.45f, 7.45f, 7.05f, 7.46f)
            reflectiveQuadTo(6.4f, 7.8f)
            quadTo(5.73f, 8.7f, 5.36f, 9.77f)
            reflectiveQuadTo(5f, 12f)
            reflectiveQuadToRelative(0.36f, 2.22f)
            reflectiveQuadTo(6.4f, 16.2f)
            quadToRelative(0.25f, 0.32f, 0.65f, 0.34f)
            reflectiveQuadToRelative(0.7f, -0.29f)
            quadToRelative(0.28f, -0.28f, 0.29f, -0.7f)
            quadTo(8.05f, 15.13f, 7.83f, 14.78f)
            quadTo(7.4f, 14.15f, 7.2f, 13.45f)
            reflectiveQuadTo(7f, 12f)
            reflectiveQuadTo(7.2f, 10.55f)
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
        .buildSimpIcon()
    return _SurroundSound!!
  }

private var _SurroundSound: ImageVector? = null
