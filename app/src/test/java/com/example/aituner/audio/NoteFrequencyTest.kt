package com.example.aituner.audio

import org.junit.Assert.*
import org.junit.Test

class NoteFrequencyTest {

    // ─── parseNote ───

    @Test
    fun parseNote_A4_returns440Hz() {
        val result = NoteFrequency.parseNote("A4")
        assertEquals("A4", result.noteName)
        assertEquals(440f, result.frequency)
        assertEquals(69f, result.midiNumber)
    }

    @Test
    fun parseNote_C4_returnsAround261Hz() {
        val result = NoteFrequency.parseNote("C4")
        assertEquals("C4", result.noteName)
        assertEquals(261.62558f, result.frequency, 0.01f)
    }

    @Test
    fun parseNote_sharpNote_CSharp4() {
        val result = NoteFrequency.parseNote("C#4")
        assertEquals("C#4", result.noteName)
        assertEquals(277.18265f, result.frequency, 0.01f)
    }

    @Test
    fun parseNote_lowString_E2() {
        val result = NoteFrequency.parseNote("E2")
        assertEquals("E2", result.noteName)
        assertEquals(82.4069f, result.frequency, 0.01f)
    }

    @Test
    fun parseNote_highNote_B4() {
        val result = NoteFrequency.parseNote("B4")
        assertEquals("B4", result.noteName)
        assertEquals(493.8833f, result.frequency, 0.01f)
    }

    @Test
    fun parseNote_G3() {
        val result = NoteFrequency.parseNote("G3")
        assertEquals("G3", result.noteName)
        assertEquals(195.99773f, result.frequency, 0.01f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseNote_invalidNote_throwsException() {
        NoteFrequency.parseNote("H3")
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseNote_emptyString_throwsException() {
        NoteFrequency.parseNote("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseNote_garbageInput_throwsException() {
        NoteFrequency.parseNote("not-a-note")
    }

    // ─── findClosestNote ───

    @Test
    fun findClosestNote_exactA440() {
        val result = NoteFrequency.findClosestNote(440f)
        assertEquals("A4", result.closestNote.noteName)
        assertEquals(440f, result.closestNote.frequency)
        assertEquals(0f, result.centsOffset, 0.1f)
    }

    @Test
    fun findClosestNote_445Hz_stillA4_positiveCents() {
        val result = NoteFrequency.findClosestNote(445f)
        assertEquals("A4", result.closestNote.noteName)
        // 445 Hz from 440 Hz → 1200 * log2(445/440) ≈ 19.6 cents
        assertEquals(19.56f, result.centsOffset, 0.5f)
    }

    @Test
    fun findClosestNote_435Hz_stillA4_negativeCents() {
        val result = NoteFrequency.findClosestNote(435f)
        assertEquals("A4", result.closestNote.noteName)
        assertEquals(-19.78f, result.centsOffset, 0.5f)
    }

    @Test
    fun findClosestNote_guitarLowE() {
        // E2 ≈ 82.4 Hz
        val result = NoteFrequency.findClosestNote(82.41f)
        assertEquals("E2", result.closestNote.noteName)
        assertEquals(0f, result.centsOffset, 0.2f)
    }

    @Test
    fun findClosestNote_guitarHighE() {
        // E4 ≈ 329.6 Hz
        val result = NoteFrequency.findClosestNote(329.63f)
        assertEquals("E4", result.closestNote.noteName)
        assertEquals(0f, result.centsOffset, 0.2f)
    }

    @Test
    fun findClosestNote_centsCoercedTo50() {
        // A frequency way off from any note should be coerced to ±50 cents
        val result = NoteFrequency.findClosestNote(450f)
        // 450 Hz from A4 (440): 1200 * log2(450/440) ≈ 38.9 cents, should not exceed 50
        assertTrue(result.centsOffset >= -50f)
        assertTrue(result.centsOffset <= 50f)
    }

    @Test
    fun findClosestNote_C4() {
        val result = NoteFrequency.findClosestNote(261.63f)
        assertEquals("C4", result.closestNote.noteName)
        assertEquals(0f, result.centsOffset, 0.2f)
    }

    // ─── centsFromTarget ───

    @Test
    fun centsFromTarget_exactMatch_returnsZero() {
        val target = NoteFrequency.parseNote("A4")
        val cents = NoteFrequency.centsFromTarget(440f, target)
        assertEquals(0f, cents, 0.1f)
    }

    @Test
    fun centsFromTarget_sharp() {
        val target = NoteFrequency.parseNote("A4")
        // One semitone up from A4 (≈ 466.16 Hz) should be +100 cents
        val cents = NoteFrequency.centsFromTarget(466.16f, target)
        assertEquals(50f, cents, 1f) // coerced to 50
    }

    @Test
    fun centsFromTarget_flat() {
        val target = NoteFrequency.parseNote("A4")
        // One semitone down from A4 (≈ 415.30 Hz) should be -100 cents
        val cents = NoteFrequency.centsFromTarget(415.30f, target)
        assertEquals(-50f, cents, 1f) // coerced to 50
    }

    // ─── GUITAR_STANDARD ───

    @Test
    fun guitarStandard_hasSixStrings() {
        assertEquals(6, NoteFrequency.GUITAR_STANDARD.size)
    }

    @Test
    fun guitarStandard_stringsInOrder() {
        val strings = NoteFrequency.GUITAR_STANDARD
        assertEquals("E2", strings[0].noteName)
        assertEquals("A2", strings[1].noteName)
        assertEquals("D3", strings[2].noteName)
        assertEquals("G3", strings[3].noteName)
        assertEquals("B3", strings[4].noteName)
        assertEquals("E4", strings[5].noteName)
    }

    @Test
    fun stringNames_matchesCount() {
        assertEquals(6, NoteFrequency.STRING_NAMES.size)
    }
}
