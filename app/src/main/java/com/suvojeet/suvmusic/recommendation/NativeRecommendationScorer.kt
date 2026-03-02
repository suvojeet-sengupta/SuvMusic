package com.suvojeet.suvmusic.recommendation

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JNI bridge to the native SIMD-accelerated recommendation scoring engine.
 *
 * Uses ARM NEON (or x86 SSE) to batch-process candidate scoring in a single
 * native call, avoiding per-candidate JVM overhead. For 500 candidates this
 * reduces scoring from ~5–10ms (Kotlin loop) to <1ms (native SIMD).
 *
 * Falls back to Kotlin scoring if the native library fails to load.
 *
 * The native library (`suvmusic_native`) is shared with the audio DSP pipeline —
 * no additional .so file is needed.
 */
@Singleton
class NativeRecommendationScorer @Inject constructor() {

    companion object {
        private const val TAG = "NativeRecoScorer"

        /**
         * Number of scoring features per candidate (must match C++ NUM_FEATURES).
         * See recommendation_scorer.cpp for feature index documentation.
         */
        const val NUM_FEATURES = 11

        /** Number of weight values (must match C++ NUM_WEIGHTS) */
        const val NUM_WEIGHTS = 11
    }

    @Volatile
    private var isAvailable = false

    init {
        try {
            // Library is already loaded by NativeSpatialAudio — but ensure it's loaded
            System.loadLibrary("suvmusic_native")
            isAvailable = true
            Log.d(TAG, "Native recommendation scorer loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native recommendation scorer not available, will use Kotlin fallback", e)
            isAvailable = false
        }
    }

    /**
     * Whether the native scorer is available. If false, callers should
     * fall back to the Kotlin-based scoring loop.
     */
    fun isNativeAvailable(): Boolean = isAvailable

    /**
     * Score candidates using the native SIMD engine and return top-K indices.
     *
     * @param features Flat FloatArray in SoA (column-major) layout: [NUM_FEATURES × numCandidates].
     *   Feature at index `featureIndex * numCandidates + candidateIndex`.
     *
     *   Feature indices:
     *   - 0: artistAffinity (0.0–1.0)
     *   - 1: freshnessFlag (0.0 or 1.0)
     *   - 2: skipFlag (0.0 or 1.0)
     *   - 3: likedSongFlag (0.0 or 1.0)
     *   - 4: likedArtistFlag (0.0 or 1.0)
     *   - 5: timeOfDayWeight (0.0–1.0)
     *   - 6: varietyPenalty (0.0–N)
     *   - 7: genreSimilarity (0.0–1.0)
     *   - 8: recentGenreSimilarity (0.0–1.0)
     *   - 9: skipGenrePenalty (0.0–1.0)
     *   - 10: reserved (0.0)
     *
     * @param numCandidates Number of candidate songs
     * @param weights FloatArray(11) — scoring weights [base, artistAff, freshness, skipPenalty,
     *   likedSong, likedArtist, timeOfDay, varietyPen, genreSim, recentGenre, skipGenre]
     * @param topK How many top results to return
     * @return IntArray of candidate indices sorted by descending score, length = min(topK, numCandidates)
     */
    fun scoreCandidates(
        features: FloatArray,
        numCandidates: Int,
        weights: FloatArray,
        topK: Int
    ): IntArray? {
        if (!isAvailable) return null
        if (numCandidates <= 0) return IntArray(0)
        if (features.size < NUM_FEATURES * numCandidates) {
            Log.e(TAG, "Feature array too small: ${features.size} < ${NUM_FEATURES * numCandidates}")
            return null
        }
        if (weights.size < NUM_WEIGHTS) {
            Log.e(TAG, "Weights array too small: ${weights.size} < $NUM_WEIGHTS")
            return null
        }

        return try {
            nScoreCandidates(features, numCandidates, NUM_FEATURES, weights, topK)
        } catch (e: Exception) {
            Log.e(TAG, "Native scoring failed", e)
            null
        }
    }

    /**
     * Compute cosine similarity between two genre vectors using NEON/SSE.
     *
     * @return Cosine similarity in [0, 1], or 0 if native is unavailable.
     */
    fun cosineSimilarity(vecA: FloatArray, vecB: FloatArray): Float {
        if (!isAvailable) return cosineSimilarityFallback(vecA, vecB)
        if (vecA.size != vecB.size) return 0f

        return try {
            nCosineSimilarity(vecA, vecB, vecA.size)
        } catch (e: Exception) {
            cosineSimilarityFallback(vecA, vecB)
        }
    }

    /**
     * Batch compute cosine similarities between a user vector and N candidate vectors.
     * Single JNI call for all candidates — much faster than N individual calls.
     *
     * @param userVector FloatArray(dim) — the user's genre vector
     * @param candidateVectors FloatArray(N × dim) — packed candidate vectors
     * @param numCandidates N
     * @return FloatArray(N) of cosine similarities
     */
    fun batchCosineSimilarity(
        userVector: FloatArray,
        candidateVectors: FloatArray,
        numCandidates: Int
    ): FloatArray? {
        if (!isAvailable) return null
        if (numCandidates <= 0 || userVector.isEmpty()) return FloatArray(0)

        return try {
            nBatchCosineSimilarity(userVector, candidateVectors, numCandidates, userVector.size)
        } catch (e: Exception) {
            Log.e(TAG, "Batch cosine similarity failed", e)
            null
        }
    }

    /**
     * Kotlin fallback for cosine similarity when native is unavailable.
     */
    private fun cosineSimilarityFallback(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size || a.isEmpty()) return 0f
        var dot = 0f
        var magA = 0f
        var magB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            magA += a[i] * a[i]
            magB += b[i] * b[i]
        }
        val denom = kotlin.math.sqrt(magA) * kotlin.math.sqrt(magB)
        return if (denom > 1e-8f) (dot / denom).coerceIn(0f, 1f) else 0f
    }

    // ---- Native method declarations ----

    private external fun nScoreCandidates(
        features: FloatArray,
        numCandidates: Int,
        numFeatures: Int,
        weights: FloatArray,
        topK: Int
    ): IntArray

    private external fun nCosineSimilarity(
        vecA: FloatArray,
        vecB: FloatArray,
        dim: Int
    ): Float

    private external fun nBatchCosineSimilarity(
        userVec: FloatArray,
        candidateVecs: FloatArray,
        numCandidates: Int,
        dim: Int
    ): FloatArray
}
