#include <jni.h>
#include <vector>
#include <cmath>
#include <algorithm>
#include <atomic>
#include "limiter.h"

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

class Spatializer {
// ... existing Spatializer code ...
};

class Crossfeed {
public:
    Crossfeed() : enabled(false), strength(0.3f), sampleRate(44100) {
        delayBufferL.resize(128, 0.0f);
        delayBufferR.resize(128, 0.0f);
        reset();
    }

    void setParams(bool enabled, float strength) {
        this->enabled.store(enabled, std::memory_order_relaxed);
        this->strength.store(strength, std::memory_order_relaxed);
    }

    void process(float* buffer, int numFrames, int sr) {
        if (!enabled.load(std::memory_order_relaxed)) return;
        
        if (sr != sampleRate) {
            sampleRate = sr;
            reset();
        }

        float localStrength = strength.load(std::memory_order_relaxed);
        // Delay for ~300 microseconds (standard for crossfeed)
        float delaySamples = (300.0f / 1000000.0f) * (float)sampleRate;
        
        // Simple 1st order Low Pass Filter coeffs (~700Hz)
        float cutoff = 700.0f;
        float x = exp(-2.0f * M_PI * cutoff / (float)sampleRate);
        float a0 = 1.0f - x;
        float b1 = x;

        for (int i = 0; i < numFrames; ++i) {
            float inL = buffer[i * 2];
            float inR = buffer[i * 2 + 1];

            // 1. Update Delay Buffers
            delayBufferL[writeIndex] = inL;
            delayBufferR[writeIndex] = inR;

            // 2. Read Delayed Samples
            float delayedL = readDelay(delayBufferL, delaySamples);
            float delayedR = readDelay(delayBufferR, delaySamples);

            // 3. Low Pass Filter the delayed "cross" signal
            lpL = a0 * delayedL + b1 * lpL;
            lpR = a0 * delayedR + b1 * lpR;

            // 4. Mix Crossfeed (attenuate main to keep volume consistent)
            // L_out = L_main + (R_cross * strength)
            buffer[i * 2] = (inL * (1.0f - localStrength * 0.5f)) + (lpR * localStrength);
            buffer[i * 2 + 1] = (inR * (1.0f - localStrength * 0.5f)) + (lpL * localStrength);

            writeIndex = (writeIndex + 1) % 128;
        }
    }

    void reset() {
        std::fill(delayBufferL.begin(), delayBufferL.end(), 0.0f);
        std::fill(delayBufferR.begin(), delayBufferR.end(), 0.0f);
        lpL = 0.0f;
        lpR = 0.0f;
        writeIndex = 0;
    }

private:
    std::atomic<bool> enabled;
    std::atomic<float> strength;
    int sampleRate;
    std::vector<float> delayBufferL;
    std::vector<float> delayBufferR;
    int writeIndex = 0;
    float lpL = 0.0f, lpR = 0.0f; // Low pass states

    float readDelay(const std::vector<float>& buffer, float delay) {
        float rIndex = (float)writeIndex - delay;
        if (rIndex < 0) rIndex += 128.0f;
        int i1 = (int)rIndex;
        int i2 = (i1 + 1) % 128;
        float frac = rIndex - (float)i1;
        return buffer[i1] * (1.0f - frac) + buffer[i2] * frac;
    }
};

static Spatializer spatializer;
static Limiter limiter;
static Crossfeed crossfeed;

extern "C"
JNIEXPORT void JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nProcess(JNIEnv *env, jobject thiz,
                                                            jfloatArray buffer, jfloat azimuth,
                                                            jfloat elevation, jint sample_rate) {
    jfloat* data = (jfloat*)env->GetPrimitiveArrayCritical(buffer, nullptr);
    jsize len = env->GetArrayLength(buffer);

    if (data != nullptr) {
        // 1. Crossfeed (Subtle headphone correction)
        crossfeed.process(data, len / 2, sample_rate);

        // 2. Spatial Audio (Positioning)
        spatializer.process(data, len / 2, azimuth, elevation, sample_rate);
        
        // 3. Limiter / Volume Boost
        limiter.process(data, len / 2, 2, sample_rate);

        env->ReleasePrimitiveArrayCritical(buffer, data, 0);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nSetCrossfeedParams(JNIEnv *env, jobject thiz, jboolean enabled, jfloat strength) {
    crossfeed.setParams(enabled, strength);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nReset(JNIEnv *env, jobject thiz) {
    spatializer.reset();
    limiter.reset();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nSetSpatializerEnabled(JNIEnv *env, jobject thiz, jboolean enabled) {
    spatializer.setEnabled(enabled);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nSetLimiterEnabled(JNIEnv *env, jobject thiz, jboolean enabled) {
    limiter.setEnabled(enabled);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nSetLimiterParams(JNIEnv *env, jobject thiz, 
                                                                       jfloat thresholdDb, jfloat ratio, 
                                                                       jfloat attackMs, jfloat releaseMs, 
                                                                       jfloat makeupGainDb) {
    limiter.setParams(thresholdDb, ratio, attackMs, releaseMs, makeupGainDb);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nSetLimiterBalance(JNIEnv *env, jobject thiz, jfloat balance) {
    limiter.setBalance(balance);
}
