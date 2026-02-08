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
public:
    Spatializer() : leftDelayBuffer(4096, 0.0f), rightDelayBuffer(4096, 0.0f),
                    writeIndex(0), headRadius(0.0875f), speedOfSound(343.0f), enabled(false) {}

    void process(float* buffer, int numFrames, float azimuth, float elevation, int sampleRate) {
        if (!enabled.load(std::memory_order_relaxed)) return;

        // Woodworth ITD model
        // Delay = (r/c) * (sin(theta) + theta)
        float thetaL = azimuth + M_PI / 2.0f;
        float thetaR = azimuth - M_PI / 2.0f;

        float delayL = (headRadius / speedOfSound) * (sin(azimuth) + azimuth) * sampleRate;
        float delayR = (headRadius / speedOfSound) * (sin(-azimuth) - azimuth) * sampleRate;

        // ILD and Head Shadowing factors
        float gainL = 1.0f;
        float gainR = 1.0f;

        // Simple head shadowing: attenuate the ear that is facing away
        if (azimuth > 0) { // Source is on the right
            gainL = 1.0f - (0.5f * sin(azimuth));
        } else { // Source is on the left
            gainR = 1.0f - (0.5f * sin(-azimuth));
        }

        // Elevation effect (basic gain reduction)
        float elevationGain = cos(elevation);
        gainL *= elevationGain;
        gainR *= elevationGain;

        for (int i = 0; i < numFrames; ++i) {
            float inL = buffer[i * 2];
            float inR = buffer[i * 2 + 1];

            // Update delay buffers
            leftDelayBuffer[writeIndex] = inL;
            rightDelayBuffer[writeIndex] = inR;

            // Read with fractional delay (linear interpolation)
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

static Spatializer spatializer;
static Limiter limiter;

extern "C"
JNIEXPORT void JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nProcess(JNIEnv *env, jobject thiz,
                                                            jfloatArray buffer, jfloat azimuth,
                                                            jfloat elevation, jint sample_rate) {
    jfloat* data = env->GetFloatArrayElements(buffer, nullptr);
    jsize len = env->GetArrayLength(buffer);

    if (data != nullptr) {
        // 1. Spatial Audio (if enabled inside class)
        spatializer.process(data, len / 2, azimuth, elevation, sample_rate);
        
        // 2. Limiter / Volume Boost (if enabled inside class)
        limiter.process(data, len / 2, 2, sample_rate); // Assuming Stereo (2 channels)

        env->ReleaseFloatArrayElements(buffer, data, 0);
    }
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
