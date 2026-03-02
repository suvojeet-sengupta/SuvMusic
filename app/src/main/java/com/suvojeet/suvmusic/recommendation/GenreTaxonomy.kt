package com.suvojeet.suvmusic.recommendation

/**
 * Defines a fixed genre taxonomy with 20 base genres.
 * Each genre maps to an integer index (0–19) used in genre affinity vectors.
 *
 * Genre vectors are FloatArray(20) where each dimension represents
 * the affinity/probability for that genre.
 */
object GenreTaxonomy {

    const val GENRE_COUNT = 20

    /** Ordered genre labels — index matches the vector dimension */
    val GENRES = arrayOf(
        "pop",          // 0
        "rock",         // 1
        "hip-hop",      // 2
        "electronic",   // 3
        "classical",    // 4
        "jazz",         // 5
        "r&b",          // 6
        "metal",        // 7
        "folk",         // 8
        "country",      // 9
        "reggae",       // 10
        "latin",        // 11
        "bollywood",    // 12
        "k-pop",        // 13
        "indie",        // 14
        "punk",         // 15
        "blues",        // 16
        "soul",         // 17
        "ambient",      // 18
        "lo-fi"         // 19
    )

    /** Index lookup by genre name */
    private val INDEX_MAP: Map<String, Int> = GENRES.withIndex().associate { (i, g) -> g to i }

    /**
     * Keyword → genre index mapping for lightweight title/artist-based genre inference.
     * Each keyword maps to a primary genre index and an optional secondary index with a lower weight.
     */
    private val KEYWORD_GENRE_MAP: Map<String, List<Pair<Int, Float>>> = buildMap {
        // Pop
        put("pop", listOf(0 to 1.0f))
        put("mainstream", listOf(0 to 0.8f))
        put("chart", listOf(0 to 0.6f))

        // Rock
        put("rock", listOf(1 to 1.0f))
        put("guitar", listOf(1 to 0.6f))
        put("grunge", listOf(1 to 0.8f, 7 to 0.3f))
        put("alternative", listOf(1 to 0.7f, 14 to 0.5f))

        // Hip-Hop / Rap
        put("hip-hop", listOf(2 to 1.0f))
        put("hiphop", listOf(2 to 1.0f))
        put("rap", listOf(2 to 1.0f))
        put("trap", listOf(2 to 0.9f, 3 to 0.2f))
        put("drill", listOf(2 to 0.9f))
        put("boom bap", listOf(2 to 0.8f))

        // Electronic / EDM
        put("electronic", listOf(3 to 1.0f))
        put("edm", listOf(3 to 1.0f))
        put("techno", listOf(3 to 0.9f))
        put("house", listOf(3 to 0.9f))
        put("dubstep", listOf(3 to 0.9f))
        put("trance", listOf(3 to 0.8f))
        put("drum and bass", listOf(3 to 0.8f))
        put("dnb", listOf(3 to 0.8f))
        put("synth", listOf(3 to 0.7f))

        // Classical
        put("classical", listOf(4 to 1.0f))
        put("orchestra", listOf(4 to 0.9f))
        put("symphony", listOf(4 to 0.9f))
        put("piano", listOf(4 to 0.6f, 5 to 0.3f))
        put("concerto", listOf(4 to 0.9f))
        put("sonata", listOf(4 to 0.9f))
        put("opera", listOf(4 to 0.8f))

        // Jazz
        put("jazz", listOf(5 to 1.0f))
        put("swing", listOf(5 to 0.8f))
        put("bebop", listOf(5 to 0.9f))
        put("smooth jazz", listOf(5 to 0.8f))
        put("saxophone", listOf(5 to 0.6f))

        // R&B / Soul
        put("r&b", listOf(6 to 1.0f))
        put("rnb", listOf(6 to 1.0f))
        put("rhythm and blues", listOf(6 to 1.0f))

        // Metal
        put("metal", listOf(7 to 1.0f))
        put("heavy metal", listOf(7 to 1.0f))
        put("death metal", listOf(7 to 0.9f))
        put("black metal", listOf(7 to 0.9f))
        put("thrash", listOf(7 to 0.8f, 1 to 0.2f))
        put("metalcore", listOf(7 to 0.8f, 15 to 0.3f))

        // Folk
        put("folk", listOf(8 to 1.0f))
        put("acoustic", listOf(8 to 0.6f, 14 to 0.3f))
        put("singer-songwriter", listOf(8 to 0.5f, 14 to 0.5f))

        // Country
        put("country", listOf(9 to 1.0f))
        put("bluegrass", listOf(9 to 0.8f, 8 to 0.3f))
        put("nashville", listOf(9 to 0.7f))

        // Reggae
        put("reggae", listOf(10 to 1.0f))
        put("dancehall", listOf(10 to 0.8f))
        put("dub", listOf(10 to 0.7f, 3 to 0.3f))
        put("ska", listOf(10 to 0.7f, 15 to 0.2f))

        // Latin
        put("latin", listOf(11 to 1.0f))
        put("reggaeton", listOf(11 to 0.9f, 2 to 0.2f))
        put("salsa", listOf(11 to 0.8f))
        put("bossa nova", listOf(11 to 0.7f, 5 to 0.3f))
        put("bachata", listOf(11 to 0.8f))
        put("cumbia", listOf(11 to 0.8f))

        // Bollywood / Indian
        put("bollywood", listOf(12 to 1.0f))
        put("bollywood song", listOf(12 to 1.0f))
        put("hindi", listOf(12 to 0.8f))
        put("punjabi", listOf(12 to 0.7f))
        put("tamil", listOf(12 to 0.6f))
        put("telugu", listOf(12 to 0.6f))
        put("bengali", listOf(12 to 0.6f))
        put("sufi", listOf(12 to 0.5f, 8 to 0.3f))
        put("ghazal", listOf(12 to 0.5f, 4 to 0.2f))
        put("qawwali", listOf(12 to 0.6f))
        put("a.r. rahman", listOf(12 to 0.8f))
        put("arijit", listOf(12 to 0.7f))

        // K-Pop
        put("k-pop", listOf(13 to 1.0f))
        put("kpop", listOf(13 to 1.0f))
        put("korean", listOf(13 to 0.7f))
        put("j-pop", listOf(13 to 0.5f, 0 to 0.5f))

        // Indie
        put("indie", listOf(14 to 1.0f))
        put("indie rock", listOf(14 to 0.7f, 1 to 0.4f))
        put("indie pop", listOf(14 to 0.7f, 0 to 0.4f))
        put("shoegaze", listOf(14 to 0.7f, 1 to 0.3f))
        put("dream pop", listOf(14 to 0.6f, 0 to 0.3f, 18 to 0.2f))

        // Punk
        put("punk", listOf(15 to 1.0f))
        put("punk rock", listOf(15 to 0.8f, 1 to 0.3f))
        put("pop punk", listOf(15 to 0.6f, 0 to 0.4f))
        put("hardcore", listOf(15 to 0.8f, 7 to 0.3f))
        put("emo", listOf(15 to 0.6f, 1 to 0.3f))

        // Blues
        put("blues", listOf(16 to 1.0f))
        put("delta blues", listOf(16 to 0.9f))
        put("blues rock", listOf(16 to 0.6f, 1 to 0.4f))

        // Soul
        put("soul", listOf(17 to 1.0f))
        put("neo soul", listOf(17 to 0.8f, 6 to 0.3f))
        put("motown", listOf(17 to 0.8f))
        put("funk", listOf(17 to 0.6f, 6 to 0.3f))
        put("gospel", listOf(17 to 0.5f))

        // Ambient
        put("ambient", listOf(18 to 1.0f))
        put("new age", listOf(18 to 0.8f))
        put("meditation", listOf(18 to 0.8f))
        put("drone", listOf(18 to 0.7f))
        put("sleep", listOf(18 to 0.6f, 19 to 0.3f))

        // Lo-Fi
        put("lo-fi", listOf(19 to 1.0f))
        put("lofi", listOf(19 to 1.0f))
        put("chillhop", listOf(19 to 0.8f, 2 to 0.2f))
        put("chill", listOf(19 to 0.5f, 18 to 0.3f))
        put("study", listOf(19 to 0.6f, 18 to 0.3f))
        put("relaxing", listOf(19 to 0.4f, 18 to 0.4f))
    }

    /**
     * Get the genre index for a named genre (case-insensitive).
     * @return index 0–19, or -1 if not found
     */
    fun indexOf(genre: String): Int = INDEX_MAP[genre.lowercase()] ?: -1

    /**
     * Infer a genre vector for a song from its title and artist name.
     * Uses keyword matching against the taxonomy.
     *
     * @return FloatArray(20) with genre probabilities, normalized to max 1.0
     */
    fun inferGenreVector(title: String, artist: String): FloatArray {
        val vector = FloatArray(GENRE_COUNT)
        val text = "$title $artist".lowercase()

        // Match longer keyword phrases first (sorted by descending length)
        val sortedKeywords = KEYWORD_GENRE_MAP.keys.sortedByDescending { it.length }

        var matchCount = 0
        for (keyword in sortedKeywords) {
            if (keyword in text) {
                val genreWeights = KEYWORD_GENRE_MAP[keyword] ?: continue
                for ((genreIndex, weight) in genreWeights) {
                    vector[genreIndex] = maxOf(vector[genreIndex], weight)
                }
                matchCount++
                if (matchCount >= 5) break // Limit to top 5 keyword matches
            }
        }

        // Normalize: if there are any scores, ensure max is 1.0
        val maxVal = vector.max()
        if (maxVal > 0f) {
            for (i in vector.indices) {
                vector[i] /= maxVal
            }
        }

        return vector
    }

    /**
     * Check if a genre vector has meaningful data (any non-zero dimension).
     */
    fun isNonZero(vector: FloatArray): Boolean {
        return vector.any { it > 0f }
    }

    /**
     * Get the top N genres from a genre vector.
     * @return List of (genre name, weight) pairs sorted descending by weight.
     */
    fun topGenres(vector: FloatArray, n: Int = 3): List<Pair<String, Float>> {
        return vector.withIndex()
            .filter { it.value > 0f }
            .sortedByDescending { it.value }
            .take(n)
            .map { GENRES[it.index] to it.value }
    }
}
