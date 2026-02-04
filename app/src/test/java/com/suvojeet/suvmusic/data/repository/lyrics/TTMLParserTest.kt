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

    @Test
    fun parseTTML_withSpans_extractsWords() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttp="http://www.w3.org/ns/ttml#parameter" ttp:timeBase="media">
                <body>
                    <div>
                        <p begin="00:00:10.000" end="00:00:12.000">
                            <span begin="00:00:10.000" end="00:00:10.500">Word1</span>
                            <span> </span>
                            <span begin="00:00:10.500" end="00:00:11.000">Word2</span>
                        </p>
                    </div>
                </body>
            </tt>
        """.trimIndent()
        
        val lines = TTMLParser.parseTTML(ttml)
        
        assertEquals(1, lines.size)
        val line = lines[0]
        assertEquals("Word1 Word2", line.text)
        assertNotNull(line.words)
        assertEquals(2, line.words.size)
        assertEquals("Word1", line.words[0].text)
        assertEquals(10000L, line.words[0].startTimeMs)
        assertEquals("Word2", line.words[1].text)
        assertEquals(10500L, line.words[1].startTimeMs)
    }
}
