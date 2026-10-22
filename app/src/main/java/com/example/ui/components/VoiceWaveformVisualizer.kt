package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgentState
import com.example.ui.theme.HexzieAccent
import com.example.ui.theme.HexzieAccentElectric
import com.example.ui.theme.HexzieAllowGreen
import com.example.ui.theme.HexzieCyan
import com.example.ui.theme.HexzieDenyRed
import com.example.ui.theme.HexzieOnSurface
import com.example.ui.theme.HexzieOnSurfaceMuted
import com.example.ui.theme.HexzieOutline
import com.example.ui.theme.HexziePendingAmber
import com.example.ui.theme.HexzieSurfaceElevated
import com.example.ui.theme.HexzieSurfaceVariant
import com.example.ui.theme.HexzieViolet
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.SpaceGrotesk
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Center Voice Stage: Combines the interactive HexZie Orb with real-time reactive
 * concentric acoustic ripple halos, radial frequency equalizer spikes, and fluid sinusoidal waveforms.
 */
@Composable
fun CenterVoiceRippleStage(
    agentState: AgentState,
    micLevel: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    stageSize: Dp = 330.dp
) {
    val isListening = agentState == AgentState.LISTENING
    val isSpeaking = agentState == AgentState.SPEAKING

    // Smooth microphone amplitude reaction
    val smoothedMicLevel by animateFloatAsState(
        targetValue = if (isListening) micLevel.coerceIn(0.08f, 1.0f) else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 800f),
        label = "smoothed_mic_level"
    )

    // Infinite phase clock for fluid ripples and waveforms
    val infiniteTransition = rememberInfiniteTransition(label = "voice_visualizer_clock")

    val ripplePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_phase"
    )

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Box(
        modifier = modifier
            .size(stageSize)
            .testTag("center_voice_stage"),
        contentAlignment = Alignment.Center
    ) {
        // 1. Concentric Real-Time Microphone Ripples (Rendered Behind Orb)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val orbRadius = (220.dp.toPx() / 2f) * 0.58f

            if (isListening || isSpeaking) {
                drawMicrophoneRipples(
                    center = center,
                    orbRadius = orbRadius,
                    ripplePhase = ripplePhase,
                    micLevel = if (isListening) smoothedMicLevel else 0.5f,
                    isSpeaking = isSpeaking
                )

                // 2. Radial 360-degree Frequency Spikes around Orb perimeter
                drawRadialFrequencySpikes(
                    center = center,
                    orbRadius = orbRadius,
                    phase = wavePhase,
                    micLevel = if (isListening) smoothedMicLevel else 0.4f,
                    isSpeaking = isSpeaking
                )
            }
        }

        // 3. Central Interactive Orb
        HexzieOrb(
            agentState = agentState,
            micLevel = smoothedMicLevel,
            onClick = onClick,
            onLongClick = onLongClick,
            size = 220.dp
        )
    }
}

/**
 * Real-time Concentric Ripple Waves radiating outward from the center orb.
 * Reacts directly to microphone RMS decibel levels with dynamic radius expansion,
 * stroke width modulation, and cyan/electric-blue glow.
 */
private fun DrawScope.drawMicrophoneRipples(
    center: Offset,
    orbRadius: Offset = Offset.Zero,
    orbRadiusFloat: Float = 60.dp.toPx(),
    ripplePhase: Float,
    micLevel: Float,
    isSpeaking: Boolean
) {
    // 4 Concentric ripple wave fronts with staggered phase offsets
    val waveCount = 4
    val maxRadius = size.minDimension / 2f * 0.96f
    val baseRadius = orbRadiusFloat * 1.05f
    val spreadDistance = maxRadius - baseRadius

    val primaryColor = if (isSpeaking) HexzieAllowGreen else HexzieCyan
    val secondaryColor = if (isSpeaking) HexzieCyan else HexzieAccentElectric

    for (i in 0 until waveCount) {
        val waveFraction = (ripplePhase + (i.toFloat() / waveCount)) % 1f
        // Dynamic amplitude pulse: louder mic input expands ripple further
        val amplitudeBoost = (micLevel * 28.dp.toPx() * (1f - waveFraction))
        val currentRadius = baseRadius + (waveFraction * spreadDistance) + amplitudeBoost

        // Alpha decays smoothly as the ripple expands towards outer edge
        val alphaDecay = ((1f - waveFraction) * (0.25f + micLevel * 0.65f)).coerceIn(0f, 0.95f)
        val strokeWidth = (3.5.dp.toPx() * (1f - (waveFraction * 0.6f)) * (1f + micLevel * 0.8f))

        // Outer glow stroke
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = alphaDecay),
                    secondaryColor.copy(alpha = alphaDecay * 0.5f),
                    Color.Transparent
                ),
                center = center,
                radius = currentRadius + 8.dp.toPx()
            ),
            radius = currentRadius,
            center = center,
            style = Stroke(width = strokeWidth + 4.dp.toPx())
        )

        // Sharp luminous wavefront ring
        drawCircle(
            color = if (i % 2 == 0) primaryColor.copy(alpha = alphaDecay) else secondaryColor.copy(alpha = alphaDecay),
            radius = currentRadius,
            center = center,
            style = Stroke(width = strokeWidth)
        )
    }
}

private fun DrawScope.drawMicrophoneRipples(
    center: Offset,
    orbRadius: Float,
    ripplePhase: Float,
    micLevel: Float,
    isSpeaking: Boolean
) {
    drawMicrophoneRipples(
        center = center,
        orbRadiusFloat = orbRadius,
        ripplePhase = ripplePhase,
        micLevel = micLevel,
        isSpeaking = isSpeaking
    )
}

/**
 * 360-degree Radial Equalizer Spikes radiating outward from the orb boundary,
 * oscillating in real-time according to speech frequencies and mic decibels.
 */
private fun DrawScope.drawRadialFrequencySpikes(
    center: Offset,
    orbRadius: Float,
    phase: Float,
    micLevel: Float,
    isSpeaking: Boolean
) {
    val spikeCount = 36
    val colorPrimary = if (isSpeaking) HexzieAllowGreen else HexzieCyan
    val colorSecondary = if (isSpeaking) HexzieCyan else HexzieAccent

    val baseRim = orbRadius * 1.04f

    for (i in 0 until spikeCount) {
        val angleRad = (i * (360f / spikeCount)) * (PI.toFloat() / 180f)

        // Wave oscillation formula driven by micLevel and index phase
        val harmonic1 = sin(phase * 1.8f + i * 0.55f)
        val harmonic2 = cos(phase * 2.4f - i * 0.35f)
        val spikeHeight = 4.dp.toPx() + (micLevel * 32.dp.toPx() * ((harmonic1 + harmonic2).coerceAtLeast(0.1f) / 2f))

        val startOffset = Offset(
            x = center.x + baseRim * cos(angleRad),
            y = center.y + baseRim * sin(angleRad)
        )
        val endOffset = Offset(
            x = center.x + (baseRim + spikeHeight) * cos(angleRad),
            y = center.y + (baseRim + spikeHeight) * sin(angleRad)
        )

        val spikeAlpha = (0.35f + micLevel * 0.65f).coerceIn(0.2f, 1f)

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(colorPrimary.copy(alpha = spikeAlpha), colorSecondary.copy(alpha = spikeAlpha * 0.4f)),
                start = startOffset,
                end = endOffset
            ),
            start = startOffset,
            end = endOffset,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/**
 * Fluid Harmonic Real-Time Audio Waveform Ribbon that sits right beneath the center orb stage.
 * Draws 3 harmonic sinusoidal bezier curves with glowing gradient fills that react to mic amplitude.
 */
@Composable
fun RealtimeFluidWaveform(
    micLevel: Float,
    isListening: Boolean,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fluid_waveform_clock")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val smoothedLevel by animateFloatAsState(
        targetValue = if (isListening) micLevel.coerceIn(0.1f, 1.0f) else if (isSpeaking) 0.55f else 0.05f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "wave_smooth_level"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(HexzieSurfaceVariant.copy(alpha = 0.65f))
            .border(1.dp, HexzieOutline, RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val midY = size.height / 2f
            val baseAmplitude = (size.height * 0.42f) * smoothedLevel

            // 1. Tertiary Harmonic Wave (Violet background glow)
            drawSineWave(
                midY = midY,
                width = width,
                amplitude = baseAmplitude * 0.65f,
                frequency = 2.5f,
                phase = phase * 0.8f,
                strokeColor = HexzieViolet.copy(alpha = 0.5f),
                strokeWidth = 1.5.dp.toPx()
            )

            // 2. Secondary Harmonic Wave (Electric Blue)
            drawSineWave(
                midY = midY,
                width = width,
                amplitude = baseAmplitude * 0.85f,
                frequency = 3.2f,
                phase = phase * 1.3f + 1.2f,
                strokeColor = HexzieAccentElectric.copy(alpha = 0.75f),
                strokeWidth = 2.dp.toPx()
            )

            // 3. Primary Harmonic Wave (Neon Cyan with glowing stroke)
            val primaryColor = if (isSpeaking) HexzieAllowGreen else HexzieCyan
            drawSineWave(
                midY = midY,
                width = width,
                amplitude = baseAmplitude,
                frequency = 2.0f,
                phase = phase,
                strokeColor = primaryColor,
                strokeWidth = 2.8.dp.toPx(),
                hasGlow = true
            )
        }

        // Center overlay indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isListening) HexzieCyan else if (isSpeaking) HexzieAllowGreen else HexzieOnSurfaceMuted)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (isListening) "MIC ACTIVE ${(smoothedLevel * 100).toInt()}%" else if (isSpeaking) "SPEECH SYNTHESIS" else "STANDBY",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = JetBrainsMono,
                color = if (isListening) HexzieCyan else if (isSpeaking) HexzieAllowGreen else HexzieOnSurfaceMuted
            )
        }
    }
}

/**
 * Helper to draw a smooth sinusoidal curve with windowed Hanning attenuation at screen boundaries.
 */
private fun DrawScope.drawSineWave(
    midY: Float,
    width: Float,
    amplitude: Float,
    frequency: Float,
    phase: Float,
    strokeColor: Color,
    strokeWidth: Float,
    hasGlow: Boolean = false
) {
    val path = Path()
    val step = 4f
    var x = 0f

    var first = true
    while (x <= width) {
        val normalizedX = x / width
        // Hanning window function: zeroes out wave at ends so it fits inside the container seamlessly
        val window = sin(normalizedX * PI).toFloat()
        val y = midY + sin(normalizedX * frequency * (2 * PI).toFloat() + phase) * amplitude * window

        if (first) {
            path.moveTo(x, y)
            first = false
        } else {
            path.lineTo(x, y)
        }
        x += step
    }

    if (hasGlow) {
        drawPath(
            path = path,
            color = strokeColor.copy(alpha = 0.35f),
            style = Stroke(width = strokeWidth + 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    drawPath(
        path = path,
        color = strokeColor,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}
