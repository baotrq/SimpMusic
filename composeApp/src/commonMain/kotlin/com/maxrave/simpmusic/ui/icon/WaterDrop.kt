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
val SimpIcons.WaterDrop: ImageVector
  get() {
    if (_WaterDrop != null) {
      return _WaterDrop!!
    }
    _WaterDrop =
      ImageVector.Builder(
          name = "WaterDrop",
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
            moveTo(6.29f, 19.65f)
            quadTo(4f, 17.3f, 4f, 13.8f)
            quadTo(4f, 12.25f, 4.7f, 10.7f)
            reflectiveQuadTo(6.45f, 7.72f)
            reflectiveQuadTo(8.73f, 5.05f)
            reflectiveQuadTo(11f, 2.88f)
            quadToRelative(0.2f, -0.2f, 0.46f, -0.29f)
            reflectiveQuadTo(12f, 2.5f)
            quadToRelative(0.28f, 0f, 0.54f, 0.09f)
            reflectiveQuadTo(13f, 2.88f)
            quadToRelative(1.05f, 0.92f, 2.28f, 2.18f)
            reflectiveQuadToRelative(2.28f, 2.67f)
            reflectiveQuadTo(19.3f, 10.7f)
            reflectiveQuadTo(20f, 13.8f)
            quadToRelative(0f, 3.5f, -2.29f, 5.85f)
            reflectiveQuadTo(12f, 22f)
            quadTo(8.58f, 22f, 6.29f, 19.65f)
            close()
            moveTo(12.28f, 19f)
            quadToRelative(0.3f, -0.02f, 0.51f, -0.24f)
            reflectiveQuadTo(13f, 18.25f)
            quadTo(13f, 17.9f, 12.78f, 17.69f)
            reflectiveQuadTo(12.2f, 17.5f)
            quadToRelative(-1.03f, 0.07f, -2.18f, -0.56f)
            reflectiveQuadTo(8.58f, 14.63f)
            quadTo(8.53f, 14.35f, 8.31f, 14.18f)
            reflectiveQuadTo(7.83f, 14f)
            quadTo(7.48f, 14f, 7.25f, 14.26f)
            quadTo(7.03f, 14.53f, 7.1f, 14.88f)
            quadToRelative(0.42f, 2.27f, 2f, 3.25f)
            reflectiveQuadTo(12.28f, 19f)
            close()
          }
        }
        .buildSimpIcon()
    return _WaterDrop!!
  }

private var _WaterDrop: ImageVector? = null
