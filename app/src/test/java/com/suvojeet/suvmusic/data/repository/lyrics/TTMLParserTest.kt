package com.suvojeet.suvmusic.data.repository.lyrics

import org.junit.Assert.*
import org.junit.Test

class TTMLParserTest {

    @Test
    fun parseTime_validTime_returnsSeconds() {
        val ttml = """
            <ttxml>
                <body>
                    <p begin="00:01:30">Line 1</p>
                </body>
            </ttxml>
        """.trimIndent()
        
        val lines = TTMLParser.parseTTML(ttml)
        assertEquals(90.0, lines[0].startTime, 0.01)
    }

    @Test
    fun parseTime_malformedTime_returnsZeroInsteadOfCrash() {
        val ttml = """
            <ttxml>
                <body>
                    <p begin="00:xx:30">Line 1</p>
                </body>
            </ttxml>
        """.trimIndent()
        
        // This should not crash
        val lines = TTMLParser.parseTTML(ttml)
        
        assertEquals(1, lines.size)
        // Should default to 0.0
        assertEquals(0.0, lines[0].startTime, 0.01)
    }

    @Test
    fun parseTime_emptyTime_returnsZero() {
        val ttml = """
            <ttxml>
                <body>
                    <p begin="">Line 1</p>
                </body>
            </ttxml>
        """.trimIndent()
        
        val lines = TTMLParser.parseTTML(ttml)
        assertEquals(0, lines.size) // Empty time causes continue
    }
}
