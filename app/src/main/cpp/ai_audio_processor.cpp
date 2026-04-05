#include "ai_audio_processor.h"
#include "limiter.h"
#include "biquad.h"
#include <algorithm>

// Extern declarations for existing engine instances from spatial_audio.cpp
// Note: We'd ideally move these to a common header, but this works for now.
#include "spatial_audio_bridge.h" 

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
    getEngineCrossfeed().setParams(state.crossfeedEnabled, 0.5f); // 0.5 default strength

    // 6. Apply Limiter Makeup Gain
    getEngineLimiter().setParams(-0.1f, 4.0f, 5.0f, 100.0f, state.limiterMakeupGain);
}

AIAudioState AIAudioProcessor::getCurrentState() {
    AIAudioState state;
    state.eqEnabled = getEngineEqualizer().isEnabled();
    for (int i = 0; i < 10; ++i) {
        state.eqBands[i] = getEngineEqualizer().getBandGain(i);
    }
    state.bassBoost = getEngineBassBoost().getStrength();
    state.virtualizer = getEngineVirtualizer().getStrength();
    state.spatialEnabled = false; // Spatializer lacks isEnabled getter currently
    state.crossfeedEnabled = true;
    state.limiterMakeupGain = 0.0f;
    return state;
}
