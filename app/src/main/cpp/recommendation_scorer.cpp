/**
 * recommendation_scorer.cpp — Native SIMD-accelerated recommendation scoring engine
 *
 * Processes candidate songs in batches using ARM NEON (or SSE on x86) for vectorized
 * weighted scoring. Accepts flat SoA (Structure of Arrays) feature data from JNI and
 * returns top-K candidate indices sorted by descending score.
 *
 * Signal weights layout (10 floats):
 *   [0] base         — starting score (0.5)
 *   [1] artistAff    — artist affinity weight (0.22)
 *   [2] freshness    — freshness bonus weight (0.12)
 *   [3] skipPenalty   — skip avoidance penalty (0.12)
 *   [4] likedSong    — liked song boost (0.12)
 *   [5] likedArtist  — liked artist boost (0.08)
 *   [6] timeOfDay    — time-of-day weight (0.08)
 *   [7] varietyPen   — variety penalty per excess artist (0.10)
 *   [8] genreSim     — genre similarity weight (0.20)
 *   [9] recentGenre  — recent/session genre similarity (0.08)
 *   [10] skipGenre   — skip genre penalty (0.08)
 *
 * Features layout (flat array, N candidates × 10 features, row-major):
 *   For candidate i, feature j is at index [j * N + i] (SoA / column-major)
 *
 * Copyright (c) 2026 SuvMusic. All rights reserved.
 */

#include <jni.h>
#include <cstdlib>
#include <cmath>
#include <algorithm>
#include <vector>
#include <android/log.h>

#ifdef __ARM_NEON__
#include <arm_neon.h>
#elif defined(__SSE__)
#include <xmmintrin.h>
#include <emmintrin.h>
#endif

#define LOG_TAG "NativeRecoScorer"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Number of scoring features per candidate
static constexpr int NUM_FEATURES = 11;

// Number of weight values expected
static constexpr int NUM_WEIGHTS = 11;

/**
 * Clamp a float to [0, 1].
 */
static inline float clamp01(float x) {
    return x < 0.0f ? 0.0f : (x > 1.0f ? 1.0f : x);
}

/**
 * Compute cosine similarity between two vectors of given dimension.
 * Returns 0 if either vector has zero magnitude.
 */
static float cosine_similarity(const float* a, const float* b, int dim) {
    float dot = 0.0f, magA = 0.0f, magB = 0.0f;

#ifdef __ARM_NEON__
    // NEON: process 4 floats at a time
    int i = 0;
    float32x4_t vDot = vdupq_n_f32(0.0f);
    float32x4_t vMagA = vdupq_n_f32(0.0f);
    float32x4_t vMagB = vdupq_n_f32(0.0f);

    for (; i + 3 < dim; i += 4) {
        float32x4_t va = vld1q_f32(a + i);
        float32x4_t vb = vld1q_f32(b + i);
        vDot = vmlaq_f32(vDot, va, vb);
        vMagA = vmlaq_f32(vMagA, va, va);
        vMagB = vmlaq_f32(vMagB, vb, vb);
    }

    // Horizontal sum
    float32x2_t dLow = vget_low_f32(vDot);
    float32x2_t dHigh = vget_high_f32(vDot);
    dLow = vadd_f32(dLow, dHigh);
    dot = vget_lane_f32(vpadd_f32(dLow, dLow), 0);

    float32x2_t aLow = vget_low_f32(vMagA);
    float32x2_t aHigh = vget_high_f32(vMagA);
    aLow = vadd_f32(aLow, aHigh);
    magA = vget_lane_f32(vpadd_f32(aLow, aLow), 0);

    float32x2_t bLow = vget_low_f32(vMagB);
    float32x2_t bHigh = vget_high_f32(vMagB);
    bLow = vadd_f32(bLow, bHigh);
    magB = vget_lane_f32(vpadd_f32(bLow, bLow), 0);

    // Handle remainder
    for (; i < dim; i++) {
        dot += a[i] * b[i];
        magA += a[i] * a[i];
        magB += b[i] * b[i];
    }

#elif defined(__SSE__)
    int i = 0;
    __m128 sDot = _mm_setzero_ps();
    __m128 sMagA = _mm_setzero_ps();
    __m128 sMagB = _mm_setzero_ps();

    for (; i + 3 < dim; i += 4) {
        __m128 va = _mm_loadu_ps(a + i);
        __m128 vb = _mm_loadu_ps(b + i);
        sDot = _mm_add_ps(sDot, _mm_mul_ps(va, vb));
        sMagA = _mm_add_ps(sMagA, _mm_mul_ps(va, va));
        sMagB = _mm_add_ps(sMagB, _mm_mul_ps(vb, vb));
    }

    // Horizontal sum (SSE3 hadd would be better but SSE2 is more portable)
    float tmpDot[4], tmpA[4], tmpB[4];
    _mm_storeu_ps(tmpDot, sDot);
    _mm_storeu_ps(tmpA, sMagA);
    _mm_storeu_ps(tmpB, sMagB);
    dot = tmpDot[0] + tmpDot[1] + tmpDot[2] + tmpDot[3];
    magA = tmpA[0] + tmpA[1] + tmpA[2] + tmpA[3];
    magB = tmpB[0] + tmpB[1] + tmpB[2] + tmpB[3];

    for (; i < dim; i++) {
        dot += a[i] * b[i];
        magA += a[i] * a[i];
        magB += b[i] * b[i];
    }

#else
    // Scalar fallback
    for (int i = 0; i < dim; i++) {
        dot += a[i] * b[i];
        magA += a[i] * a[i];
        magB += b[i] * b[i];
    }
#endif

    float denom = sqrtf(magA) * sqrtf(magB);
    return (denom > 1e-8f) ? (dot / denom) : 0.0f;
}

/**
 * Score N candidates using weighted feature vectors.
 *
 * Features are in SoA (column-major) layout:
 *   feature[featureIndex * N + candidateIndex]
 *
 *   Feature indices:
 *     0  = artistAffinity      (0.0–1.0)
 *     1  = freshnessFlag       (0.0 or 1.0)
 *     2  = skipFlag             (0.0 or 1.0)
 *     3  = likedSongFlag        (0.0 or 1.0)
 *     4  = likedArtistFlag      (0.0 or 1.0)
 *     5  = timeOfDayWeight      (0.0–1.0)
 *     6  = varietyPenalty        (0.0–N, count above 2)
 *     7  = genreSimilarity      (0.0–1.0, cosine sim)
 *     8  = recentGenreSimilarity(0.0–1.0, cosine sim)
 *     9  = skipGenrePenalty     (0.0–1.0, cosine sim)
 *     10 = (reserved)          (0.0)
 *
 * weights[0..10] maps to base + per-feature weights
 */
static void score_candidates(
    const float* features,  // SoA: [NUM_FEATURES * N]
    int N,
    const float* weights,   // [NUM_WEIGHTS]
    float* scores           // output: [N]
) {
    const float base       = weights[0];
    const float wArtist    = weights[1];
    const float wFresh     = weights[2];
    const float wSkip      = weights[3];
    const float wLikedSong = weights[4];
    const float wLikedArt  = weights[5];
    const float wTime      = weights[6];
    const float wVariety   = weights[7];
    const float wGenre     = weights[8];
    const float wRecGenre  = weights[9];
    const float wSkipGenre = weights[10];

    // Feature column pointers (SoA)
    const float* artistAff   = features + 0 * N;
    const float* freshness   = features + 1 * N;
    const float* skipFlag    = features + 2 * N;
    const float* likedSong   = features + 3 * N;
    const float* likedArt    = features + 4 * N;
    const float* timeWeight  = features + 5 * N;
    const float* variety     = features + 6 * N;
    const float* genreSim    = features + 7 * N;
    const float* recGenreSim = features + 8 * N;
    const float* skipGenre   = features + 9 * N;

#ifdef __ARM_NEON__
    // NEON: process 4 candidates at a time
    float32x4_t vBase       = vdupq_n_f32(base);
    float32x4_t vWArtist    = vdupq_n_f32(wArtist);
    float32x4_t vWFresh     = vdupq_n_f32(wFresh);
    float32x4_t vWSkip      = vdupq_n_f32(wSkip);
    float32x4_t vWLikedSong = vdupq_n_f32(wLikedSong);
    float32x4_t vWLikedArt  = vdupq_n_f32(wLikedArt);
    float32x4_t vWTime      = vdupq_n_f32(wTime);
    float32x4_t vWVariety   = vdupq_n_f32(wVariety);
    float32x4_t vWGenre     = vdupq_n_f32(wGenre);
    float32x4_t vWRecGenre  = vdupq_n_f32(wRecGenre);
    float32x4_t vWSkipGenre = vdupq_n_f32(wSkipGenre);
    float32x4_t vZero       = vdupq_n_f32(0.0f);
    float32x4_t vOne        = vdupq_n_f32(1.0f);

    int i = 0;
    for (; i + 3 < N; i += 4) {
        float32x4_t s = vBase;

        // Positive signals: accumulate weighted features
        s = vmlaq_f32(s, vld1q_f32(artistAff + i),   vWArtist);
        s = vmlaq_f32(s, vld1q_f32(freshness + i),   vWFresh);
        s = vmlaq_f32(s, vld1q_f32(likedSong + i),   vWLikedSong);
        s = vmlaq_f32(s, vld1q_f32(likedArt + i),    vWLikedArt);
        s = vmlaq_f32(s, vld1q_f32(timeWeight + i),  vWTime);
        s = vmlaq_f32(s, vld1q_f32(genreSim + i),    vWGenre);
        s = vmlaq_f32(s, vld1q_f32(recGenreSim + i), vWRecGenre);

        // Negative signals: subtract
        s = vmlsq_f32(s, vld1q_f32(skipFlag + i),    vWSkip);
        s = vmlsq_f32(s, vld1q_f32(variety + i),     vWVariety);
        s = vmlsq_f32(s, vld1q_f32(skipGenre + i),   vWSkipGenre);

        // Clamp to [0, 1]
        s = vmaxq_f32(s, vZero);
        s = vminq_f32(s, vOne);

        vst1q_f32(scores + i, s);
    }

    // Handle remaining candidates
    for (; i < N; i++) {
        float s = base
            + artistAff[i]   * wArtist
            + freshness[i]   * wFresh
            + likedSong[i]   * wLikedSong
            + likedArt[i]    * wLikedArt
            + timeWeight[i]  * wTime
            + genreSim[i]    * wGenre
            + recGenreSim[i] * wRecGenre
            - skipFlag[i]    * wSkip
            - variety[i]     * wVariety
            - skipGenre[i]   * wSkipGenre;
        scores[i] = clamp01(s);
    }

#elif defined(__SSE__)
    __m128 vBase       = _mm_set1_ps(base);
    __m128 vWArtist    = _mm_set1_ps(wArtist);
    __m128 vWFresh     = _mm_set1_ps(wFresh);
    __m128 vWSkip      = _mm_set1_ps(wSkip);
    __m128 vWLikedSong = _mm_set1_ps(wLikedSong);
    __m128 vWLikedArt  = _mm_set1_ps(wLikedArt);
    __m128 vWTime      = _mm_set1_ps(wTime);
    __m128 vWVariety   = _mm_set1_ps(wVariety);
    __m128 vWGenre     = _mm_set1_ps(wGenre);
    __m128 vWRecGenre  = _mm_set1_ps(wRecGenre);
    __m128 vWSkipGenre = _mm_set1_ps(wSkipGenre);
    __m128 vZero       = _mm_setzero_ps();
    __m128 vOne        = _mm_set1_ps(1.0f);

    int i = 0;
    for (; i + 3 < N; i += 4) {
        __m128 s = vBase;

        s = _mm_add_ps(s, _mm_mul_ps(_mm_loadu_ps(artistAff + i),   vWArtist));
        s = _mm_add_ps(s, _mm_mul_ps(_mm_loadu_ps(freshness + i),   vWFresh));
        s = _mm_add_ps(s, _mm_mul_ps(_mm_loadu_ps(likedSong + i),   vWLikedSong));
        s = _mm_add_ps(s, _mm_mul_ps(_mm_loadu_ps(likedArt + i),    vWLikedArt));
        s = _mm_add_ps(s, _mm_mul_ps(_mm_loadu_ps(timeWeight + i),  vWTime));
        s = _mm_add_ps(s, _mm_mul_ps(_mm_loadu_ps(genreSim + i),    vWGenre));
        s = _mm_add_ps(s, _mm_mul_ps(_mm_loadu_ps(recGenreSim + i), vWRecGenre));

        s = _mm_sub_ps(s, _mm_mul_ps(_mm_loadu_ps(skipFlag + i),    vWSkip));
        s = _mm_sub_ps(s, _mm_mul_ps(_mm_loadu_ps(variety + i),     vWVariety));
        s = _mm_sub_ps(s, _mm_mul_ps(_mm_loadu_ps(skipGenre + i),   vWSkipGenre));

        s = _mm_max_ps(s, vZero);
        s = _mm_min_ps(s, vOne);

        _mm_storeu_ps(scores + i, s);
    }

    for (; i < N; i++) {
        float s = base
            + artistAff[i]   * wArtist
            + freshness[i]   * wFresh
            + likedSong[i]   * wLikedSong
            + likedArt[i]    * wLikedArt
            + timeWeight[i]  * wTime
            + genreSim[i]    * wGenre
            + recGenreSim[i] * wRecGenre
            - skipFlag[i]    * wSkip
            - variety[i]     * wVariety
            - skipGenre[i]   * wSkipGenre;
        scores[i] = clamp01(s);
    }

#else
    // Pure scalar fallback
    for (int i = 0; i < N; i++) {
        float s = base
            + artistAff[i]   * wArtist
            + freshness[i]   * wFresh
            + likedSong[i]   * wLikedSong
            + likedArt[i]    * wLikedArt
            + timeWeight[i]  * wTime
            + genreSim[i]    * wGenre
            + recGenreSim[i] * wRecGenre
            - skipFlag[i]    * wSkip
            - variety[i]     * wVariety
            - skipGenre[i]   * wSkipGenre;
        scores[i] = clamp01(s);
    }
#endif
}

/**
 * Partial top-K selection using partial_sort.
 * Returns the indices of the top K candidates sorted by descending score.
 */
static void top_k_indices(const float* scores, int N, int K, int* outIndices) {
    // Create index array
    std::vector<int> indices(N);
    for (int i = 0; i < N; i++) indices[i] = i;

    K = std::min(K, N);

    // Partial sort: only sort the top K elements
    std::partial_sort(indices.begin(), indices.begin() + K, indices.end(),
        [&scores](int a, int b) {
            return scores[a] > scores[b]; // Descending
        });

    for (int i = 0; i < K; i++) {
        outIndices[i] = indices[i];
    }
}

// ============================================================================
// JNI EXPORTS
// ============================================================================

extern "C" {

/**
 * Score candidates and return top-K indices.
 *
 * @param features     FloatArray [numFeatures * numCandidates], SoA layout
 * @param numCandidates Number of candidate songs
 * @param numFeatures   Number of features per candidate (must be NUM_FEATURES)
 * @param weights       FloatArray [NUM_WEIGHTS] — scoring weights
 * @param topK          How many top results to return
 * @return IntArray of length min(topK, numCandidates), indices sorted by descending score
 */
JNIEXPORT jintArray JNICALL
Java_com_suvojeet_suvmusic_recommendation_NativeRecommendationScorer_nScoreCandidates(
    JNIEnv* env,
    jobject /* this */,
    jfloatArray jFeatures,
    jint numCandidates,
    jint numFeatures,
    jfloatArray jWeights,
    jint topK
) {
    if (numCandidates <= 0 || numFeatures < NUM_FEATURES) {
        LOGE("Invalid params: N=%d, F=%d", numCandidates, numFeatures);
        return env->NewIntArray(0);
    }

    // Get feature and weight arrays
    jfloat* features = env->GetFloatArrayElements(jFeatures, nullptr);
    jfloat* weights = env->GetFloatArrayElements(jWeights, nullptr);

    if (!features || !weights) {
        LOGE("Failed to get array elements");
        if (features) env->ReleaseFloatArrayElements(jFeatures, features, JNI_ABORT);
        if (weights) env->ReleaseFloatArrayElements(jWeights, weights, JNI_ABORT);
        return env->NewIntArray(0);
    }

    // Allocate score buffer
    std::vector<float> scores(numCandidates);

    // Run SIMD scoring
    score_candidates(features, numCandidates, weights, scores.data());

    // Top-K selection
    int K = std::min((int)topK, (int)numCandidates);
    std::vector<int> topIndices(K);
    top_k_indices(scores.data(), numCandidates, K, topIndices.data());

    // Release input arrays
    env->ReleaseFloatArrayElements(jFeatures, features, JNI_ABORT);
    env->ReleaseFloatArrayElements(jWeights, weights, JNI_ABORT);

    // Return result
    jintArray result = env->NewIntArray(K);
    env->SetIntArrayRegion(result, 0, K, topIndices.data());

    LOGD("Scored %d candidates, returned top %d", numCandidates, K);
    return result;
}

/**
 * Compute cosine similarity between two genre vectors.
 *
 * @param vecA FloatArray — first vector
 * @param vecB FloatArray — second vector
 * @param dim  Dimension of vectors
 * @return Cosine similarity [0, 1] (clamped, since genre vectors are non-negative)
 */
JNIEXPORT jfloat JNICALL
Java_com_suvojeet_suvmusic_recommendation_NativeRecommendationScorer_nCosineSimilarity(
    JNIEnv* env,
    jobject /* this */,
    jfloatArray jVecA,
    jfloatArray jVecB,
    jint dim
) {
    jfloat* vecA = env->GetFloatArrayElements(jVecA, nullptr);
    jfloat* vecB = env->GetFloatArrayElements(jVecB, nullptr);

    if (!vecA || !vecB) {
        if (vecA) env->ReleaseFloatArrayElements(jVecA, vecA, JNI_ABORT);
        if (vecB) env->ReleaseFloatArrayElements(jVecB, vecB, JNI_ABORT);
        return 0.0f;
    }

    float sim = cosine_similarity(vecA, vecB, dim);

    env->ReleaseFloatArrayElements(jVecA, vecA, JNI_ABORT);
    env->ReleaseFloatArrayElements(jVecB, vecB, JNI_ABORT);

    // Genre vectors are non-negative, so similarity is [0, 1]
    return clamp01(sim);
}

/**
 * Batch compute cosine similarities between one user vector and N candidate vectors.
 * More efficient than N individual calls.
 *
 * @param userVector    FloatArray[dim] — single user genre vector
 * @param candidateVecs FloatArray[N * dim] — N candidate vectors packed contiguously
 * @param numCandidates Number of candidates
 * @param dim           Dimension of each vector
 * @return FloatArray[N] — cosine similarities
 */
JNIEXPORT jfloatArray JNICALL
Java_com_suvojeet_suvmusic_recommendation_NativeRecommendationScorer_nBatchCosineSimilarity(
    JNIEnv* env,
    jobject /* this */,
    jfloatArray jUserVec,
    jfloatArray jCandidateVecs,
    jint numCandidates,
    jint dim
) {
    jfloat* userVec = env->GetFloatArrayElements(jUserVec, nullptr);
    jfloat* candidateVecs = env->GetFloatArrayElements(jCandidateVecs, nullptr);

    if (!userVec || !candidateVecs || numCandidates <= 0 || dim <= 0) {
        if (userVec) env->ReleaseFloatArrayElements(jUserVec, userVec, JNI_ABORT);
        if (candidateVecs) env->ReleaseFloatArrayElements(jCandidateVecs, candidateVecs, JNI_ABORT);
        return env->NewFloatArray(0);
    }

    std::vector<float> sims(numCandidates);
    for (int i = 0; i < numCandidates; i++) {
        sims[i] = clamp01(cosine_similarity(userVec, candidateVecs + i * dim, dim));
    }

    env->ReleaseFloatArrayElements(jUserVec, userVec, JNI_ABORT);
    env->ReleaseFloatArrayElements(jCandidateVecs, candidateVecs, JNI_ABORT);

    jfloatArray result = env->NewFloatArray(numCandidates);
    env->SetFloatArrayRegion(result, 0, numCandidates, sims.data());
    return result;
}

} // extern "C"
