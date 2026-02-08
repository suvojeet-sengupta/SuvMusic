#ifndef LIMITER_H
#define LIMITER_H

#include <vector>
#include <cmath>
#include <algorithm>
#include <mutex>
#include <atomic>

class Limiter {
public:
    Limiter();
    
    // Process audio in-place. Buffer is interleaved [L, R, L, R...]
    void process(float* buffer, int numFrames, int numChannels, int sampleRate);
    
    void setParams(float thresholdDb, float ratio, float attackMs, float releaseMs, float makeupGainDb);
    void setEnabled(bool enabled);
    void setBalance(float balance);
    void reset();

private:
    std::mutex mtx;
    std::atomic<bool> enabled;
    float threshold; // Linear
    float ratio;
    float attackCoeff;
    float releaseCoeff;
    float makeupGain; // Linear
    
    // Look-ahead buffer
    std::vector<float> delayBuffer;
    int delayWriteIndex;
    int delayLength; // In frames
    
    float envelope;
    
    // Constants
    const float LOOKAHEAD_MS = 5.0f; // 5ms lookahead

    float attackMs_;
    float releaseMs_;
    int currentSampleRate;
    float balance; // -1.0 (Left) to 1.0 (Right)
    
    void updateCoefficients(int sampleRate);
};

#endif // LIMITER_H