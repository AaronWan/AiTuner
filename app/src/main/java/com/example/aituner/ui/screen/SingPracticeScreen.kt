package com.example.aituner.ui.screen

import com.example.aituner.R

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aituner.ui.theme.*
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SingPracticeScreen(
    viewModel: SingPracticeViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var useJianpu by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopPractice() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sing_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopPractice()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.chat_back))
                    }
                },
                actions = {
                    // Jianpu / Note name toggle
                    Text(
                        if (useJianpu) stringResource(R.string.sing_jianpu) else stringResource(R.string.sing_note),
                        color = AccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentCyan.copy(alpha = 0.15f))
                            .clickable { useJianpu = !useJianpu }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DeepBlack
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state.phase) {
                Phase.IDLE, Phase.GENERATING -> IdlePhase(state, viewModel)
                Phase.GENERATED -> GeneratedPhase(state, viewModel, useJianpu)
                Phase.COUNTDOWN -> CountdownPhase(state, useJianpu)
                Phase.SINGING -> SingingPhase(state, viewModel, useJianpu)
                Phase.REVIEW -> ReviewPhase(state, viewModel, useJianpu)
            }
        }
    }
}

// ─── IDLE / GENERATING ───

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdlePhase(state: SingPracticeState, viewModel: SingPracticeViewModel) {
    var prompt by remember { mutableStateOf(state.generationPrompt) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎤", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.sing_idle_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.sing_idle_desc),
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // Prompt input
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            placeholder = { Text(stringResource(R.string.sing_prompt_hint), color = TextMuted, fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = darkFieldColors(),
            minLines = 2,
            maxLines = 3
        )

        // Quick prompts
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(stringResource(R.string.sing_tag_scale), stringResource(R.string.sing_tag_twinkle), stringResource(R.string.sing_tag_birthday), stringResource(R.string.sing_tag_interval)).forEach { tag ->
                SuggestionChipSmall(tag) {
                    prompt = tag
                }
            }
        }

        // Built-in preset songs (no AI needed)
        val presets = viewModel.presetList
        if (presets.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { preset ->
                    SuggestionChipSmall(preset.title) {
                        viewModel.loadPreset(preset.id)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Generate button
        Button(
            onClick = { viewModel.generateScore(prompt) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(color = DeepBlack, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.sing_generating))
            } else {
                Icon(Icons.Default.AutoAwesome, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.sing_generate), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (state.error != null) {
            Spacer(Modifier.height(16.dp))
            Text(state.error!!, color = AccentRed, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

// ─── GENERATED — Full-screen score display ───

@Composable
private fun GeneratedPhase(state: SingPracticeState, viewModel: SingPracticeViewModel, useJianpu: Boolean) {
    val score = state.generatedScore ?: return
    val hasLyrics = score.notes.any { it.syllable.isNotBlank() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Title ──
            Text("🎵  ${score.title}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "${score.notes.size} 个音符  ·  ~${"%.0f".format(score.notes.sumOf { it.durationSec.toDouble() })}s",
                fontSize = 13.sp,
                color = TextMuted
            )

            Spacer(Modifier.height(28.dp))

            // ── Score card ──
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(0.5.dp, AccentCyan.copy(alpha = 0.12f))
            ) {
                Column {
                    // Score content with a subtle "staff" top bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(AccentCyan.copy(alpha = 0.18f))
                            .padding(horizontal = 16.dp)
                    )

                    // ── Notes row ──
                    LazyRow(
                        modifier = Modifier.padding(top = 24.dp, bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        item { Spacer(Modifier.width(20.dp)) }

                        itemsIndexed(score.notes) { index, note ->
                            val beat = index % 4

                            Column(
                                modifier = Modifier.width(noteWidth(note)),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (note.isRest) {
                                    Text(
                                        "0",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextMuted.copy(alpha = 0.3f)
                                    )
                                } else {
                                    Text(
                                        text = noteDisplay(note, useJianpu),
                                        fontSize = 30.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        letterSpacing = 0.sp
                                    )
                                }

                                // Duration marker
                                Spacer(Modifier.height(2.dp))
                                if (!note.isRest && note.durationSec >= 3f) {
                                    Text("——", fontSize = 12.sp, color = TextSecondary.copy(alpha = 0.5f))
                                } else if (!note.isRest && note.durationSec >= 2f) {
                                    Text("—", fontSize = 12.sp, color = TextSecondary.copy(alpha = 0.5f))
                                } else if (!note.isRest && note.durationSec >= 1.5f) {
                                    Text("·", fontSize = 14.sp, color = TextSecondary.copy(alpha = 0.4f))
                                } else {
                                    Spacer(Modifier.height(14.dp))
                                }

                                // Lyric
                                if (hasLyrics && note.syllable.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        note.syllable,
                                        fontSize = 11.sp,
                                        color = TextSecondary.copy(alpha = 0.55f)
                                    )
                                } else {
                                    Spacer(Modifier.height(14.dp))
                                }
                            }

                            // Bar line
                            if (beat == 3 && index < score.notes.lastIndex) {
                                Spacer(Modifier.width(2.dp))
                                Box(
                                    modifier = Modifier
                                        .width(1.5.dp)
                                        .height(60.dp)
                                        .background(TextMuted.copy(alpha = 0.18f))
                                )
                                Spacer(Modifier.width(2.dp))
                            }
                        }

                        item { Spacer(Modifier.width(20.dp)) }
                    }

                    // Bottom accent line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(AccentCyan.copy(alpha = 0.18f))
                    )
                }
            }

            // ── Legend ──
            Spacer(Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(AccentCyan.copy(alpha = 0.4f), RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(6.dp))
                    Text("1 = C4", fontSize = 12.sp, color = TextSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("1̇", fontSize = 14.sp, color = TextPrimary)
                    Spacer(Modifier.width(4.dp))
                    Text("高八度", fontSize = 11.sp, color = TextMuted)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("—", fontSize = 12.sp, color = TextSecondary)
                    Spacer(Modifier.width(4.dp))
                    Text("二分", fontSize = 11.sp, color = TextMuted)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("0", fontSize = 14.sp, color = TextMuted)
                    Spacer(Modifier.width(4.dp))
                    Text("休止", fontSize = 11.sp, color = TextMuted)
                }
            }

            Spacer(Modifier.height(80.dp))
        }

        // ── Play FAB ──
        Button(
            onClick = { viewModel.startPractice() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .size(68.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
            elevation = ButtonDefaults.buttonElevation(8.dp)
        ) {
            Icon(Icons.Default.PlayArrow, null, Modifier.size(32.dp), tint = DeepBlack)
        }
    }
}

private fun noteWidth(note: ScoreNote): androidx.compose.ui.unit.Dp = when {
    note.durationSec >= 3f -> 44.dp
    note.durationSec >= 2f -> 38.dp
    else -> 30.dp
}

// ─── COUNTDOWN ───

@Composable
private fun CountdownPhase(state: SingPracticeState, useJianpu: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = state.countdown,
            transitionSpec = {
                slideInVertically { it } + fadeIn() togetherWith
                slideOutVertically { -it } + fadeOut()
            }
        ) { count ->
            Text(
                text = if (count > 0) "$count" else stringResource(R.string.sing_countdown_go),
                fontSize = 96.sp,
                fontWeight = FontWeight.Black,
                color = AccentGreen
            )
        }
    }
}

// ─── SINGING ───

@Composable
private fun SingingPhase(state: SingPracticeState, viewModel: SingPracticeViewModel, useJianpu: Boolean) {
    val score = state.score ?: return
    val listState = rememberLazyListState()

    // Auto-scroll to current note
    LaunchedEffect(state.currentIndex) {
        if (state.currentIndex < score.notes.size) {
            listState.animateScrollToItem(
                index = (state.currentIndex - 1).coerceAtLeast(0),
                scrollOffset = 0
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // ── Full score overview — horizontal scroll, current note highlighted ──
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark)
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                // Section label
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎵", fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = score.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.sing_notes_count, score.notes.size),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                // Full score row
                LazyRow(
                    state = listState,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(score.notes) { index, note ->
                        val isCurrent = index == state.currentIndex
                        val isPast = index < state.currentIndex
                        val result = state.results.getOrNull(index)

                        Card(
                            modifier = Modifier.width(52.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isCurrent -> AccentGreen.copy(alpha = 0.25f)
                                    isPast && result?.verdict == Verdict.PERFECT -> AccentGreen.copy(alpha = 0.1f)
                                    isPast && result?.verdict == Verdict.GOOD -> AccentYellow.copy(alpha = 0.1f)
                                    isPast -> AccentRed.copy(alpha = 0.08f)
                                    note.isRest -> TextMuted.copy(alpha = 0.08f)
                                    else -> CardDark
                                }
                            ),
                            border = if (isCurrent) BorderStroke(2.dp, AccentGreen) else null
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = when {
                                        note.isRest -> "—"
                                        useJianpu -> JianpuConverter.toJianpu(note.noteName)
                                        else -> note.noteName
                                    },
                                    fontSize = if (isCurrent) 22.sp else 16.sp,
                                    fontWeight = if (isCurrent) FontWeight.Black else FontWeight.SemiBold,
                                    color = when {
                                        isCurrent -> AccentGreen
                                        isPast -> TextMuted
                                        note.isRest -> TextMuted
                                        else -> TextPrimary
                                    }
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.sing_note_duration, note.durationSec),
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Current note (big display) ──
        val currentNote = if (state.currentIndex < score.notes.size) score.notes[state.currentIndex] else null

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            if (currentNote != null) {
                Text(
                    if (currentNote.isRest) "🤫" else "🎤",
                    fontSize = 48.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (currentNote.isRest) stringResource(R.string.sing_rest_long) else noteDisplay(currentNote, useJianpu),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    color = if (currentNote.isRest) TextMuted else AccentCyan
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (currentNote.isRest)
                        stringResource(R.string.sing_silent, currentNote.durationSec)
                    else
                        stringResource(R.string.sing_target_freq, currentNote.frequency.toInt()),
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                // Note name subtitle in jianpu mode
                if (useJianpu && !currentNote.isRest) {
                    Text(
                        text = currentNote.noteName,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Your pitch
            if (state.currentFreq > 0f) {
                Text(
                    stringResource(R.string.sing_your_pitch, state.currentNoteName, state.currentFreq.toInt()),
                    fontSize = 18.sp,
                    color = AccentGreen
                )
            } else {
                Text(stringResource(R.string.sing_waiting_sound), fontSize = 16.sp, color = TextMuted)
            }

            // Live feedback
            state.currentResult?.let { result ->
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            when (result.verdict) {
                                Verdict.PERFECT -> AccentGreen.copy(alpha = 0.2f)
                                Verdict.GOOD -> AccentYellow.copy(alpha = 0.2f)
                                else -> AccentRed.copy(alpha = 0.15f)
                            }
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        when (result.verdict) {
                            Verdict.PERFECT -> stringResource(R.string.sing_perfect)
                            Verdict.GOOD -> stringResource(R.string.sing_good)
                            Verdict.FLAT -> stringResource(R.string.sing_flat_feedback)
                            Verdict.SHARP -> stringResource(R.string.sing_sharp_feedback)
                            else -> "..."
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (result.verdict) {
                            Verdict.PERFECT -> AccentGreen
                            Verdict.GOOD -> AccentYellow
                            else -> AccentRed
                        }
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Progress bar
            LinearProgressIndicator(
                progress = (state.currentIndex + 1).toFloat() / score.notes.size,
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = AccentCyan,
                trackColor = CardDark
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.sing_progress, state.currentIndex + 1, score.notes.size),
                fontSize = 12.sp,
                color = TextMuted
            )

            Spacer(Modifier.height(16.dp))

            // Stop button
            OutlinedButton(
                onClick = { viewModel.stopPractice() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed)
            ) {
                Icon(Icons.Default.Stop, null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.tuner_stop))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── REVIEW ───

@Composable
private fun ReviewPhase(state: SingPracticeState, viewModel: SingPracticeViewModel, useJianpu: Boolean) {
    val stats = state.stats ?: return
    val score = state.score ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if ((stats.accuracy) >= 80f) "🌟" else if ((stats.accuracy) >= 60f) "👍" else "💪",
            fontSize = 64.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.sing_complete), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(4.dp))

        // Accuracy ring
        Text(
            "${"%.0f".format(stats.accuracy)}%",
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = when {
                stats.accuracy >= 80f -> AccentGreen
                stats.accuracy >= 60f -> AccentYellow
                else -> AccentRed
            }
        )
        Text(stringResource(R.string.sing_accuracy), fontSize = 14.sp, color = TextSecondary)

        Spacer(Modifier.height(24.dp))

        // Stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(stringResource(R.string.sing_stat_perfect), stats.perfect, AccentGreen)
            StatItem(stringResource(R.string.sing_stat_good), stats.good, AccentYellow)
            StatItem(stringResource(R.string.sing_stat_flat), stats.flat, AccentRed.copy(alpha = 0.8f))
            StatItem(stringResource(R.string.sing_stat_sharp), stats.sharp, AccentRed.copy(alpha = 0.8f))
            StatItem(stringResource(R.string.sing_stat_missed), stats.missed, TextMuted)
        }

        Spacer(Modifier.height(24.dp))

        // Detailed results
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.sing_detail_title), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))

                state.results.forEachIndexed { index, result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Note indicator
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    when (result.verdict) {
                                        Verdict.PERFECT -> AccentGreen.copy(alpha = 0.3f)
                                        Verdict.GOOD -> AccentYellow.copy(alpha = 0.3f)
                                        Verdict.MISSED -> AccentRed.copy(alpha = 0.2f)
                                        Verdict.CORRECT_REST -> TextMuted.copy(alpha = 0.2f)
                                        else -> AccentRed.copy(alpha = 0.15f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                when (result.verdict) {
                                    Verdict.PERFECT -> "✓"
                                    Verdict.GOOD -> "~"
                                    Verdict.MISSED -> "✗"
                                    Verdict.CORRECT_REST -> "—"
                                    else -> "?"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (result.verdict) {
                                    Verdict.PERFECT -> AccentGreen
                                    Verdict.GOOD -> AccentYellow
                                    else -> AccentRed
                                }
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                if (result.note.isRest) stringResource(R.string.sing_rest_long) else noteDisplay(result.note, useJianpu),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            if (!result.note.isRest && result.sungNoteName != null) {
                                Text(
                                    stringResource(R.string.sing_sang_note, result.sungNoteName, (result.centsOff ?: 0f).toInt()),
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Text(
                            when (result.verdict) {
                                Verdict.PERFECT -> stringResource(R.string.sing_stat_perfect)
                                Verdict.GOOD -> stringResource(R.string.sing_stat_good)
                                Verdict.FLAT -> stringResource(R.string.sing_stat_flat)
                                Verdict.SHARP -> stringResource(R.string.sing_stat_sharp)
                                Verdict.MISSED -> stringResource(R.string.sing_not_sang)
                                Verdict.CORRECT_REST -> stringResource(R.string.sing_rest_label)
                                else -> ""
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when (result.verdict) {
                                Verdict.PERFECT -> AccentGreen
                                Verdict.GOOD -> AccentYellow
                                else -> AccentRed
                            }
                        )
                    }
                    if (index < state.results.size - 1) {
                        Divider(color = TextMuted.copy(alpha = 0.1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.reset() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.sing_new_song))
            }
            Button(
                onClick = { viewModel.startPractice() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                Text(stringResource(R.string.sing_retry))
            }
        }
    }
}

// ─── Helpers ───

@Composable
private fun StatItem(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun SuggestionChipSmall(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = CardDark
    ) {
        Text(text, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = CardDark,
    unfocusedContainerColor = CardDark,
    focusedBorderColor = AccentBlue,
    unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = AccentBlue,
    unfocusedLabelColor = TextSecondary,
    cursorColor = AccentBlue
)

/** Show note as jianpu or standard name based on toggle. */
private fun noteDisplay(note: ScoreNote, useJianpu: Boolean): String =
    if (useJianpu) JianpuConverter.toJianpu(note.noteName) else note.noteName
