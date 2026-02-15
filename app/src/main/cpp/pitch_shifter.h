#ifndef PITCH_SHIFTER_H
#define PITCH_SHIFTER_H

#include <vector>
#include <cmath>
#include <algorithm>
#include <atomic>
#include <mutex>

/**
 * High-quality Pitch Shifter using dual delay-line technique with crossfading.
 * This provides much smoother results than simple resampling for pitch adjustment.
 */
class PitchShifter {
public:
    PitchShifter() : enabled(false), pitchRatio(1.0f), sampleRate(44100) {
        delayBuffer.resize(8192 * 2, 0.0f); // Large enough for low pitch
        reset();
    }

    void setParams(float pitch, int sr) {
        std::lock_guard<std::mutex> lock(mtx);
        this->pitchRatio = std::max(0.1f, std::min(5.0f, pitch));
        if (sr > 0) this->sampleRate = sr;
        enabled = (std::abs(pitchRatio - 1.0f) > 0.01f);
    }

    void process(float* buffer, int numFrames, int numChannels) {
        if (!enabled.load(std::memory_order_acquire)) return;
        if (numChannels > 2) return; // Simplified for Mono/Stereo

        std::lock_guard<std::mutex> lock(mtx);

        float rate = 1.0f - pitchRatio;
        int bufferSize = delayBuffer.size() / numChannels;

        for (int i = 0; i < numFrames; ++i) {
            for (int ch = 0; ch < numChannels; ++ch) {
                float input = buffer[i * numChannels + ch];
                
                // Write to delay line
                delayBuffer[writeIndex * numChannels + ch] = input;

                // Dual delay line pitch shifting
                float offset1 = pos1;
                float offset2 = pos2;

                // Triangular crossfade window
                float crossfade = std::abs(pos1 - (MAX_DELAY / 2.0f)) / (MAX_DELAY / 2.0f);
                
                float out1 = readDelay(ch, offset1, numChannels);
                float out2 = readDelay(ch, offset2, numChannels);

                buffer[i * numChannels + ch] = out1 * (1.0f - crossfade) + out2 * crossfade;
            }

            // Update delay positions
            pos1 += rate;
            while (pos1 >= MAX_DELAY) pos1 -= MAX_DELAY;
            while (pos1 < 0) pos1 += MAX_DELAY;

            pos2 = pos1 + (MAX_DELAY / 2.0f);
            while (pos2 >= MAX_DELAY) pos2 -= MAX_DELAY;
            
            writeIndex = (writeIndex + 1) % bufferSize;
        }
    }

    void reset() {
        std::fill(delayBuffer.begin(), delayBuffer.end(), 0.0f);
        writeIndex = 0;
        pos1 = 0;
        pos2 = MAX_DELAY / 2.0f;
    }

private:
    std::mutex mtx;
    std::atomic<bool> enabled;
    float pitchRatio;
    int sampleRate;
    
    std::vector<float> delayBuffer;
    int writeIndex = 0;
    float pos1 = 0, pos2 = 0;
    const float MAX_DELAY = 4096.0f;

    float readDelay(int channel, float offset, int numChannels) {
        int bufferSize = delayBuffer.size() / numChannels;
        float readIdx = (float)writeIndex - offset;
        while (readIdx < 0) readIdx += (float)bufferSize;
        
        int i1 = (int)readIdx % bufferSize;
        int i2 = (i1 + 1) % bufferSize;
        float frac = readIdx - (float)((int)readIdx);

        float v1 = delayBuffer[i1 * numChannels + channel];
        float v2 = delayBuffer[i2 * numChannels + channel];
        return v1 + frac * (v2 - v1);
    }
};

#endif // PITCH_SHIFTER_H
