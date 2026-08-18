package com.utags.androidpc.CensusHub.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.utags.androidpc.CensusHub.R
import com.utags.androidpc.CensusHub.presentation.theme.Figtree

private val BgGreen     = Color(0xFF1A3B1C)
private val UtaGreen    = Color(0xFF00BB47)
private val LimeGreen   = Color(0xFFB1F946)
private val White       = Color.White
private val WhiteDim    = Color.White.copy(alpha = 0.55f)

@Composable
fun SplashScreen(onFinished: () -> Unit) {

    // ── entrance animations ──────────────────────────────────────────────────
    val logoScale = remember { Animatable(0.4f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val tagAlpha  = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(350)) }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMedium
                )
            )
        }
        delay(400)
        launch { textAlpha.animateTo(1f, tween(350, easing = FastOutSlowInEasing)) }
        delay(250)
        launch { tagAlpha.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
        delay(1800)
        onFinished()
    }

    // ── pulsing RFID rings ───────────────────────────────────────────────────
    val rings = rememberInfiniteTransition(label = "rings")
    val r1 by rings.animateFloat(0f, 1f,
        infiniteRepeatable(tween(2200, easing = LinearEasing, delayMillis =    0), RepeatMode.Restart), "r1")
    val r2 by rings.animateFloat(0f, 1f,
        infiniteRepeatable(tween(2200, easing = LinearEasing, delayMillis =  700), RepeatMode.Restart), "r2")
    val r3 by rings.animateFloat(0f, 1f,
        infiniteRepeatable(tween(2200, easing = LinearEasing, delayMillis = 1400), RepeatMode.Restart), "r3")

    // ── loading dot pulse ────────────────────────────────────────────────────
    val dots = rememberInfiniteTransition(label = "dots")
    val d1 by dots.animateFloat(0.25f, 1f,
        infiniteRepeatable(tween(600, easing = FastOutSlowInEasing, delayMillis =   0), RepeatMode.Reverse), "d1")
    val d2 by dots.animateFloat(0.25f, 1f,
        infiniteRepeatable(tween(600, easing = FastOutSlowInEasing, delayMillis = 180), RepeatMode.Reverse), "d2")
    val d3 by dots.animateFloat(0.25f, 1f,
        infiniteRepeatable(tween(600, easing = FastOutSlowInEasing, delayMillis = 360), RepeatMode.Reverse), "d3")

    // ── layout ───────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGreen),
        contentAlignment = Alignment.Center
    ) {

        // RFID expanding rings centred behind logo
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centre   = Offset(size.width / 2f, size.height / 2f)
            val minR     = 80.dp.toPx()
            val maxR     = 250.dp.toPx()
            val sw       = 1.5.dp.toPx()
            listOf(r1, r2, r3).forEach { p ->
                val r = minR + p * (maxR - minR)
                val a = (1f - p) * 0.38f
                if (a > 0f) drawCircle(UtaGreen.copy(alpha = a), r, centre, style = Stroke(sw))
            }
        }

        // Logo + wordmarks
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // UTAGS ear-tag icon
            Image(
                painter            = painterResource(R.drawable.ic_utags_logo),
                contentDescription = "UTAGS",
                modifier           = Modifier
                    .size(130.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            )

            Spacer(Modifier.height(24.dp))

            // "UTAGS" wordmark
            Text(
                text          = "UTAGS",
                fontFamily    = Figtree,
                fontWeight    = FontWeight.Bold,
                fontSize      = 42.sp,
                letterSpacing = 7.sp,
                color         = White,
                modifier      = Modifier.alpha(textAlpha.value)
            )

            Spacer(Modifier.height(6.dp))

            // Product name in lime
            Text(
                text          = "CENSUS HUB",
                fontFamily    = Figtree,
                fontWeight    = FontWeight.Medium,
                fontSize      = 14.sp,
                letterSpacing = 4.sp,
                color         = LimeGreen,
                modifier      = Modifier.alpha(textAlpha.value)
            )

            Spacer(Modifier.height(36.dp))

            // Website tagline
            Text(
                text       = "Animal Identification that is\nFast, Accurate and Safe",
                fontFamily = Figtree,
                fontWeight = FontWeight.Normal,
                fontSize   = 13.sp,
                lineHeight = 20.sp,
                color      = WhiteDim,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.alpha(tagAlpha.value)
            )
        }

        // Three pulsing loading dots at bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .alpha(tagAlpha.value),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(Modifier.size(7.dp)) { drawCircle(UtaGreen.copy(alpha = d1)) }
            Canvas(Modifier.size(7.dp)) { drawCircle(UtaGreen.copy(alpha = d2)) }
            Canvas(Modifier.size(7.dp)) { drawCircle(UtaGreen.copy(alpha = d3)) }
        }
    }
}
