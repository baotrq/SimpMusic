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
val SimpIcons.Password: ImageVector
  get() {
    if (_Password != null) {
      return _Password!!
    }
    _Password =
      ImageVector.Builder(
          name = "Password",
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
            moveTo(3f, 17f)
            horizontalLineTo(21f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(22f, 18f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(21f, 19f)
            horizontalLineTo(3f)
            quadTo(2.58f, 19f, 2.29f, 18.71f)
            quadTo(2f, 18.43f, 2f, 18f)
            reflectiveQuadTo(2.29f, 17.29f)
            reflectiveQuadTo(3f, 17f)
            close()
            moveTo(4f, 11.45f)
            lineTo(3.53f, 12.3f)
            quadTo(3.38f, 12.58f, 3.08f, 12.65f)
            quadTo(2.78f, 12.73f, 2.5f, 12.58f)
            quadTo(2.23f, 12.43f, 2.15f, 12.13f)
            quadTo(2.08f, 11.83f, 2.23f, 11.55f)
            lineTo(2.7f, 10.7f)
            horizontalLineTo(1.75f)
            quadToRelative(-0.32f, 0f, -0.54f, -0.21f)
            reflectiveQuadTo(1f, 9.95f)
            reflectiveQuadTo(1.21f, 9.41f)
            reflectiveQuadTo(1.75f, 9.2f)
            horizontalLineTo(2.7f)
            lineTo(2.23f, 8.4f)
            quadTo(2.08f, 8.13f, 2.15f, 7.82f)
            quadTo(2.23f, 7.52f, 2.5f, 7.38f)
            quadTo(2.78f, 7.22f, 3.08f, 7.3f)
            reflectiveQuadTo(3.53f, 7.65f)
            lineTo(4f, 8.45f)
            lineTo(4.48f, 7.65f)
            quadTo(4.63f, 7.38f, 4.93f, 7.3f)
            quadTo(5.23f, 7.22f, 5.5f, 7.38f)
            quadTo(5.78f, 7.52f, 5.85f, 7.82f)
            reflectiveQuadTo(5.78f, 8.4f)
            lineTo(5.3f, 9.2f)
            horizontalLineTo(6.25f)
            quadToRelative(0.33f, 0f, 0.54f, 0.21f)
            reflectiveQuadTo(7f, 9.95f)
            reflectiveQuadTo(6.79f, 10.49f)
            reflectiveQuadTo(6.25f, 10.7f)
            horizontalLineTo(5.3f)
            lineToRelative(0.47f, 0.85f)
            quadToRelative(0.15f, 0.28f, 0.08f, 0.58f)
            reflectiveQuadTo(5.5f, 12.58f)
            reflectiveQuadTo(4.93f, 12.65f)
            quadTo(4.63f, 12.58f, 4.48f, 12.3f)
            lineTo(4f, 11.45f)
            close()
            moveToRelative(8f, 0f)
            lineTo(11.53f, 12.3f)
            quadToRelative(-0.15f, 0.28f, -0.45f, 0.35f)
            quadToRelative(-0.3f, 0.08f, -0.58f, -0.07f)
            reflectiveQuadTo(10.15f, 12.13f)
            quadToRelative(-0.07f, -0.3f, 0.08f, -0.58f)
            lineTo(10.7f, 10.7f)
            horizontalLineTo(9.75f)
            quadToRelative(-0.32f, 0f, -0.54f, -0.21f)
            reflectiveQuadTo(9f, 9.95f)
            reflectiveQuadTo(9.21f, 9.41f)
            reflectiveQuadTo(9.75f, 9.2f)
            horizontalLineTo(10.7f)
            lineTo(10.23f, 8.4f)
            quadTo(10.08f, 8.13f, 10.15f, 7.82f)
            reflectiveQuadTo(10.5f, 7.38f)
            quadTo(10.78f, 7.22f, 11.08f, 7.3f)
            reflectiveQuadToRelative(0.45f, 0.35f)
            lineTo(12f, 8.45f)
            lineToRelative(0.48f, -0.8f)
            quadTo(12.63f, 7.38f, 12.93f, 7.3f)
            quadToRelative(0.3f, -0.08f, 0.57f, 0.07f)
            quadToRelative(0.28f, 0.15f, 0.35f, 0.45f)
            reflectiveQuadTo(13.78f, 8.4f)
            lineTo(13.3f, 9.2f)
            horizontalLineToRelative(0.95f)
            quadToRelative(0.33f, 0f, 0.54f, 0.21f)
            reflectiveQuadTo(15f, 9.95f)
            reflectiveQuadToRelative(-0.21f, 0.54f)
            reflectiveQuadTo(14.25f, 10.7f)
            horizontalLineTo(13.3f)
            lineToRelative(0.47f, 0.85f)
            quadToRelative(0.15f, 0.28f, 0.08f, 0.58f)
            reflectiveQuadTo(13.5f, 12.58f)
            reflectiveQuadToRelative(-0.57f, 0.07f)
            quadTo(12.63f, 12.58f, 12.48f, 12.3f)
            lineTo(12f, 11.45f)
            close()
            moveToRelative(8f, 0f)
            lineTo(19.53f, 12.3f)
            quadToRelative(-0.15f, 0.28f, -0.45f, 0.35f)
            quadToRelative(-0.3f, 0.08f, -0.57f, -0.07f)
            quadTo(18.23f, 12.43f, 18.15f, 12.13f)
            reflectiveQuadToRelative(0.07f, -0.58f)
            lineTo(18.7f, 10.7f)
            horizontalLineTo(17.75f)
            quadToRelative(-0.32f, 0f, -0.54f, -0.21f)
            reflectiveQuadTo(17f, 9.95f)
            reflectiveQuadTo(17.21f, 9.41f)
            reflectiveQuadTo(17.75f, 9.2f)
            horizontalLineTo(18.7f)
            lineTo(18.23f, 8.4f)
            quadTo(18.08f, 8.13f, 18.15f, 7.82f)
            reflectiveQuadTo(18.5f, 7.38f)
            quadTo(18.78f, 7.22f, 19.08f, 7.3f)
            quadToRelative(0.3f, 0.07f, 0.45f, 0.35f)
            lineTo(20f, 8.45f)
            lineToRelative(0.48f, -0.8f)
            quadTo(20.63f, 7.38f, 20.93f, 7.3f)
            quadToRelative(0.3f, -0.08f, 0.57f, 0.07f)
            quadToRelative(0.28f, 0.15f, 0.35f, 0.45f)
            reflectiveQuadTo(21.78f, 8.4f)
            lineTo(21.3f, 9.2f)
            horizontalLineToRelative(0.95f)
            quadToRelative(0.33f, 0f, 0.54f, 0.21f)
            reflectiveQuadTo(23f, 9.95f)
            reflectiveQuadToRelative(-0.21f, 0.54f)
            reflectiveQuadTo(22.25f, 10.7f)
            horizontalLineTo(21.3f)
            lineToRelative(0.48f, 0.85f)
            quadToRelative(0.15f, 0.28f, 0.07f, 0.58f)
            reflectiveQuadTo(21.5f, 12.58f)
            reflectiveQuadToRelative(-0.57f, 0.07f)
            quadTo(20.63f, 12.58f, 20.48f, 12.3f)
            lineTo(20f, 11.45f)
            close()
          }
        }
        .buildSimpIcon()
    return _Password!!
  }

private var _Password: ImageVector? = null
