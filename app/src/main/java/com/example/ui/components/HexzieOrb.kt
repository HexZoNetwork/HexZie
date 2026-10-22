package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AgentState
import com.example.ui.theme.HexzieAccent
import com.example.ui.theme.HexzieAccentElectric
import com.example.ui.theme.HexzieAllowGreen
import com.example.ui.theme.HexzieCyan
import com.example.ui.theme.HexzieDenyRed
import com.example.ui.theme.HexzieOutline
import com.example.ui.theme.HexziePendingAmber
import com.example.ui.theme.HexzieViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HexzieOrb(
    agentState: AgentState,
    micLevel: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 230.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hexzie_orb_transition")
    var isPressed by remember { mutableStateOf(false) }

    // Touch press scale
    val touchScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "orb_touch_scale"
    )

    // Idle organic breathing animation
    val idleBreathing by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_breathing"
    )

    // Outer aura pulse
    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    // Fast clockwise rotation for Thinking / Acting
    val rotationFast by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_fast"
    )

    // Slow counter-clockwise rotation for subtle gyro effect
    val rotationCounter by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_counter"
    )

    // Speaking rhythm waveform pulse
    val speakingRhythm by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speaking_rhythm"
    )

    // Ripple wave for listening
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "listening_ripple"
    )

    // Determine target primary and secondary colors based on state
    val (targetPrimary, targetSecondary) = when (agentState) {
        AgentState.IDLE -> HexzieAccent to HexzieCyan
        AgentState.LISTENING -> HexzieCyan to HexzieAccentElectric
        AgentState.THINKING -> HexzieViolet to HexzieAccent
        AgentState.ACTING -> HexzieAccent to HexzieCyan
        AgentState.SPEAKING -> HexzieAllowGreen to HexzieCyan
        AgentState.AWAITING_CONFIRMATION -> HexziePendingAmber to Color(0xFFFFB300)
        AgentState.EMERGENCY_STOP, AgentState.ERROR -> HexzieDenyRed to Color(0xFFB91C1C)
    }

    // Color animation with instant snap for emergency stop
    val primaryColor by animateColorAsState(
        targetValue = targetPrimary,
        animationSpec = if (agentState == AgentState.EMERGENCY_STOP) tween(0) else spring(stiffness = Spring.StiffnessLow),
        label = "orb_primary_color"
    )

    val secondaryColor by animateColorAsState(
        targetValue = targetSecondary,
        animationSpec = if (agentState == AgentState.EMERGENCY_STOP) tween(0) else spring(stiffness = Spring.StiffnessLow),
        label = "orb_secondary_color"
    )

    Box(
        modifier = modifier
            .size(size)
            .testTag("hexzie_orb")
            .pointerInput(agentState) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.58f

            // Dynamic scale factor based on agent state
            val stateScale = when (agentState) {
                AgentState.IDLE -> idleBreathing
                AgentState.LISTENING -> 1.05f + (micLevel.coerceIn(0f, 1f) * 0.28f)
                AgentState.THINKING -> 1.02f
                AgentState.ACTING -> 1.04f
                AgentState.SPEAKING -> speakingRhythm
                AgentState.AWAITING_CONFIRMATION -> 1.0f
                AgentState.EMERGENCY_STOP, AgentState.ERROR -> 0.88f
            }

            val radius = baseRadius * stateScale * touchScale

            // 1. Deep Atmospheric Outer Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = if (agentState == AgentState.LISTENING || agentState == AgentState.SPEAKING) 0.45f else 0.22f),
                        secondaryColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.85f * (if (agentState == AgentState.LISTENING) auraPulse else 1.0f)
                ),
                radius = radius * 1.85f * (if (agentState == AgentState.LISTENING) auraPulse else 1.0f),
                center = center
            )

            // 2. Active Audio Ripple Waves (When Listening)
            if (agentState == AgentState.LISTENING) {
                val wave1Radius = radius + (rippleProgress * 38.dp.toPx())
                val wave1Alpha = (1f - rippleProgress).coerceIn(0f, 1f) * 0.7f
                drawCircle(
                    color = primaryColor.copy(alpha = wave1Alpha),
                    radius = wave1Radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                val wave2Progress = (rippleProgress + 0.5f) % 1.0f
                val wave2Radius = radius + (wave2Progress * 38.dp.toPx())
                val wave2Alpha = (1f - wave2Progress).coerceIn(0f, 1f) * 0.5f
                drawCircle(
                    color = secondaryColor.copy(alpha = wave2Alpha),
                    radius = wave2Radius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // 3. Gyroscopic Rotating Orbit Rings (Thinking & Acting)
            if (agentState == AgentState.THINKING || agentState == AgentState.ACTING) {
                // Clockwise Primary Ring
                rotate(degrees = rotationFast, pivot = center) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                primaryColor,
                                secondaryColor,
                                primaryColor.copy(alpha = 0.1f),
                                Color.Transparent,
                                primaryColor
                            ),
                            center = center
                        ),
                        radius = radius * 1.22f,
                        center = center,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Counter-Clockwise Secondary Ring with Ellipse Tilt
                rotate(degrees = rotationCounter, pivot = center) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                secondaryColor,
                                HexzieCyan.copy(alpha = 0.8f),
                                Color.Transparent,
                                secondaryColor.copy(alpha = 0.2f),
                                secondaryColor
                            ),
                            center = center
                        ),
                        radius = radius * 1.36f,
                        center = center,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Orbiting Satellite Node (acting progress)
                    if (agentState == AgentState.ACTING) {
                        val satelliteAngle = (rotationFast * (PI.toFloat() / 180f))
                        val satX = center.x + (radius * 1.36f) * cos(satelliteAngle)
                        val satY = center.y + (radius * 1.36f) * sin(satelliteAngle)
                        drawCircle(
                            color = Color.White,
                            radius = 4.5.dp.toPx(),
                            center = Offset(satX, satY)
                        )
                        drawCircle(
                            color = HexzieCyan,
                            radius = 7.dp.toPx(),
                            center = Offset(satX, satY),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
            }

            // 4. Speaking Acoustic Bars / Rays
            if (agentState == AgentState.SPEAKING) {
                val barCount = 16
                for (i in 0 until barCount) {
                    val angle = (i * (360f / barCount)) * (PI.toFloat() / 180f)
                    val barHeight = 8.dp.toPx() + (sin(speakingRhythm * 10f + i).coerceAtLeast(0f) * 16.dp.toPx())
                    val startOffset = Offset(
                        x = center.x + (radius * 1.08f) * cos(angle),
                        y = center.y + (radius * 1.08f) * sin(angle)
                    )
                    val endOffset = Offset(
                        x = center.x + (radius * 1.08f + barHeight) * cos(angle),
                        y = center.y + (radius * 1.08f + barHeight) * sin(angle)
                    )
                    drawLine(
                        color = HexzieAllowGreen.copy(alpha = 0.85f),
                        start = startOffset,
                        end = endOffset,
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 5. Emergency Shockwave Ring
            if (agentState == AgentState.EMERGENCY_STOP) {
                drawCircle(
                    color = HexzieDenyRed.copy(alpha = 0.8f),
                    radius = radius * 1.25f,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // 6. Main Luminous Core with Specular Light Reflection
            val lightSource = center.minus(Offset(radius * 0.32f, radius * 0.32f))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.96f),
                        primaryColor,
                        secondaryColor,
                        primaryColor.copy(alpha = 0.8f)
                    ),
                    center = lightSource,
                    radius = radius * 1.1f
                ),
                radius = radius,
                center = center
            )

            // 7. High-Tech Hexagonal / Rim Accent
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        primaryColor.copy(alpha = 0.6f),
                        secondaryColor.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.8f)
                    ),
                    center = center
                ),
                radius = radius,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
    }
}
