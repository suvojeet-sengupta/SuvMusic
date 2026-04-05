#include "ai_audio_processor.h"
#include "limiter.h"
#include "biquad.h"
#include <algorithm>

// Extern declarations for existing engine instances from spatial_audio.cpp
// Note: We'd ideally move these to a common header, but this works for now.
#include "spatial_audio_bridge.h" 

#include <cmath>
#include <mutex>

static AudioSignalStats g_latestStats = {0.0f, 0.0f};
static std::mutex g_statsMutex;

void AIAudioProcessor::applyState(const AIAudioState& state) {
    // 1. Apply EQ
    getEngineEqualizer().setEnabled(state.eqEnabled);
    for (int i = 0; i < 10; ++i) {
        getEngineEqualizer().setBandGain(i, state.eqBands[i]);
    }

    // 2. Apply Bass Boost
    getEngineBassBoost().setStrength(state.bassBoost);

    // 3. Apply Virtualizer
    getEngineVirtualizer().setStrength(state.virtualizer);

    // 4. Apply Spatializer
    getEngineSpatializer().setEnabled(state.spatialEnabled);

    // 5. Apply Crossfeed
    getEngineCrossfeed().setParams(state.crossfeedEnabled, 0.5f);

    // 6. Apply Professional Limiter Tuning
    getEngineLimiter().setEnabled(true);
    getEngineLimiter().setParams(
        state.limiterThresholdDb,
        state.limiterRatio,
        state.limiterAttackMs,
        state.limiterReleaseMs,
        state.limiterMakeupGain
    );
}

AIAudioState AIAudioProcessor::getCurrentState() {
    AIAudioState state;
    state.eqEnabled = getEngineEqualizer().isEnabled();
    for (int i = 0; i < 10; ++i) {
        state.eqBands[i] = getEngineEqualizer().getBandGain(i);
    }
    state.bassBoost = getEngineBassBoost().getStrength();
    state.virtualizer = getEngineVirtualizer().getStrength();
    state.spatialEnabled = false; 
    state.crossfeedEnabled = true;
    
    // Default professional values if not set
    state.limiterThresholdDb = -0.1f;
    state.limiterRatio = 4.0f;
    state.limiterAttackMs = 5.0f;
    state.limiterReleaseMs = 100.0f;
    state.limiterMakeupGain = 0.0f;
    
    return state;
}

void AIAudioProcessor::updateStats(const float* buffer, int numFrames, int numChannels) {
    if (!buffer || numFrames <= 0) return;

    float peak = 0.0f;
    float sumSquares = 0.0f;
    int totalSamples = numFrames * numChannels;

    for (int i = 0; i < totalSamples; ++i) {
        float absSample = std::abs(buffer[i]);
        if (absSample > peak) peak = absSample;
        sumSquares += buffer[i] * buffer[i];
    }

    float rms = std::sqrt(sumSquares / (float)totalSamples);

    std::lock_guard<std::mutex> lock(g_statsMutex);
    // Simple EMA (Exponential Moving Average) for smoothing
    g_latestStats.peakLevel = g_latestStats.peakLevel * 0.9f + peak * 0.1f;
    g_latestStats.rmsLevel = g_latestStats.rmsLevel * 0.9f + rms * 0.1f;
}

AudioSignalStats AIAudioProcessor::getLatestStats() {
    std::lock_guard<std::mutex> lock(g_statsMutex);
    return g_latestStats;
}
