package com.example.aituner.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.aituner.R
import com.example.aituner.ui.screen.TuningStatus
import com.example.aituner.ui.theme.*

/**
 * Compact tuning display — detected note, needle bar, status.
 */
@Composable
fun TuningDial(
    centsOffset: Float,      // -50 to +50
    isInTune: Boolean,
    targetNote: String,
    detectedNote: String,
    status: TuningStatus,
    modifier: Modifier = Modifier
) {
    val animatedOffset by animateFloatAsState(
        targetValue = centsOffset.coerceIn(-50f, 50f),
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)
    )

    val dialColor by animateColorAsState(
        targetValue = when (status) {
            TuningStatus.InTune -> AccentGreen
            TuningStatus.Close -> AccentYellow
            TuningStatus.NoSignal -> TextMuted
            else -> AccentRed
        },
        animationSpec = tween(200)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ── Detected note (large) ──
        if (status != TuningStatus.NoSignal) {
            Text(
                text = detectedNote,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = dialColor,
                letterSpacing = (-2).sp
            )
        } else {
            Text(
                text = "--",
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = (-2).sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Needle bar ──
        val barWidth = 240.dp
        val barHeight = 8.dp
        Canvas(
            modifier = Modifier
                .width(barWidth)
                .height(barHeight + 16.dp)
        ) {
            val barY = size.height / 2
            val barLeft = 0f
            val barRight = size.width
            val barCenterX = size.width / 2

            // Background track
            drawRoundRect(
                color = CardDark,
                topLeft = Offset(barLeft, barY - barHeight.toPx() / 2),
                size = androidx.compose.ui.geometry.Size(size.width, barHeight.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barHeight.toPx() / 2)
            )

            // Center mark
            drawLine(
                color = TextMuted,
                start = Offset(barCenterX, barY - 12.dp.toPx()),
                end = Offset(barCenterX, barY + 12.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )

            if (status != TuningStatus.NoSignal) {
                // Needle position: -50 cents → far left, +50 cents → far right
                val needleX = barCenterX + (animatedOffset / 50f) * (size.width / 2)

                // In-tune zone (green band at center: ±10 cents)
                val zoneHalfWidth = (10f / 50f) * (size.width / 2)
                drawRoundRect(
                    color = AccentGreen.copy(alpha = 0.15f),
                    topLeft = Offset(barCenterX - zoneHalfWidth, barY - barHeight.toPx()),
                    size = androidx.compose.ui.geometry.Size(zoneHalfWidth * 2, barHeight.toPx() * 2.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barHeight.toPx())
                )

                // Needle
                drawCircle(
                    color = dialColor,
                    radius = 7.dp.toPx(),
                    center = Offset(needleX, barY)
                )
                drawCircle(
                    color = dialColor.copy(alpha = 0.3f),
                    radius = 14.dp.toPx(),
                    center = Offset(needleX, barY)
                )
            }
        }

        // ── Labels under bar ──
        Row(
            modifier = Modifier.width(barWidth).padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("-50¢", fontSize = 10.sp, color = TextMuted)
            Text("0¢", fontSize = 10.sp, color = TextMuted)
            Text("+50¢", fontSize = 10.sp, color = TextMuted)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Status text ──
        Text(
            text = when (status) {
                TuningStatus.NoSignal -> stringResource(R.string.tuner_waiting)
                TuningStatus.TooLow -> if (centsOffset < -15) stringResource(R.string.tuner_too_low) else stringResource(R.string.tuner_flat)
                TuningStatus.Close -> stringResource(R.string.tuner_close)
                TuningStatus.InTune -> stringResource(R.string.tuner_in_tune)
                TuningStatus.TooHigh -> if (centsOffset > 15) stringResource(R.string.tuner_too_high) else stringResource(R.string.tuner_sharp)
            },
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = when (status) {
                TuningStatus.InTune -> AccentGreen
                TuningStatus.Close -> AccentYellow
                TuningStatus.NoSignal -> TextMuted
                else -> TextPrimary
            }
        )

        // ── Target hint ──
        if (targetNote.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.tuner_target, targetNote),
                fontSize = 14.sp,
                color = TextSecondary
            )
        }

        // ── Cent offset ──
        if (status != TuningStatus.NoSignal && status != TuningStatus.InTune) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.tuner_offset, centsOffset),
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
    }
}
