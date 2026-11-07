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
val SimpIcons.RecordVoiceOver: ImageVector
  get() {
    if (_RecordVoiceOver != null) {
      return _RecordVoiceOver!!
    }
    _RecordVoiceOver =
      ImageVector.Builder(
          name = "RecordVoiceOver",
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
            moveTo(23f, 9f)
            quadToRelative(0f, 1.72f, -0.61f, 3.29f)
            reflectiveQuadToRelative(-1.66f, 2.84f)
            quadToRelative(-0.3f, 0.35f, -0.75f, 0.38f)
            reflectiveQuadToRelative(-0.8f, -0.33f)
            quadTo(18.85f, 14.85f, 18.88f, 14.4f)
            reflectiveQuadToRelative(0.3f, -0.82f)
            quadToRelative(0.75f, -0.95f, 1.16f, -2.13f)
            reflectiveQuadTo(20.75f, 9f)
            quadToRelative(0f, -1.28f, -0.41f, -2.43f)
            reflectiveQuadTo(19.18f, 4.47f)
            quadTo(18.88f, 4.1f, 18.86f, 3.65f)
            reflectiveQuadToRelative(0.31f, -0.8f)
            reflectiveQuadTo(19.96f, 2.51f)
            reflectiveQuadToRelative(0.76f, 0.36f)
            quadToRelative(1.05f, 1.27f, 1.66f, 2.84f)
            reflectiveQuadTo(23f, 9f)
            close()
            moveTo(18.45f, 9f)
            quadToRelative(0f, 0.8f, -0.25f, 1.54f)
            quadToRelative(-0.25f, 0.74f, -0.7f, 1.36f)
            quadToRelative(-0.27f, 0.38f, -0.74f, 0.39f)
            reflectiveQuadTo(15.95f, 11.95f)
            quadTo(15.63f, 11.63f, 15.61f, 11.16f)
            reflectiveQuadToRelative(0.21f, -0.89f)
            quadTo(15.98f, 10f, 16.06f, 9.67f)
            reflectiveQuadTo(16.15f, 9f)
            reflectiveQuadTo(16.06f, 8.32f)
            quadTo(15.98f, 8f, 15.83f, 7.7f)
            quadTo(15.6f, 7.27f, 15.61f, 6.82f)
            reflectiveQuadTo(15.95f, 6.05f)
            quadTo(16.3f, 5.7f, 16.76f, 5.71f)
            quadTo(17.23f, 5.72f, 17.5f, 6.1f)
            quadToRelative(0.45f, 0.63f, 0.7f, 1.36f)
            reflectiveQuadTo(18.45f, 9f)
            close()
            moveTo(9f, 13f)
            quadTo(7.35f, 13f, 6.18f, 11.83f)
            reflectiveQuadTo(5f, 9f)
            reflectiveQuadTo(6.18f, 6.18f)
            reflectiveQuadTo(9f, 5f)
            reflectiveQuadToRelative(2.83f, 1.18f)
            reflectiveQuadTo(13f, 9f)
            reflectiveQuadToRelative(-1.17f, 2.82f)
            reflectiveQuadTo(9f, 13f)
            close()
            moveTo(1f, 19f)
            verticalLineTo(18.2f)
            quadTo(1f, 17.38f, 1.43f, 16.65f)
            quadTo(1.85f, 15.93f, 2.6f, 15.55f)
            quadTo(3.88f, 14.9f, 5.48f, 14.45f)
            reflectiveQuadTo(9f, 14f)
            reflectiveQuadToRelative(3.53f, 0.45f)
            reflectiveQuadToRelative(2.88f, 1.1f)
            quadToRelative(0.75f, 0.38f, 1.17f, 1.1f)
            reflectiveQuadTo(17f, 18.2f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(15f, 21f)
            horizontalLineTo(3f)
            quadTo(2.18f, 21f, 1.59f, 20.41f)
            reflectiveQuadTo(1f, 19f)
            close()
          }
        }
        .buildSimpIcon()
    return _RecordVoiceOver!!
  }

private var _RecordVoiceOver: ImageVector? = null
