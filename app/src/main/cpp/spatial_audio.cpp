#include <jni.h>
#include <vector>
#include <cmath>
#include <algorithm>
#include <atomic>
#include <cstdint>
#include <mutex>
#include "limiter.h"
#include "biquad.h"
#include "pitch_shifter.h"
#include "spatial_audio_bridge.h"
#include "audio_engine_components.h"
#include "ai_audio_processor.h"

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

static Spatializer spatializer;
static Limiter limiter;
static Crossfeed crossfeed;
static ParametricEQ equalizer;
static BassBoost bassBoost;
static Virtualizer virtualizer;
static PitchShifter pitchShifter;
static std::vector<float> processingBuffer;
static std::mutex processingMutex;

static constexpr int MAX_TOTAL_SAMPLES = 48000 * 8; // max 48kHz * 8 channels = 1 second cap

extern "C"
JNIEXPORT void JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nApplyAIState(JNIEnv *env, jobject thiz, 
    jboolean eqEnabled, jfloatArray eqBands, jfloat bassBoost, jfloat virtualizer, 
    jboolean spatialEnabled, jboolean crossfeedEnabled, 
    jfloat limiterThreshold, jfloat limiterRatio, jfloat limiterAttack, jfloat limiterRelease, jfloat limiterGain) {
    
    AIAudioState state;
    state.eqEnabled = eqEnabled;
    state.bassBoost = bassBoost;
    state.virtualizer = virtualizer;
    state.spatialEnabled = spatialEnabled;
    state.crossfeedEnabled = crossfeedEnabled;
    
    state.limiterThresholdDb = limiterThreshold;
    state.limiterRatio = limiterRatio;
    state.limiterAttackMs = limiterAttack;
    state.limiterReleaseMs = limiterRelease;
    state.limiterMakeupGain = limiterGain;

    jfloat* bands = env->GetFloatArrayElements(eqBands, nullptr);
    for (int i = 0; i < 10; ++i) {
        state.eqBands[i] = bands[i];
    }
    env->ReleaseFloatArrayElements(eqBands, bands, JNI_ABORT);

    AIAudioProcessor::applyState(state);
}

extern "C"
JNIEXPORT jfloat JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nGetPeakLevel(JNIEnv *env, jobject thiz) {
    return AIAudioProcessor::getLatestStats().peakLevel;
}

extern "C"
JNIEXPORT jfloat JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nGetRmsLevel(JNIEnv *env, jobject thiz) {
    return AIAudioProcessor::getLatestStats().rmsLevel;
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
    if (totalSamples <= 0 || totalSamples > MAX_TOTAL_SAMPLES) {
        return;
    }

    std::lock_guard<std::mutex> lock(processingMutex);

    if (processingBuffer.size() < static_cast<size_t>(totalSamples)) {
        processingBuffer.resize(static_cast<size_t>(totalSamples));
    }

    float *floatData = processingBuffer.data();
    for (int i = 0; i < totalSamples; ++i) {
        floatData[i] = static_cast<float>(pcmData[i]) / 32768.0f;
    }

    crossfeed.process(floatData, frameCount, channelCount, sampleRate);
    equalizer.process(floatData, frameCount, channelCount, sampleRate);
    bassBoost.process(floatData, frameCount, channelCount);
    virtualizer.process(floatData, frameCount, channelCount);
    pitchShifter.process(floatData, frameCount, channelCount);
    spatializer.process(floatData, frameCount, channelCount, azimuth, elevation, sampleRate);
    limiter.process(floatData, frameCount, channelCount, sampleRate);

    // AI Analyzer - Direct signal feedback
    AIAudioProcessor::updateStats(floatData, frameCount, channelCount);

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
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nSetEqPreamp(JNIEnv *env, jobject thiz, jfloat gainDb) {
    equalizer.setPreamp(gainDb);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nSetBassBoost(JNIEnv *env, jobject thiz, jfloat strength) {
    bassBoost.setStrength(strength);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nSetVirtualizer(JNIEnv *env, jobject thiz, jfloat strength) {
    virtualizer.setStrength(strength);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nSetCrossfeedParams(JNIEnv *env, jobject thiz, jboolean enabled, jfloat strength) {
    crossfeed.setParams(enabled, strength);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nSetPlaybackParams(JNIEnv *env, jobject thiz, jfloat pitch) {
    pitchShifter.setParams(pitch, 44100);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nReset(JNIEnv *env, jobject thiz) {
    spatializer.reset();
    limiter.reset();
    crossfeed.reset();
    equalizer.reset();
    bassBoost.reset();
    virtualizer.reset();
    pitchShifter.reset();
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

extern "C"
JNIEXPORT jfloat JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nGetEqBand(JNIEnv *env, jobject thiz, jint index) {
    return equalizer.getBandGain(index);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nIsEqEnabled(JNIEnv *env, jobject thiz) {
    return equalizer.isEnabled();
}

extern "C"
JNIEXPORT jfloat JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nGetBassBoost(JNIEnv *env, jobject thiz) {
    return bassBoost.getStrength();
}

extern "C"
JNIEXPORT jfloat JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nGetVirtualizer(JNIEnv *env, jobject thiz) {
    return virtualizer.getStrength();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nIsSpatializerEnabled(JNIEnv *env, jobject thiz) {
    return spatializer.isEnabled();
}

ParametricEQ& getEngineEqualizer() { return equalizer; }
BassBoost& getEngineBassBoost() { return bassBoost; }
Virtualizer& getEngineVirtualizer() { return virtualizer; }
Spatializer& getEngineSpatializer() { return spatializer; }
Crossfeed& getEngineCrossfeed() { return crossfeed; }
Limiter& getEngineLimiter() { return limiter; }
