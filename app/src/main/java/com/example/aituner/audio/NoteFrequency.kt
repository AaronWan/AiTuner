package com.example.aituner.audio

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Maps frequencies to musical notes and calculates cent deviation.
 *
 * Reference: A4 = 440 Hz
 * n = 12 * log2(f / 440)  =>  MIDI note number
 */
object NoteFrequency {

    // Standard tuning frequencies for guitar (6 strings, E2 A2 D3 G3 B3 E4)
    val GUITAR_STANDARD = listOf("E2", "A2", "D3", "G3", "B3", "E4")
        .map { parseNote(it) }

    // Open string names
    val STRING_NAMES = listOf("6弦 (E)", "5弦 (A)", "4弦 (D)", "3弦 (G)", "2弦 (B)", "1弦 (E)")

    data class TuningNote(
        val noteName: String,     // e.g. "E2", "A#4"
        val frequency: Float,     // exact frequency
        val midiNumber: Float     // MIDI note number
    )

    data class PitchResult(
        val frequency: Float,              // detected frequency
        val closestNote: TuningNote,       // nearest musical note
        val centsOffset: Float,            // deviation in cents (-50 to +50)
        val confidence: Float = 0f         // 0..1 confidence score
    )

    /** Parse a note string like "A4" or "C#5" to frequency */
    fun parseNote(note: String): TuningNote {
        val regex = Regex("^([A-G])(#?)(\\d+)$")
        val (name, sharp, octave) = regex.find(note)?.destructured
            ?: throw IllegalArgumentException("Invalid note: $note")

        val semitone = noteToSemitone(name[0], sharp.isNotEmpty())
        val midi = semitone + (octave.toInt() + 1) * 12
        val freq = midiToFrequency(midi.toFloat())
        return TuningNote("$name$sharp$octave", freq, midi.toFloat())
    }

    /** Find the closest musical note to a given frequency */
    fun findClosestNote(frequency: Float): PitchResult {
        val midi = frequencyToMidi(frequency)
        val roundedMidi = midi.roundToInt()
        val cents = (midi - roundedMidi) * 100f  // cents deviation

        val octave = (roundedMidi / 12) - 1
        val noteIndex = roundedMidi % 12
        val (noteName, _) = semitoneToNote(noteIndex)

        val exactFreq = midiToFrequency(roundedMidi.toFloat())
        val note = TuningNote("$noteName$octave", exactFreq, roundedMidi.toFloat())

        return PitchResult(frequency, note, cents.coerceIn(-50f, 50f))
    }

    /** Calculate how many cents a detected frequency is from a target note */
    fun centsFromTarget(frequency: Float, target: TuningNote): Float {
        val diff = 1200f * (ln(frequency / target.frequency) / ln(2f))
        return diff.coerceIn(-50f, 50f)
    }

    private fun noteToSemitone(name: Char, sharp: Boolean): Int = when (name) {
        'C' -> if (sharp) 1 else 0
        'D' -> if (sharp) 3 else 2
        'E' -> 4  // no E#
        'F' -> if (sharp) 6 else 5
        'G' -> if (sharp) 8 else 7
        'A' -> if (sharp) 10 else 9
        'B' -> 11 // no B#
        else -> throw IllegalArgumentException("Invalid note name: $name")
    }

    private fun semitoneToNote(semitone: Int): Pair<String, Int> = when (semitone % 12) {
        0 -> "C" to 0
        1 -> "C#" to 1
        2 -> "D" to 2
        3 -> "D#" to 3
        4 -> "E" to 4
        5 -> "F" to 5
        6 -> "F#" to 6
        7 -> "G" to 7
        8 -> "G#" to 8
        9 -> "A" to 9
        10 -> "A#" to 10
        11 -> "B" to 11
        else -> "?" to 0
    }

    private fun midiToFrequency(midi: Float): Float =
        440f * 2f.pow((midi - 69f) / 12f)

    private fun frequencyToMidi(freq: Float): Float =
        69f + 12f * (ln(freq / 440f) / ln(2f))
}
