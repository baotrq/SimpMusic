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
val SimpIcons.PowerSettingsNew: ImageVector
  get() {
    if (_PowerSettingsNew != null) {
      return _PowerSettingsNew!!
    }
    _PowerSettingsNew =
      ImageVector.Builder(
          name = "PowerSettingsNew",
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
            moveTo(12f, 22f)
            quadTo(9.93f, 22f, 8.1f, 21.21f)
            quadTo(6.28f, 20.43f, 4.93f, 19.08f)
            quadTo(3.58f, 17.73f, 2.79f, 15.9f)
            reflectiveQuadTo(2f, 12f)
            quadTo(2f, 10.17f, 2.61f, 8.54f)
            reflectiveQuadTo(4.3f, 5.6f)
            quadTo(4.58f, 5.25f, 4.96f, 5.27f)
            reflectiveQuadTo(5.63f, 5.55f)
            reflectiveQuadTo(5.99f, 6.2f)
            reflectiveQuadTo(5.73f, 7.02f)
            quadTo(4.93f, 8.05f, 4.46f, 9.31f)
            reflectiveQuadTo(4f, 12f)
            quadToRelative(0f, 3.35f, 2.33f, 5.68f)
            reflectiveQuadTo(12f, 20f)
            reflectiveQuadToRelative(5.68f, -2.32f)
            reflectiveQuadTo(20f, 12f)
            quadTo(20f, 10.58f, 19.54f, 9.31f)
            reflectiveQuadTo(18.28f, 7.02f)
            quadTo(17.93f, 6.6f, 18.01f, 6.2f)
            reflectiveQuadTo(18.38f, 5.55f)
            reflectiveQuadTo(19.04f, 5.27f)
            reflectiveQuadTo(19.7f, 5.6f)
            quadToRelative(1.07f, 1.3f, 1.69f, 2.94f)
            quadTo(22f, 10.17f, 22f, 12f)
            quadToRelative(0f, 2.07f, -0.79f, 3.9f)
            reflectiveQuadToRelative(-2.14f, 3.17f)
            quadToRelative(-1.35f, 1.35f, -3.17f, 2.14f)
            reflectiveQuadTo(12f, 22f)
            close()
            moveTo(11.29f, 12.71f)
            quadTo(11f, 12.43f, 11f, 12f)
            verticalLineTo(3f)
            quadTo(11f, 2.57f, 11.29f, 2.29f)
            reflectiveQuadTo(12f, 2f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(13f, 3f)
            verticalLineToRelative(9f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(12f, 13f)
            reflectiveQuadTo(11.29f, 12.71f)
            close()
          }
        }
        .buildSimpIcon()
    return _PowerSettingsNew!!
  }

private var _PowerSettingsNew: ImageVector? = null
