package com.example.aituner.ui.screen

import com.example.aituner.R

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aituner.ui.component.TuningDial
import com.example.aituner.ui.theme.*
import androidx.compose.ui.res.stringResource

@Composable
fun TunerScreen(
    viewModel: TunerViewModel = hiltViewModel(),
    onNavigateToChat: () -> Unit = {},
    onNavigateToSing: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.tuner_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // Settings gear
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tuning dial
            TuningDial(
                centsOffset = uiState.centsOffset,
                isInTune = uiState.tuningStatus == TuningStatus.InTune,
                targetNote = uiState.targetStringName,
                detectedNote = uiState.noteName,
                status = uiState.tuningStatus
            )

            Spacer(modifier = Modifier.height(24.dp))

            // String selector indicator
            if (uiState.isListening) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 0..5) {
                        val isTarget = i == uiState.targetStringIndex
                        val isInTune = isTarget && uiState.tuningStatus == TuningStatus.InTune

                        Text(
                            text = when (i) {
                                0 -> stringResource(R.string.tuner_string_e)
                                1 -> stringResource(R.string.tuner_string_a)
                                2 -> stringResource(R.string.tuner_string_d)
                                3 -> stringResource(R.string.tuner_string_g)
                                4 -> stringResource(R.string.tuner_string_b)
                                5 -> stringResource(R.string.tuner_string_e_high)
                                else -> "?"
                            },
                            fontSize = 18.sp,
                            fontWeight = if (isTarget) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isInTune -> AccentGreen
                                isTarget -> AccentBlue
                                else -> TextMuted
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .then(
                                    if (isTarget)
                                        Modifier.background(
                                            if (isInTune) AccentGreen.copy(alpha = 0.2f)
                                            else AccentBlue.copy(alpha = 0.15f)
                                        )
                                    else Modifier
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Start/Stop button
            Button(
                onClick = {
                    if (uiState.isListening) viewModel.stopTuner()
                    else viewModel.startTuner()
                },
                modifier = Modifier
                    .size(72.dp)
                    .padding(bottom = 32.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isListening) AccentRed else AccentGreen
                ),
                elevation = ButtonDefaults.buttonElevation(8.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (uiState.isListening) stringResource(R.string.tuner_stop) else stringResource(R.string.tuner_start),
                    tint = DeepBlack,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}
