#include <jni.h>
#include <vector>
#include <cmath>
#include <algorithm>
#include <atomic>
#include <cstdint>
#include "limiter.h"
#include "biquad.h"

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

class Spatializer {
public:
    Spatializer() : leftDelayBuffer(4096, 0.0f), rightDelayBuffer(4096, 0.0f),
                    writeIndex(0), headRadius(0.0875f), speedOfSound(343.0f), enabled(false) {}

    void process(float* buffer, int numFrames, float azimuth, float elevation, int sampleRate) {
        if (!enabled.load(std::memory_order_relaxed)) return;

        // Correct Woodworth ITD model (magnitude only)
        // Delay = (r/c) * (sin|theta| + |theta|)
        float absAzimuth = fabsf(azimuth);
        float itdSamples = (headRadius / speedOfSound) * (sinf(absAzimuth) + absAzimuth) * (float)sampleRate;

        // Determine which ear to delay
        // If azimuth > 0 (Source on Right), Left ear is delayed
        // If azimuth < 0 (Source on Left), Right ear is delayed
        float delayL = (azimuth > 0.0f) ? itdSamples : 0.0f;
        float delayR = (azimuth < 0.0f) ? itdSamples : 0.0f;

        // ILD (Interaural Level Difference) - Head Shadowing
        float gainL = 1.0f;
        float gainR = 1.0f;
        if (azimuth > 0.0f) {
            gainL = 1.0f - (0.6f * sinf(absAzimuth));
        } else if (azimuth < 0.0f) {
            gainR = 1.0f - (0.6f * sinf(absAzimuth));
        }

        // Elevation effect
        float elevationGain = cosf(elevation);
        gainL *= elevationGain;
        gainR *= elevationGain;

        for (int i = 0; i < numFrames; ++i) {
            float inL = buffer[i * 2];
            float inR = buffer[i * 2 + 1];

            leftDelayBuffer[writeIndex] = inL;
            rightDelayBuffer[writeIndex] = inR;

            // Apply correct positive delays
            buffer[i * 2] = readDelay(leftDelayBuffer, writeIndex, delayL) * gainL;
            buffer[i * 2 + 1] = readDelay(rightDelayBuffer, writeIndex, delayR) * gainR;

            writeIndex = (writeIndex + 1) % 4096;
        }
    }

    void reset() {
        std::fill(leftDelayBuffer.begin(), leftDelayBuffer.end(), 0.0f);
        std::fill(rightDelayBuffer.begin(), rightDelayBuffer.end(), 0.0f);
        writeIndex = 0;
    }
    
    void setEnabled(bool e) { enabled.store(e, std::memory_order_relaxed); }

private:
    std::vector<float> leftDelayBuffer;
    std::vector<float> rightDelayBuffer;
    int writeIndex;
    float headRadius;
    float speedOfSound;
    std::atomic<bool> enabled;

    float readDelay(const std::vector<float>& buffer, int currentWriteIndex, float delaySamples) {
        float readIndex = (float)currentWriteIndex - delaySamples;
        while (readIndex < 0) readIndex += 4096.0f;
        while (readIndex >= 4096.0f) readIndex -= 4096.0f;

        int i1 = (int)readIndex;
        int i2 = (i1 + 1) % 4096;
        float frac = readIndex - (float)i1;

        return buffer[i1] * (1.0f - frac) + buffer[i2] * frac;
    }
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

class ParametricEQ {
public:
    ParametricEQ() : enabled(false) {
        // Standard 10-Band ISO Frequencies
        float freqs[] = {31.0f, 62.0f, 125.0f, 250.0f, 500.0f, 1000.0f, 2000.0f, 4000.0f, 8000.0f, 16000.0f};
        
        for (int i = 0; i < 10; ++i) {
            Biquad filter;
            FilterType type = PEAKING;
            if (i == 0) type = LOW_SHELF;
            else if (i == 9) type = HIGH_SHELF;
            
            filter.setParams(type, freqs[i], 1.41f, 0.0f, 44100); // Q=1.41 (Butterworth)
            filters.push_back(filter);
        }
    }

    void setBandGain(int bandIndex, float gainDb) {
        if (bandIndex >= 0 && bandIndex < 10) {
            filters[bandIndex].updateGain(gainDb);
        }
    }

    void setEnabled(bool e) {
        enabled.store(e, std::memory_order_relaxed);
    }

    void process(float* buffer, int numFrames, int numChannels, int sampleRate) {
        if (!enabled.load(std::memory_order_relaxed)) return;

        // Process each filter in series
        for (auto& filter : filters) {
            // Need to update sample rate if changed? (Ideally yes, but skipping for simplicity now)
            filter.process(buffer, numFrames, numChannels);
        }
    }
    
    void reset() {
        for (auto& filter : filters) filter.reset();
    }

private:
    std::atomic<bool> enabled;
    std::vector<Biquad> filters;
};

static Spatializer spatializer;
static Limiter limiter;
static Crossfeed crossfeed;
static ParametricEQ equalizer;
static std::vector<float> processingBuffer;

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

        // 2. Parametric EQ (Tone shaping before spatial)
        equalizer.process(data, len / 2, 2, sample_rate);

        // 3. Spatial Audio (Positioning)
        spatializer.process(data, len / 2, azimuth, elevation, sample_rate);
        
        // 4. Limiter / Volume Boost
        limiter.process(data, len / 2, 2, sample_rate);

        env->ReleasePrimitiveArrayCritical(buffer, data, 0);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nProcessPcm16(JNIEnv *env, jobject thiz,
                                                                   jobject buffer,
                                                                   jint frameCount,
                                                                   jint channelCount,
                                                                   jint sampleRate,
                                                                   jfloat azimuth,
                                                                   jfloat elevation) {
    if (buffer == nullptr || frameCount <= 0 || channelCount <= 0) {
        return;
    }

    auto *pcmData = static_cast<int16_t *>(env->GetDirectBufferAddress(buffer));
    if (pcmData == nullptr) {
        return;
    }

    const int totalSamples = frameCount * channelCount;
    if (totalSamples <= 0) {
        return;
    }

    if (processingBuffer.size() < static_cast<size_t>(totalSamples)) {
        processingBuffer.resize(static_cast<size_t>(totalSamples));
    }

    float *floatData = processingBuffer.data();
    for (int i = 0; i < totalSamples; ++i) {
        floatData[i] = static_cast<float>(pcmData[i]) / 32768.0f;
    }

    crossfeed.process(floatData, frameCount, sampleRate);
    equalizer.process(floatData, frameCount, channelCount, sampleRate);
    spatializer.process(floatData, frameCount, azimuth, elevation, sampleRate);
    limiter.process(floatData, frameCount, channelCount, sampleRate);

    for (int i = 0; i < totalSamples; ++i) {
        float sample = std::max(-1.0f, std::min(1.0f, floatData[i]));
        pcmData[i] = static_cast<int16_t>(sample * 32767.0f);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nSetEqEnabled(JNIEnv *env, jobject thiz, jboolean enabled) {
    equalizer.setEnabled(enabled);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nSetEqBand(JNIEnv *env, jobject thiz, jint bandIndex, jfloat gainDb) {
    equalizer.setBandGain(bandIndex, gainDb);
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
    equalizer.reset();
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
