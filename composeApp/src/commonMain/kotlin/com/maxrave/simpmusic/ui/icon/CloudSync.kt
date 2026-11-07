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
val SimpIcons.CloudSync: ImageVector
  get() {
    if (_CloudSync != null) {
      return _CloudSync!!
    }
    _CloudSync =
      ImageVector.Builder(
          name = "CloudSync",
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
            moveTo(6f, 12f)
            quadToRelative(0f, 1.35f, 0.54f, 2.49f)
            quadTo(7.08f, 15.63f, 8f, 16.45f)
            verticalLineTo(15f)
            quadTo(8f, 14.58f, 8.29f, 14.29f)
            quadTo(8.58f, 14f, 9f, 14f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(10f, 15f)
            verticalLineToRelative(4f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(9f, 20f)
            horizontalLineTo(5f)
            quadTo(4.58f, 20f, 4.29f, 19.71f)
            quadTo(4f, 19.43f, 4f, 19f)
            reflectiveQuadTo(4.29f, 18.29f)
            reflectiveQuadTo(5f, 18f)
            horizontalLineTo(6.73f)
            quadToRelative(-1.28f, -1.1f, -2f, -2.65f)
            reflectiveQuadTo(4f, 12f)
            quadTo(4f, 9.63f, 5.24f, 7.72f)
            reflectiveQuadTo(8.45f, 4.82f)
            quadTo(8.8f, 4.63f, 9.18f, 4.81f)
            reflectiveQuadTo(9.68f, 5.4f)
            reflectiveQuadTo(9.64f, 6.18f)
            reflectiveQuadTo(9.13f, 6.75f)
            quadTo(7.73f, 7.52f, 6.86f, 8.91f)
            reflectiveQuadTo(6f, 12f)
            close()
            moveToRelative(9f, 8f)
            quadToRelative(-1.25f, 0f, -2.13f, -0.88f)
            reflectiveQuadTo(12f, 17f)
            quadToRelative(0f, -1.2f, 0.83f, -2.06f)
            reflectiveQuadToRelative(2.03f, -0.91f)
            quadToRelative(0.42f, -0.9f, 1.26f, -1.46f)
            reflectiveQuadTo(18f, 12f)
            quadToRelative(1.33f, 0f, 2.29f, 0.86f)
            quadToRelative(0.96f, 0.86f, 1.16f, 2.14f)
            quadToRelative(1.05f, 0f, 1.8f, 0.72f)
            reflectiveQuadTo(24f, 17.48f)
            quadToRelative(0f, 1.05f, -0.72f, 1.79f)
            reflectiveQuadTo(21.5f, 20f)
            horizontalLineTo(15f)
            close()
            moveTo(16f, 7.55f)
            verticalLineTo(9f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(15f, 10f)
            reflectiveQuadTo(14.29f, 9.71f)
            reflectiveQuadTo(14f, 9f)
            verticalLineTo(5f)
            quadTo(14f, 4.57f, 14.29f, 4.29f)
            reflectiveQuadTo(15f, 4f)
            horizontalLineToRelative(4f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(20f, 5f)
            reflectiveQuadTo(19.71f, 5.71f)
            reflectiveQuadTo(19f, 6f)
            horizontalLineTo(17.28f)
            quadToRelative(0.82f, 0.72f, 1.44f, 1.66f)
            reflectiveQuadTo(19.65f, 9.7f)
            quadToRelative(0.13f, 0.4f, -0.1f, 0.72f)
            reflectiveQuadToRelative(-0.65f, 0.4f)
            reflectiveQuadTo(18.14f, 10.66f)
            reflectiveQuadTo(17.65f, 10.02f)
            quadTo(17.4f, 9.3f, 16.98f, 8.67f)
            reflectiveQuadTo(16f, 7.55f)
            close()
          }
        }
        .buildSimpIcon()
    return _CloudSync!!
  }

private var _CloudSync: ImageVector? = null
