package com.example.aituner.ui.screen

import com.example.aituner.audio.NoteFrequency
import org.junit.Assert.*
import org.junit.Test

class ScoreModelTest {

    // ─── ScoreParser ───

    @Test
    fun parse_simpleNotes() {
        val score = ScoreParser.parse("C4, D4, E4", "Test")
        assertEquals("Test", score.title)
        assertEquals(3, score.notes.size)
        assertEquals("C4", score.notes[0].noteName)
        assertEquals("D4", score.notes[1].noteName)
        assertEquals("E4", score.notes[2].noteName)
    }

    @Test
    fun parse_withRest() {
        val score = ScoreParser.parse("C4, -, E4")
        assertEquals(3, score.notes.size)
        assertTrue(score.notes[1].isRest)
        assertEquals(0f, score.notes[1].frequency)
    }

    @Test
    fun parse_withDuration() {
        val score = ScoreParser.parse("C4:2, D4:1")
        assertEquals(2, score.notes.size)
        assertEquals(2f, score.notes[0].durationSec)
        assertEquals(1f, score.notes[1].durationSec)
    }

    @Test
    fun parse_defaultDuration() {
        val score = ScoreParser.parse("C4, D4")
        assertEquals(1f, score.notes[0].durationSec)
        assertEquals(1f, score.notes[1].durationSec)
    }

    @Test
    fun parse_chineseComma_delimiter() {
        val score = ScoreParser.parse("C4，D4，E4")
        assertEquals(3, score.notes.size)
    }

    @Test
    fun parse_skipMarkdownCodeBlock() {
        // code block content is stripped — notes inside are removed too
        val score = ScoreParser.parse("```\nC4, D4\n```")
        assertEquals(0, score.notes.size)
    }

    @Test
    fun parse_notesOutsideCodeBlock_parsed() {
        val score = ScoreParser.parse("```\ncode\n```\nC4, D4")
        assertEquals(2, score.notes.size)
    }

    @Test
    fun parse_skipJianpuLabel() {
        val score = ScoreParser.parse("乐谱：C4, D4, E4")
        assertEquals(3, score.notes.size)
    }

    @Test
    fun parse_emDashRest() {
        val score = ScoreParser.parse("C4, —, E4")
        assertTrue(score.notes[1].isRest)
    }

    @Test
    fun parse_unparseableToken_skipped() {
        val score = ScoreParser.parse("C4, INVALID123, E4")
        assertEquals(2, score.notes.size)
        assertEquals("C4", score.notes[0].noteName)
        assertEquals("E4", score.notes[1].noteName)
    }

    @Test
    fun parse_emptyInput_emptyScore() {
        val score = ScoreParser.parse("")
        assertEquals(0, score.notes.size)
    }

    // ─── evaluateNote ───

    @Test
    fun evaluateNote_perfect() {
        val target = ScoreNote("A4", 440f, 1f)
        val result = evaluateNote(440f, target)
        assertEquals(Verdict.PERFECT, result.verdict)
        assertEquals(0f, result.centsOff!!, 0.5f)
    }

    @Test
    fun evaluateNote_good_within25cents() {
        val target = ScoreNote("A4", 440f, 1f)
        val result = evaluateNote(445f, target)
        assertEquals(Verdict.GOOD, result.verdict)
    }

    @Test
    fun evaluateNote_flat() {
        val target = ScoreNote("A4", 440f, 1f)
        val result = evaluateNote(420f, target)
        assertEquals(Verdict.FLAT, result.verdict)
    }

    @Test
    fun evaluateNote_sharp() {
        val target = ScoreNote("A4", 440f, 1f)
        val result = evaluateNote(465f, target)
        assertEquals(Verdict.SHARP, result.verdict)
    }

    @Test
    fun evaluateNote_missed() {
        val target = ScoreNote("A4", 440f, 1f)
        val result = evaluateNote(null, target)
        assertEquals(Verdict.MISSED, result.verdict)
    }

    @Test
    fun evaluateNote_correctRest() {
        val target = ScoreNote("-", 0f, 1f)
        val result = evaluateNote(null, target)
        assertEquals(Verdict.CORRECT_REST, result.verdict)
    }

    @Test
    fun evaluateNote_singingOnRest() {
        val target = ScoreNote("-", 0f, 1f)
        val result = evaluateNote(440f, target)
        assertEquals(Verdict.SHARP, result.verdict)
    }
}
