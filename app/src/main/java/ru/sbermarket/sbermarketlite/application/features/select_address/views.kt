package ru.sbermarket.sbermarketlite.application.features.select_address

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// TODO: define themes in native and expose it to RN https://instamart.atlassian.net/browse/MOBA-885
val Green = Color(0xFF21A038)
val Yellow = Color(0xFFFECB00)

@Composable
fun Pin(
    modifier: Modifier = Modifier,
    outerDiameter: Dp = 46.dp,
    innerDiameterDefault: Dp = 14.dp,
    innerDiameterExpanded: Dp = 24.dp,
    pointDiameter: Dp = 7.dp,
    lineWidth: Dp = 4.dp,
    lineHeight: Dp = 15.dp,
    animated: Boolean = false
) {
    val innerRadiusDefault = innerDiameterDefault / 2
    val innerRadiusExpanded = innerDiameterExpanded / 2
    val innerRadiusDiff = innerRadiusExpanded - innerRadiusDefault
    val outerRadius = outerDiameter / 2
    val pointRadius = pointDiameter / 2


    val infiniteTransition = rememberInfiniteTransition()
    var isRunning by remember {
        mutableStateOf(animated)
    }
    val radiusCoef = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isRunning) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val radiusIsZero = radiusCoef.value == 0f

    LaunchedEffect(animated, isRunning, radiusIsZero) {
        if (!animated && isRunning && radiusIsZero) {
            isRunning = false
        }
        if (animated && !isRunning && radiusIsZero) {
            isRunning = true
        }
    }

    val height = outerDiameter + lineHeight + pointRadius

    Canvas(modifier = modifier
        .width(outerDiameter)
        .height(height)
        .background(Color.Transparent)) {
        val centerOffset = Offset(
            x = outerRadius.toPx(),
            y = outerRadius.toPx()
        )
        drawCircle(
            color = Color.Black,
            center = Offset(
                x = outerRadius.toPx(),
                y = (outerDiameter + lineHeight).toPx()
            ),
            radius = pointRadius.toPx()
        )
        drawLine(
            color = Green,
            start = Offset(
                x = outerRadius.toPx(),
                y = outerDiameter.toPx()
            ),
            end = Offset(
                x = outerRadius.toPx(),
                y = outerDiameter.toPx() + lineHeight.toPx()
            ),
            strokeWidth = lineWidth.toPx(),
            cap = StrokeCap.Round

        )
        drawCircle(
            color = Yellow,
            center = centerOffset,
            radius = outerRadius.toPx()
        )
        drawCircle(
            color = Color.White,
            center = centerOffset,
            radius = innerRadiusDefault.toPx() + innerRadiusDiff.toPx() * radiusCoef.value
        )

    }
}