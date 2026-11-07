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
val SimpIcons.Abc: ImageVector
  get() {
    if (_Abc != null) {
      return _Abc!!
    }
    _Abc =
      ImageVector.Builder(
          name = "Abc",
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
            moveTo(10f, 15f)
            quadTo(9.8f, 15f, 9.65f, 14.85f)
            reflectiveQuadTo(9.5f, 14.5f)
            verticalLineToRelative(-5f)
            quadTo(9.5f, 9.3f, 9.65f, 9.15f)
            reflectiveQuadTo(10f, 9f)
            horizontalLineToRelative(3.5f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(14.5f, 10f)
            verticalLineToRelative(1f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(13.5f, 12f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(14.5f, 13f)
            verticalLineToRelative(1f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(13.5f, 15f)
            horizontalLineTo(10f)
            close()
            moveToRelative(1f, -3.75f)
            horizontalLineToRelative(2f)
            verticalLineTo(10.5f)
            horizontalLineTo(11f)
            verticalLineToRelative(0.75f)
            close()
            moveToRelative(0f, 2.25f)
            horizontalLineToRelative(2f)
            verticalLineTo(12.75f)
            horizontalLineTo(11f)
            verticalLineTo(13.5f)
            close()
            moveTo(3.75f, 15f)
            quadTo(3.43f, 15f, 3.21f, 14.79f)
            reflectiveQuadTo(3f, 14.25f)
            verticalLineTo(10f)
            quadTo(3f, 9.57f, 3.29f, 9.29f)
            reflectiveQuadTo(4f, 9f)
            horizontalLineTo(7f)
            quadTo(7.43f, 9f, 7.71f, 9.29f)
            reflectiveQuadTo(8f, 10f)
            verticalLineToRelative(4.25f)
            quadToRelative(0f, 0.32f, -0.21f, 0.54f)
            reflectiveQuadTo(7.25f, 15f)
            reflectiveQuadTo(6.71f, 14.79f)
            reflectiveQuadTo(6.5f, 14.25f)
            verticalLineTo(13.5f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(0.75f)
            quadToRelative(0f, 0.32f, -0.21f, 0.54f)
            reflectiveQuadTo(3.75f, 15f)
            close()
            moveTo(4.5f, 12f)
            horizontalLineToRelative(2f)
            verticalLineTo(10.5f)
            horizontalLineToRelative(-2f)
            verticalLineTo(12f)
            close()
            moveTo(17f, 15f)
            quadToRelative(-0.43f, 0f, -0.71f, -0.29f)
            reflectiveQuadTo(16f, 14f)
            verticalLineTo(10f)
            quadTo(16f, 9.57f, 16.29f, 9.29f)
            reflectiveQuadTo(17f, 9f)
            horizontalLineToRelative(3f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 10f)
            verticalLineToRelative(0.5f)
            quadToRelative(0f, 0.32f, -0.21f, 0.54f)
            reflectiveQuadToRelative(-0.54f, 0.21f)
            quadToRelative(-0.32f, 0f, -0.54f, -0.21f)
            reflectiveQuadTo(19.5f, 10.5f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(3f)
            horizontalLineToRelative(2f)
            quadToRelative(0f, -0.33f, 0.21f, -0.54f)
            reflectiveQuadToRelative(0.54f, -0.21f)
            reflectiveQuadToRelative(0.54f, 0.21f)
            quadTo(21f, 13.18f, 21f, 13.5f)
            verticalLineTo(14f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(20f, 15f)
            horizontalLineTo(17f)
            close()
          }
        }
        .buildSimpIcon()
    return _Abc!!
  }

private var _Abc: ImageVector? = null
