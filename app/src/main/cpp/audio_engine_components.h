#ifndef AUDIO_ENGINE_COMPONENTS_H
#define AUDIO_ENGINE_COMPONENTS_H

#include <vector>
#include <cmath>
#include <algorithm>
#include <atomic>
#include <mutex>
#include "biquad.h"
#include "limiter.h"

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

class Spatializer {
public:
    static constexpr int DELAY_BUFFER_SIZE = 4096;
    
    Spatializer() : leftDelayBuffer(DELAY_BUFFER_SIZE, 0.0f), rightDelayBuffer(DELAY_BUFFER_SIZE, 0.0f),
                    writeIndex(0), headRadius(0.0875f), speedOfSound(343.0f), enabled(false) {}

    void process(float* buffer, int numFrames, int channelCount, float azimuth, float elevation, int sampleRate) {
        if (!enabled.load(std::memory_order_acquire)) return;
        if (buffer == nullptr || numFrames <= 0 || sampleRate <= 0) return;
        if (channelCount != 2) return;

        std::lock_guard<std::mutex> lock(processMutex);
        float absAzimuth = fabsf(azimuth);
        float itdSamples = (headRadius / speedOfSound) * (sinf(absAzimuth) + absAzimuth) * static_cast<float>(sampleRate);
        itdSamples = std::min(itdSamples, static_cast<float>(DELAY_BUFFER_SIZE - 1));

        float delayL = (azimuth > 0.0f) ? itdSamples : 0.0f;
        float delayR = (azimuth < 0.0f) ? itdSamples : 0.0f;

        float gainL = 1.0f;
        float gainR = 1.0f;
        if (azimuth > 0.0f) {
            gainL = 1.0f - (0.6f * sinf(absAzimuth));
        } else if (azimuth < 0.0f) {
            gainR = 1.0f - (0.6f * sinf(absAzimuth));
        }

        float elevationGain = cosf(elevation);
        gainL *= elevationGain;
        gainR *= elevationGain;

        for (int i = 0; i < numFrames; ++i) {
            float inL = buffer[i * 2];
            float inR = buffer[i * 2 + 1];
            leftDelayBuffer[writeIndex] = inL;
            rightDelayBuffer[writeIndex] = inR;
            buffer[i * 2] = readDelay(leftDelayBuffer, writeIndex, delayL) * gainL;
            buffer[i * 2 + 1] = readDelay(rightDelayBuffer, writeIndex, delayR) * gainR;
            writeIndex = (writeIndex + 1) % DELAY_BUFFER_SIZE;
        }
    }

    void reset() {
        std::lock_guard<std::mutex> lock(processMutex);
        std::fill(leftDelayBuffer.begin(), leftDelayBuffer.end(), 0.0f);
        std::fill(rightDelayBuffer.begin(), rightDelayBuffer.end(), 0.0f);
        writeIndex = 0;
    }
    
    void setEnabled(bool e) { enabled.store(e, std::memory_order_release); }
    bool isEnabled() { return enabled.load(std::memory_order_acquire); }

private:
    std::mutex processMutex;
    std::vector<float> leftDelayBuffer;
    std::vector<float> rightDelayBuffer;
    int writeIndex;
    const float headRadius;
    const float speedOfSound;
    std::atomic<bool> enabled;

    float readDelay(const std::vector<float>& buffer, int currentWriteIndex, float delaySamples) {
        float readIndex = static_cast<float>(currentWriteIndex) - delaySamples;
        if (readIndex < 0.0f) readIndex += static_cast<float>(DELAY_BUFFER_SIZE);
        readIndex = std::max(0.0f, std::min(readIndex, static_cast<float>(DELAY_BUFFER_SIZE - 1)));
        int i1 = static_cast<int>(readIndex);
        int i2 = (i1 + 1) % DELAY_BUFFER_SIZE;
        float frac = readIndex - static_cast<float>(i1);
        return buffer[i1] * (1.0f - frac) + buffer[i2] * frac;
    }
};

class Crossfeed {
public:
    static constexpr int DELAY_BUFFER_SIZE = 512;
    
    Crossfeed() : enabled(false), strength(0.0f), sampleRate(44100), 
                  delayBufferL(DELAY_BUFFER_SIZE, 0.0f), delayBufferR(DELAY_BUFFER_SIZE, 0.0f) {
        setParams(false, 0.0f);
    }

    void setParams(bool e, float s) {
        enabled.store(e, std::memory_order_release);
        strength.store(std::max(0.0f, std::min(1.0f, s)), std::memory_order_release);
        
        float fc = 700.0f; 
        float w0 = 2.0f * M_PI * fc / (float)sampleRate;
        b1 = expf(-w0);
        a0 = 1.0f - b1;
    }

    void process(float* buffer, int numFrames, int channelCount, int sr) {
        if (!enabled.load(std::memory_order_acquire) || channelCount != 2) return;
        if (sr != sampleRate) {
            sampleRate = sr;
            setParams(enabled.load(), strength.load());
        }

        std::lock_guard<std::mutex> lock(processMutex);
        float localStrength = strength.load(std::memory_order_acquire);
        float delayMs = 0.25f; 
        float delaySamples = (delayMs / 1000.0f) * (float)sampleRate;

        for (int i = 0; i < numFrames; ++i) {
            float inL = buffer[i * 2];
            float inR = buffer[i * 2 + 1];
            delayBufferL[writeIndex] = inL;
            delayBufferR[writeIndex] = inR;
            float delayedL = readDelay(delayBufferL, delaySamples);
            float delayedR = readDelay(delayBufferR, delaySamples);
            lpL = a0 * delayedL + b1 * lpL;
            lpR = a0 * delayedR + b1 * lpR;
            buffer[i * 2] = (inL * (1.0f - localStrength * 0.5f)) + (lpR * localStrength);
            buffer[i * 2 + 1] = (inR * (1.0f - localStrength * 0.5f)) + (lpL * localStrength);
            writeIndex = (writeIndex + 1) % DELAY_BUFFER_SIZE;
        }
    }

    void reset() {
        std::lock_guard<std::mutex> lock(processMutex);
        std::fill(delayBufferL.begin(), delayBufferL.end(), 0.0f);
        std::fill(delayBufferR.begin(), delayBufferR.end(), 0.0f);
        lpL = 0.0f; lpR = 0.0f; writeIndex = 0;
    }

    bool isEnabled() { return enabled.load(std::memory_order_acquire); }

private:
    std::mutex processMutex;
    std::atomic<bool> enabled;
    std::atomic<float> strength;
    int sampleRate;
    std::vector<float> delayBufferL;
    std::vector<float> delayBufferR;
    int writeIndex = 0;
    float lpL = 0.0f, lpR = 0.0f;
    float a0, b1;

    float readDelay(const std::vector<float>& buffer, float delay) {
        float rIndex = static_cast<float>(writeIndex) - delay;
        if (rIndex < 0.0f) rIndex += static_cast<float>(DELAY_BUFFER_SIZE);
        rIndex = std::max(0.0f, std::min(rIndex, static_cast<float>(DELAY_BUFFER_SIZE - 1)));
        int i1 = static_cast<int>(rIndex);
        int i2 = (i1 + 1) % DELAY_BUFFER_SIZE;
        float frac = rIndex - static_cast<int>(rIndex);
        return buffer[i1] * (1.0f - frac) + buffer[i2] * frac;
    }
};

class ParametricEQ {
public:
    ParametricEQ() : enabled(false), preampGain(1.0f) {
        float freqs[] = {31.0f, 62.0f, 125.0f, 250.0f, 500.0f, 1000.0f, 2000.0f, 4000.0f, 8000.0f, 16000.0f};
        for (int i = 0; i < 10; ++i) {
            Biquad filter;
            FilterType type = PEAKING;
            if (i == 0) type = LOW_SHELF;
            else if (i == 9) type = HIGH_SHELF;
            filter.setParams(type, freqs[i], 1.41f, 0.0f, 44100);
            filters.push_back(filter);
        }
    }

    void setBandGain(int bandIndex, float gainDb) {
        std::lock_guard<std::mutex> lock(filterMutex);
        if (bandIndex >= 0 && bandIndex < 10) {
            gainDb = std::max(-15.0f, std::min(15.0f, gainDb));
            filters[bandIndex].updateGain(gainDb);
        }
    }

    void setPreamp(float gainDb) { preampGain.store(powf(10.0f, gainDb / 20.0f), std::memory_order_release); }
    void setEnabled(bool e) { enabled.store(e, std::memory_order_release); }

    void process(float* buffer, int numFrames, int numChannels, int sampleRate) {
        if (!enabled.load(std::memory_order_acquire)) return;
        float pGain = preampGain.load(std::memory_order_acquire);
        if (pGain != 1.0f) {
            for (int i = 0; i < numFrames * numChannels; ++i) buffer[i] *= pGain;
        }
        std::lock_guard<std::mutex> lock(filterMutex);
        for (auto& filter : filters) filter.process(buffer, numFrames, numChannels);
    }
    
    void reset() {
        std::lock_guard<std::mutex> lock(filterMutex);
        for (auto& filter : filters) filter.reset();
        preampGain.store(1.0f, std::memory_order_release);
    }

    float getBandGain(int index) {
        std::lock_guard<std::mutex> lock(filterMutex);
        if (index >= 0 && index < (int)filters.size()) return filters[index].getGainDb();
        return 0.0f;
    }

    bool isEnabled() { return enabled.load(std::memory_order_acquire); }

private:
    std::mutex filterMutex;
    std::atomic<bool> enabled;
    std::atomic<float> preampGain;
    std::vector<Biquad> filters;
};

class BassBoost {
public:
    BassBoost() : strength(0.0f) { filter.setParams(LOW_SHELF, 80.0f, 1.0f, 0.0f, 44100); }
    void setStrength(float s) { strength.store(s, std::memory_order_release); filter.updateGain(s * 12.0f); }
    void process(float* buffer, int numFrames, int numChannels) {
        if (strength.load(std::memory_order_acquire) <= 0.01f) return;
        filter.process(buffer, numFrames, numChannels);
    }
    void reset() { filter.reset(); strength.store(0.0f, std::memory_order_release); }
    float getStrength() { return strength.load(std::memory_order_acquire); }

private:
    Biquad filter;
    std::atomic<float> strength;
};

class Virtualizer {
public:
    Virtualizer() : strength(0.0f) {}
    void setStrength(float s) { strength.store(s, std::memory_order_release); }
    float getStrength() { return strength.load(std::memory_order_acquire); }
    void process(float* buffer, int numFrames, int numChannels) {
        float s = strength.load(std::memory_order_acquire);
        if (s <= 0.01f || numChannels < 2) return;
        for (int i = 0; i < numFrames; ++i) {
            float l = buffer[i * 2]; float r = buffer[i * 2 + 1];
            float mid = (l + r) * 0.5f; float side = (l - r) * 0.5f;
            side *= (1.0f + s * 0.8f);
            buffer[i * 2] = mid + side; buffer[i * 2 + 1] = mid - side;
        }
    }
    void reset() { strength.store(0.0f, std::memory_order_release); }

private:
    std::atomic<float> strength;
};

#endif // AUDIO_ENGINE_COMPONENTS_H
