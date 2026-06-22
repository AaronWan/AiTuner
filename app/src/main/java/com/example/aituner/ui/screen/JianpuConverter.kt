package com.example.aituner.ui.screen

import com.example.aituner.audio.NoteFrequency

/**
 * Convert between note names (C4, D#4, ...) and jianpu numbered notation (1, 2, ...).
 *
 * Jianpu (简谱) uses numbers 1-7 for the scale degrees:
 *   1=Do  2=Re  3=Mi  4=Fa  5=Sol  6=La  7=Si
 *
 * Octave is indicated by dots:
 *   C4 = "1"   (middle, no dot)
 *   C5 = "1̇"   (higher, dot above)
 *   C3 = "1̣"   (lower, dot below)
 */
object JianpuConverter {

    /**
     * Convert note name "C#4" → jianpu "4̇#"
     */
    fun toJianpu(noteName: String): String {
        if (noteName == "-" || noteName.isBlank()) return "0" // rest

        try {
            val note = NoteFrequency.parseNote(noteName)
            val midiNum = note.midiNumber.toInt()

            val semitone = midiNum % 12
            val octave = (midiNum / 12) - 1

            val (base, hasSharp) = semitoneToScale(semitone)
            val number = if (hasSharp) "$base#" else base
            val middleOctave = 4

            return when {
                octave > middleOctave -> {
                    val dots = "̇".repeat(octave - middleOctave)
                    "$number$dots"
                }
                octave < middleOctave -> {
                    val dots = "̣".repeat(middleOctave - octave)
                    "$number$dots"
                }
                else -> number
            }
        } catch (_: Exception) {
            return noteName
        }
    }

    private fun semitoneToScale(semitone: Int): Pair<String, Boolean> = when (semitone) {
        0 -> "1" to false   // C
        1 -> "1" to true    // C#
        2 -> "2" to false   // D
        3 -> "2" to true    // D#
        4 -> "3" to false   // E
        5 -> "4" to false   // F
        6 -> "4" to true    // F#
        7 -> "5" to false   // G
        8 -> "5" to true    // G#
        9 -> "6" to false   // A
        10 -> "6" to true   // A#
        11 -> "7" to false  // B
        else -> "?" to false
    }
}
