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
val SimpIcons.SportsEsports: ImageVector
  get() {
    if (_SportsEsports != null) {
      return _SportsEsports!!
    }
    _SportsEsports =
      ImageVector.Builder(
          name = "SportsEsports",
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
            moveTo(4.55f, 19f)
            quadTo(3.28f, 19f, 2.58f, 18.11f)
            reflectiveQuadTo(2.05f, 15.95f)
            lineTo(3.1f, 8.45f)
            quadTo(3.33f, 6.95f, 4.44f, 5.97f)
            reflectiveQuadTo(7.05f, 5f)
            horizontalLineToRelative(9.9f)
            quadToRelative(1.5f, 0f, 2.61f, 0.97f)
            reflectiveQuadTo(20.9f, 8.45f)
            lineToRelative(1.05f, 7.5f)
            quadToRelative(0.17f, 1.28f, -0.53f, 2.16f)
            reflectiveQuadTo(19.45f, 19f)
            quadToRelative(-0.53f, 0f, -0.98f, -0.19f)
            reflectiveQuadTo(17.65f, 18.25f)
            lineTo(15.4f, 16f)
            horizontalLineTo(8.6f)
            lineTo(6.35f, 18.25f)
            quadTo(5.98f, 18.63f, 5.53f, 18.81f)
            reflectiveQuadTo(4.55f, 19f)
            close()
            moveTo(17.71f, 12.71f)
            quadTo(18f, 12.43f, 18f, 12f)
            reflectiveQuadTo(17.71f, 11.29f)
            reflectiveQuadTo(17f, 11f)
            reflectiveQuadToRelative(-0.71f, 0.29f)
            reflectiveQuadTo(16f, 12f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            reflectiveQuadTo(17f, 13f)
            reflectiveQuadToRelative(0.71f, -0.29f)
            close()
            moveToRelative(-2f, -3f)
            quadTo(16f, 9.42f, 16f, 9f)
            quadTo(16f, 8.57f, 15.71f, 8.29f)
            reflectiveQuadTo(15f, 8f)
            reflectiveQuadTo(14.29f, 8.29f)
            reflectiveQuadTo(14f, 9f)
            quadToRelative(0f, 0.42f, 0.29f, 0.71f)
            reflectiveQuadTo(15f, 10f)
            reflectiveQuadTo(15.71f, 9.71f)
            close()
            moveTo(7.75f, 11.25f)
            verticalLineToRelative(1f)
            quadToRelative(0f, 0.32f, 0.21f, 0.54f)
            reflectiveQuadTo(8.5f, 13f)
            quadToRelative(0.33f, 0f, 0.54f, -0.21f)
            reflectiveQuadTo(9.25f, 12.25f)
            verticalLineToRelative(-1f)
            horizontalLineToRelative(1f)
            quadToRelative(0.33f, 0f, 0.54f, -0.21f)
            reflectiveQuadTo(11f, 10.5f)
            quadTo(11f, 10.17f, 10.79f, 9.96f)
            reflectiveQuadTo(10.25f, 9.75f)
            horizontalLineToRelative(-1f)
            verticalLineToRelative(-1f)
            quadTo(9.25f, 8.42f, 9.04f, 8.21f)
            quadTo(8.83f, 8f, 8.5f, 8f)
            quadTo(8.18f, 8f, 7.96f, 8.21f)
            reflectiveQuadTo(7.75f, 8.75f)
            verticalLineToRelative(1f)
            horizontalLineToRelative(-1f)
            quadToRelative(-0.32f, 0f, -0.54f, 0.21f)
            reflectiveQuadTo(6f, 10.5f)
            reflectiveQuadToRelative(0.21f, 0.54f)
            reflectiveQuadToRelative(0.54f, 0.21f)
            horizontalLineToRelative(1f)
            close()
          }
        }
        .buildSimpIcon()
    return _SportsEsports!!
  }

private var _SportsEsports: ImageVector? = null
