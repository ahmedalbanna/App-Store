package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OasisSandLight

/**
 * Phase 2.4: Shimmer placeholder for loading states.
 */
@Composable
fun ShimmerPlaceholder(
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "shimmer")
    val alpha by infinite.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(OasisSandLight.copy(alpha = alpha))
    )
}

@Composable
fun ShimmerRowPlaceholder() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerPlaceholder(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShimmerPlaceholder(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp))
            ShimmerPlaceholder(modifier = Modifier.fillMaxWidth(0.5f).height(12.dp))
            ShimmerPlaceholder(modifier = Modifier.fillMaxWidth(0.4f).height(10.dp))
        }
        ShimmerPlaceholder(modifier = Modifier.width(80.dp).height(32.dp).clip(RoundedCornerShape(18.dp)))
    }
}
