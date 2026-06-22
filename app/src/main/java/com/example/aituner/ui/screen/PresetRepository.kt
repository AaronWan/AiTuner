package com.example.aituner.ui.screen

import android.content.Context
import com.example.aituner.audio.NoteFrequency
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresetRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class PresetInfo(
        val id: String,
        val title: String
    )

    fun listPresets(): List<PresetInfo> {
        return try {
            context.assets.list(PRESETS_DIR)
                ?.filter { it.endsWith(".md") }
                ?.mapNotNull { filename ->
                    val text = context.assets.open("$PRESETS_DIR/$filename")
                        .bufferedReader().use { it.readText() }
                    val title = extractTitle(text) ?: filename.removeSuffix(".md")
                    PresetInfo(
                        id = filename.removeSuffix(".md"),
                        title = title
                    )
                }
                ?.sortedBy { it.id }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun loadPreset(presetId: String): Score? {
        return try {
            val text = context.assets.open("$PRESETS_DIR/$presetId.md")
                .bufferedReader().use { it.readText() }
            parsePreset(text)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractTitle(text: String): String? {
        return text.lines()
            .firstOrNull { it.startsWith("# ") }
            ?.removePrefix("# ")
            ?.trim()
    }

    private fun parsePreset(text: String): Score? {
        val title = extractTitle(text) ?: return null
        val notes = mutableListOf<ScoreNote>()

        for (line in text.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            val parts = trimmed.split(":")
            if (parts.size < 2) continue

            val noteName = parts[0].trim()
            val duration = parts[1].trim().toFloatOrNull() ?: 1f
            val syllable = parts.getOrElse(2) { "" }.trim()

            if (noteName == "-") {
                notes.add(ScoreNote("-", 0f, duration, syllable))
            } else {
                try {
                    val tuningNote = NoteFrequency.parseNote(noteName)
                    notes.add(ScoreNote(noteName, tuningNote.frequency, duration, syllable))
                } catch (_: Exception) {
                    // skip unparseable
                }
            }
        }

        return if (notes.isEmpty()) null else Score(title, notes)
    }

    companion object {
        private const val PRESETS_DIR = "presets"
    }
}
