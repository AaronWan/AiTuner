package com.example.aituner.ui.screen

import com.example.aituner.audio.NoteFrequency
import kotlin.math.abs

/**
 * A musical score for singing practice.
 */
data class Score(
    val title: String,
    val notes: List<ScoreNote>
)

data class ScoreNote(
    val noteName: String,       // "C4", "E4", "-" for rest
    val frequency: Float,       // 0 for rest
    val durationSec: Float,     // how long to hold this note
    val syllable: String = ""   // optional lyric syllable
) {
    val isRest: Boolean get() = noteName == "-"
}

/**
 * Result of comparing a sung note against the score.
 */
data class ScoringResult(
    val note: ScoreNote,
    val sungFrequency: Float? = null,   // null = silence/miss
    val sungNoteName: String? = null,
    val centsOff: Float? = null,        // null for rest/silence
    val verdict: Verdict
)

enum class Verdict {
    PERFECT,     // within ±10 cents
    GOOD,        // within ±25 cents
    FLAT,        // too low > 25 cents
    SHARP,       // too high > 25 cents
    MISSED,      // didn't sing / silence
    CORRECT_REST, // correctly silent on a rest
    PENDING      // not yet evaluated
}

/**
 * Parse a simple text score from AI output.
 *
 * Format: Comma-separated notes, hyphens for rests
 * Examples:
 *   "C4, D4, E4, F4, G4, -, -, G4"      (simple)
 *   "C4:2, E4:1, G4:3"                   (note:seconds)
 */
object ScoreParser {

    fun parse(raw: String, title: String = "AI 乐谱"): Score {
        val clean = raw
            .replace(Regex("```[\\s\\S]*?```"), "")
            .replace(Regex("乐谱[:：]\\s*"), "")
            .replace(Regex("标题[:：]\\s*"), "")
            .trim()

        // Split by common delimiters
        val tokens = clean
            .split(Regex("[,，\\s\\n]+"))
            .filter { it.isNotBlank() }

        val notes = mutableListOf<ScoreNote>()
        for (token in tokens) {
            // Check for duration suffix ":seconds"
            val parts = token.split(":")
            val noteStr = parts[0].trim()
            val duration = if (parts.size > 1) parts[1].trim().toFloatOrNull() ?: 1f else 1f

            if (noteStr == "-" || noteStr == "—") {
                notes.add(ScoreNote(noteName = "-", frequency = 0f, durationSec = duration))
                continue
            }

            try {
                val tuningNote = NoteFrequency.parseNote(noteStr)
                notes.add(
                    ScoreNote(
                        noteName = noteStr,
                        frequency = tuningNote.frequency,
                        durationSec = duration
                    )
                )
            } catch (_: Exception) {
                // Skip unparseable tokens
            }
        }

        return Score(title, notes)
    }
}

/**
 * Compare a sung frequency against a target note.
 */
fun evaluateNote(sungFreq: Float?, target: ScoreNote): ScoringResult {
    return when {
        target.isRest && sungFreq == null ->
            ScoringResult(target, verdict = Verdict.CORRECT_REST)

        target.isRest && sungFreq != null -> {
            val detected = NoteFrequency.findClosestNote(sungFreq)
            ScoringResult(
                target, sungFreq,
                detected.closestNote.noteName,
                detected.centsOffset,
                Verdict.SHARP // singing on a rest
            )
        }

        sungFreq == null ->
            ScoringResult(target, verdict = Verdict.MISSED)

        else -> {
            val offset = NoteFrequency.centsFromTarget(sungFreq, target.let {
                NoteFrequency.TuningNote(target.noteName, target.frequency, 0f)
            })
            val absOff = abs(offset)
            val verdict = when {
                absOff <= 10f -> Verdict.PERFECT
                absOff <= 25f -> Verdict.GOOD
                offset < 0 -> Verdict.FLAT
                else -> Verdict.SHARP
            }
            val detected = NoteFrequency.findClosestNote(sungFreq)
            ScoringResult(target, sungFreq, detected.closestNote.noteName, offset, verdict)
        }
    }
}
