package com.suvojeet.suvmusic.ui.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Official Social Media Icons as ImageVectors
 */
object SocialIcons {
    val GitHub: ImageVector
        get() = ImageVector.Builder(
            name = "GitHub",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.EvenOdd
        ) {
            moveTo(12f, 2f)
            curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
            curveTo(2f, 16.417f, 4.867f, 20.162f, 8.832f, 21.482f)
            curveTo(9.332f, 21.574f, 9.515f, 21.265f, 9.515f, 21.001f)
            curveTo(9.515f, 20.763f, 9.506f, 20.133f, 9.501f, 19.299f)
            curveTo(6.723f, 19.902f, 6.136f, 17.962f, 6.136f, 17.962f)
            curveTo(5.683f, 16.811f, 5.03f, 16.505f, 5.03f, 16.505f)
            curveTo(4.124f, 15.886f, 5.099f, 15.898f, 5.099f, 15.898f)
            curveTo(6.101f, 15.968f, 6.628f, 16.927f, 6.628f, 16.927f)
            curveTo(7.517f, 18.449f, 8.961f, 18.01f, 9.531f, 17.754f)
            curveTo(9.621f, 17.11f, 9.879f, 16.671f, 10.163f, 16.423f)
            curveTo(7.945f, 16.171f, 5.613f, 15.313f, 5.613f, 11.483f)
            curveTo(5.613f, 10.393f, 6.002f, 9.502f, 6.64f, 8.804f)
            curveTo(6.537f, 8.551f, 6.195f, 7.534f, 6.738f, 6.163f)
            curveTo(6.738f, 6.163f, 7.576f, 5.894f, 9.482f, 7.185f)
            curveTo(10.278f, 6.963f, 11.131f, 6.853f, 11.979f, 6.849f)
            curveTo(12.827f, 6.853f, 13.679f, 6.963f, 14.477f, 7.185f)
            curveTo(16.381f, 5.894f, 17.217f, 6.163f, 17.217f, 6.163f)
            curveTo(17.762f, 7.534f, 17.419f, 8.551f, 17.317f, 8.804f)
            curveTo(17.957f, 9.502f, 18.344f, 10.393f, 18.344f, 11.483f)
            curveTo(18.344f, 15.323f, 16.009f, 16.168f, 13.784f, 16.415f)
            curveTo(14.143f, 16.723f, 14.463f, 17.335f, 14.463f, 18.27f)
            curveTo(14.463f, 19.61f, 14.451f, 20.693f, 14.451f, 21.001f)
            curveTo(14.451f, 21.268f, 14.631f, 21.579f, 15.138f, 21.481f)
            curveTo(19.101f, 20.158f, 21.966f, 16.415f, 21.966f, 12f)
            curveTo(21.966f, 6.477f, 17.523f, 2f, 12f, 2f)
            close()
        }.build()

    val Instagram: ImageVector
        get() = ImageVector.Builder(
            name = "Instagram",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.White)
        ) {
            moveTo(17f, 2f)
            lineTo(7f, 2f)
            curveTo(4.239f, 2f, 2f, 4.239f, 2f, 7f)
            lineTo(2f, 17f)
            curveTo(2f, 19.761f, 4.239f, 22f, 7f, 22f)
            lineTo(17f, 22f)
            curveTo(19.761f, 22f, 22f, 19.761f, 22f, 17f)
            lineTo(22f, 7f)
            curveTo(22f, 4.239f, 19.761f, 2f, 17f, 2f)
            close()
            moveTo(12f, 7.5f)
            curveTo(14.485f, 7.5f, 16.5f, 9.515f, 16.5f, 12f)
            curveTo(16.5f, 14.485f, 14.485f, 16.5f, 12f, 16.5f)
            curveTo(9.515f, 16.5f, 7.5f, 14.485f, 7.5f, 12f)
            curveTo(7.5f, 9.515f, 9.515f, 7.5f, 12f, 7.5f)
            close()
            moveTo(17.5f, 6f)
            curveTo(17.776f, 6f, 18f, 6.224f, 18f, 6.5f)
            curveTo(18f, 6.776f, 17.776f, 7f, 17.5f, 7f)
            curveTo(17.224f, 7f, 17f, 6.776f, 17f, 6.5f)
            curveTo(17f, 6.224f, 17.224f, 6f, 17.5f, 6f)
            close()
        }.build()

    val Telegram: ImageVector
        get() = ImageVector.Builder(
            name = "Telegram",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.White)
        ) {
            moveTo(20.665f, 3.717f)
            lineTo(2.937f, 10.551f)
            curveTo(1.727f, 11.037f, 1.733f, 11.711f, 2.713f, 12.012f)
            lineTo(7.265f, 13.433f)
            lineTo(17.797f, 6.791f)
            curveTo(18.295f, 6.488f, 18.751f, 6.651f, 18.377f, 6.983f)
            lineTo(9.849f, 14.68f)
            lineTo(9.521f, 19.562f)
            curveTo(10f, 19.562f, 10.211f, 19.342f, 10.457f, 19.105f)
            lineTo(12.755f, 16.871f)
            lineTo(17.531f, 20.397f)
            curveTo(18.411f, 20.881f, 19.043f, 20.631f, 19.263f, 19.581f)
            lineTo(22.396f, 4.832f)
            curveTo(22.716f, 3.548f, 21.905f, 2.966f, 21.066f, 3.328f)
            lineTo(20.665f, 3.717f)
            close()
        }.build()

    val BuyMeACoffee: ImageVector
        get() = ImageVector.Builder(
            name = "BuyMeACoffee",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.White)
        ) {
            moveTo(20.216f, 4.771f)
            curveTo(20.088f, 4.417f, 19.782f, 4.144f, 19.394f, 4.041f)
            curveTo(18.239f, 3.744f, 16.634f, 3.578f, 15.011f, 3.518f)
            curveTo(15.006f, 3.518f, 15.001f, 3.518f, 14.996f, 3.518f)
            horizontalLineTo(3.262f)
            curveTo(2.456f, 3.518f, 1.802f, 4.172f, 1.802f, 4.978f)
            verticalLineTo(13.623f)
            curveTo(1.802f, 15.894f, 3.577f, 17.771f, 5.828f, 17.892f)
            curveTo(6.346f, 17.919f, 6.864f, 17.933f, 7.382f, 17.933f)
            curveTo(9.08f, 17.933f, 10.741f, 17.768f, 12.308f, 17.476f)
            curveTo(14.283f, 17.108f, 15.808f, 15.392f, 15.808f, 13.382f)
            verticalLineTo(6.126f)
            curveTo(17.39f, 6.183f, 18.913f, 6.355f, 20.046f, 6.632f)
            curveTo(21.02f, 6.87f, 21.611f, 7.747f, 21.611f, 8.752f)
            curveTo(21.611f, 9.757f, 21.02f, 10.634f, 20.046f, 10.872f)
            curveTo(19.851f, 10.92f, 19.673f, 10.963f, 19.51f, 11.001f)
            verticalLineTo(12.784f)
            curveTo(19.697f, 12.751f, 19.897f, 12.71f, 20.108f, 12.659f)
            curveTo(21.848f, 12.234f, 23.081f, 10.672f, 23.081f, 8.752f)
            curveTo(23.081f, 6.832f, 21.848f, 5.27f, 20.216f, 4.771f)
            close()
            moveTo(14.338f, 13.382f)
            curveTo(14.338f, 14.659f, 13.361f, 15.748f, 12.115f, 15.981f)
            curveTo(10.638f, 16.257f, 9.068f, 16.413f, 7.382f, 16.413f)
            curveTo(6.945f, 16.413f, 6.508f, 16.402f, 6.071f, 16.379f)
            curveTo(4.568f, 16.298f, 3.322f, 15.083f, 3.322f, 13.623f)
            verticalLineTo(5.038f)
            horizontalLineTo(14.338f)
            verticalLineTo(13.382f)
            close()
            moveTo(4.453f, 18.441f)
            horizontalLineTo(13.254f)
            verticalLineTo(19.921f)
            curveTo(13.254f, 20.727f, 12.6f, 21.381f, 11.794f, 21.381f)
            horizontalLineTo(5.913f)
            curveTo(5.107f, 21.381f, 4.453f, 20.727f, 4.453f, 19.921f)
            verticalLineTo(18.441f)
            close()
        }.build()

    val UPI: ImageVector
        get() = ImageVector.Builder(
            name = "UPI",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.White)
        ) {
            moveTo(12f, 2f)
            lineTo(22f, 12f)
            lineTo(12f, 22f)
            lineTo(2f, 12f)
            close()
            moveTo(11f, 8f)
            horizontalLineTo(13f)
            verticalLineTo(16f)
            horizontalLineTo(11f)
            close()
            moveTo(8f, 11f)
            horizontalLineTo(16f)
            verticalLineTo(13f)
            horizontalLineTo(8f)
            close()
        }.build()
}
