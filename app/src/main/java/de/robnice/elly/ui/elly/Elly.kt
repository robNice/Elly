package de.robnice.elly.ui.elly

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.robnice.elly.R

@Composable
fun EllyMascot(size: Dp, modifier: Modifier = Modifier, pose: EllyPose = EllyPoses.Remote) {
    val density = LocalDensity.current
    val bob = with(density) { dimensionResource(R.dimen.onboarding_mascot_bob).toPx() }
    val drift = with(density) { size.toPx() } * DRIFT_FRACTION
    val transition = rememberInfiniteTransition(label = "mascot")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(LOOP_MS, easing = LinearEasing)),
        label = "time"
    )
    val frames = pose.frameCount.coerceAtLeast(1)
    val frameTime by transition.animateFloat(
        initialValue = 0f,
        targetValue = frames.toFloat(),
        animationSpec = infiniteRepeatable(tween(frames * FRAME_MS, easing = LinearEasing)),
        label = "frame"
    )
    val painters = pose.layers.map { painterResource(it.image) }
    val ratio = painters.first().intrinsicSize.let { it.width / it.height }
    val description = stringResource(R.string.onboarding_mascot)
    Box(
        modifier = modifier
            .height(size)
            .aspectRatio(ratio)
            .semantics { contentDescription = description }
    ) {
        pose.layers.forEachIndexed { index, layer ->
            Image(
                painter = painters[index],
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        applyMotion(layer, time, frameTime % 1f, bob, drift)
                        if (layer.frame >= 0) alpha = if (frameTime.toInt() % frames == layer.frame) 1f else 0f
                    }
            )
        }
    }
}

@Composable
fun EllySpeechBubble(text: String, tailSide: TailSide, modifier: Modifier = Modifier, title: String? = null) {
    val tail = dimensionResource(R.dimen.onboarding_bubble_tail)
    val shape = SpeechBubbleShape(
        cornerRadius = dimensionResource(R.dimen.onboarding_bubble_corner),
        tailSize = tail,
        tailSide = tailSide
    )
    val centered = tailSide == TailSide.TOP
    val textAlign = if (centered) TextAlign.Center else TextAlign.Start
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .background(colorResource(R.color.onboarding_bubble), shape)
            .padding(top = if (centered) tail else 0.dp, start = if (centered) 0.dp else tail)
            .padding(dimensionResource(R.dimen.onboarding_bubble_padding)),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = EllyFont, fontWeight = FontWeight.Medium),
                color = colorResource(R.color.onboarding_title),
                textAlign = textAlign
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.onboarding_bubble_spacing)))
        }
        Text(
            text = text,
            style = (if (centered) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium)
                .copy(fontFamily = EllyFont),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = textAlign
        )
    }
}

// Fades the content out at the bottom while more of it can be scrolled into view.
fun Modifier.fadeBottomEdge(canScrollForward: () -> Boolean, height: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        if (canScrollForward()) {
            val fade = height.toPx()
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Black, Color.Transparent), startY = size.height - fade, endY = size.height),
                topLeft = Offset(0f, size.height - fade),
                size = Size(size.width, fade),
                blendMode = BlendMode.DstIn
            )
        }
    }

private val EllyFont = FontFamily(
    Font(R.font.fredoka_regular, FontWeight.Normal),
    Font(R.font.fredoka_medium, FontWeight.Medium)
)

private const val DRIFT_FRACTION = 0.02f
