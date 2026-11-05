package de.robnice.elly.wear.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import de.robnice.elly.wear.R
import kotlinx.coroutines.delay

// Elly looks up over the bottom edge once when the remote opens and ducks away again, so the app shows whose
// it is without taking room from the keys. It takes no touches.
@Composable
fun EllyPeek() {
    if (!ValueAnimator.areAnimatorsEnabled()) return
    val size = dimensionResource(R.dimen.elly_peek_size)
    val visible = dimensionResource(R.dimen.elly_peek_visible)
    val shown = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(START_DELAY_MILLIS)
        shown.animateTo(1f, tween(MOVE_MILLIS, easing = FastOutSlowInEasing))
        delay(HOLD_MILLIS)
        shown.animateTo(0f, tween(MOVE_MILLIS, easing = FastOutSlowInEasing))
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Image(
            painter = painterResource(R.drawable.elly),
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .graphicsLayer { translationY = size.toPx() - visible.toPx() * shown.value }
        )
    }
}

private const val START_DELAY_MILLIS = 300L
private const val MOVE_MILLIS = 400
private const val HOLD_MILLIS = 1_000L
