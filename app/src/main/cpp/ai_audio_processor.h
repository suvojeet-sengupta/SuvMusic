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
    float limiterMakeupGain;
};

class AIAudioProcessor {
public:
    static void applyState(const AIAudioState& state);
    static AIAudioState getCurrentState();
};

#endif // AI_AUDIO_PROCESSOR_H
