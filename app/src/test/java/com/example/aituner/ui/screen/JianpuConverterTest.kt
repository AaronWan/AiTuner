package com.example.aituner.ui.screen

import org.junit.Assert.*
import org.junit.Test

class JianpuConverterTest {

    @Test
    fun toJianpu_C4_returns1() {
        assertEquals("1", JianpuConverter.toJianpu("C4"))
    }

    @Test
    fun toJianpu_D4_returns2() {
        assertEquals("2", JianpuConverter.toJianpu("D4"))
    }

    @Test
    fun toJianpu_E4_returns3() {
        assertEquals("3", JianpuConverter.toJianpu("E4"))
    }

    @Test
    fun toJianpu_F4_returns4() {
        assertEquals("4", JianpuConverter.toJianpu("F4"))
    }

    @Test
    fun toJianpu_G4_returns5() {
        assertEquals("5", JianpuConverter.toJianpu("G4"))
    }

    @Test
    fun toJianpu_A4_returns6() {
        assertEquals("6", JianpuConverter.toJianpu("A4"))
    }

    @Test
    fun toJianpu_B4_returns7() {
        assertEquals("7", JianpuConverter.toJianpu("B4"))
    }

    @Test
    fun toJianpu_CSharp4_returns1Sharp() {
        assertEquals("1#", JianpuConverter.toJianpu("C#4"))
    }

    @Test
    fun toJianpu_rest_returns0() {
        assertEquals("0", JianpuConverter.toJianpu("-"))
    }

    @Test
    fun toJianpu_blank_returns0() {
        assertEquals("0", JianpuConverter.toJianpu(""))
    }

    @Test
    fun toJianpu_octaveUp_C5_hasDotAbove() {
        val result = JianpuConverter.toJianpu("C5")
        assertTrue("Expected dot above for C5, got '$result'", result.startsWith("1"))
        assertTrue("Expected dot above for C5, got '$result'", result.contains("\u0307"))
    }

    @Test
    fun toJianpu_octaveDown_C3_hasDotBelow() {
        val result = JianpuConverter.toJianpu("C3")
        assertTrue("Expected dot below for C3, got '$result'", result.startsWith("1"))
        assertTrue("Expected dot below for C3, got '$result'", result.contains("\u0323"))
    }

    @Test
    fun toJianpu_invalidNote_returnsInput() {
        assertEquals("X9", JianpuConverter.toJianpu("X9"))
    }
}
