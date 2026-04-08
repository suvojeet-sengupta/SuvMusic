#ifndef AI_AUDIO_PROCESSOR_H
#define AI_AUDIO_PROCESSOR_H

#include <vector>
#include <atomic>

struct AIAudioState {
    bool eqEnabled;
    float eqBands[10];
    float bassBoost;
    float virtualizer;
    bool spatialEnabled;
    bool crossfeedEnabled;
    
    // Professional Tuning (Limiter/Dynamics)
    float limiterThresholdDb;
    float limiterRatio;
    float limiterAttackMs;
    float limiterReleaseMs;
    float limiterMakeupGain;
};

struct AudioSignalStats {
    float peakLevel;
    float rmsLevel;
};

class AIAudioProcessor {
public:
    static void applyState(const AIAudioState& state);
    static AIAudioState getCurrentState();
    static void updateStats(const float* buffer, int numFrames, int numChannels);
    static AudioSignalStats getLatestStats();
};

#endif // AI_AUDIO_PROCESSOR_H
