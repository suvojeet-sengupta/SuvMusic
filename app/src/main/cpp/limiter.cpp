#include "limiter.h"

Limiter::Limiter() : enabled(false), threshold(1.0f), ratio(1.0f), 
                     attackCoeff(0.0f), releaseCoeff(0.0f), makeupGain(1.0f),
                     delayWriteIndex(0), delayLength(0), envelope(0.0f),
                     attackMs_(0.1f), releaseMs_(100.0f), currentSampleRate(0), balance(0.0f) {
    setParams(-0.1f, 20.0f, 0.1f, 100.0f, 0.0f);
}

void Limiter::setParams(float thresholdDb, float ratio, float attackMs, float releaseMs, float makeupGainDb) {
    std::lock_guard<std::mutex> lock(mtx);
    this->threshold = pow(10.0f, thresholdDb / 20.0f);
    this->ratio = ratio;
    this->makeupGain = pow(10.0f, makeupGainDb / 20.0f);
    
    this->attackMs_ = attackMs;
    this->releaseMs_ = releaseMs;
}

void Limiter::setBalance(float balance) {
    std::lock_guard<std::mutex> lock(mtx);
    this->balance = std::max(-1.0f, std::min(1.0f, balance));
}

void Limiter::process(float* buffer, int numFrames, int numChannels, int sampleRate) {
    if (!enabled.load(std::memory_order_relaxed)) return;

    // Local copies of parameters to minimize lock time
    float localThreshold, localRatio, localMakeupGain, localBalance;
    float localAttackMs, localReleaseMs;
    {
        std::lock_guard<std::mutex> lock(mtx);
        localThreshold = threshold;
        localRatio = ratio;
        localMakeupGain = makeupGain;
        localBalance = balance;
        localAttackMs = attackMs_;
        localReleaseMs = releaseMs_;
    }

    if (sampleRate != currentSampleRate) {
        currentSampleRate = sampleRate;
        delayLength = (int)(LOOKAHEAD_MS * sampleRate / 1000.0f);
        delayBuffer.assign(delayLength * numChannels, 0.0f);
        delayWriteIndex = 0;
        
        // Recalculate coeffs
        float attackSamples = localAttackMs * sampleRate / 1000.0f;
        float releaseSamples = localReleaseMs * sampleRate / 1000.0f;
        
        if (attackSamples < 1.0f) attackCoeff = 0.0f; 
        else attackCoeff = exp(-1.0f / attackSamples);

        if (releaseSamples < 1.0f) releaseCoeff = 0.0f;
        else releaseCoeff = exp(-1.0f / releaseSamples);
    }

    // Calculate balance gains
    float balGainL = 1.0f;
    float balGainR = 1.0f;
    if (localBalance > 0.0f) { // Right bias, attenuate Left
        balGainL = 1.0f - localBalance;
    } else if (localBalance < 0.0f) { // Left bias, attenuate Right
        balGainR = 1.0f + localBalance;
    }

    for (int i = 0; i < numFrames; ++i) {
        float maxAbsInput = 0.0f;
        
        // Use a fixed-size array to avoid heap allocation in the loop
        float inputFrame[8]; // Support up to 8 channels
        int safeChannels = std::min(numChannels, 8);

        for (int ch = 0; ch < safeChannels; ++ch) {
            float val = buffer[i * numChannels + ch] * localMakeupGain;
            
            // Apply Balance
            if (ch == 0) val *= balGainL;
            if (ch == 1) val *= balGainR;

            inputFrame[ch] = val;
            float absSample = fabs(val);
            if (absSample > maxAbsInput) maxAbsInput = absSample;
        }
        
        // 2. Update envelope (peak detector)
        if (maxAbsInput > envelope) {
            envelope = attackCoeff * envelope + (1.0f - attackCoeff) * maxAbsInput;
        } else {
            envelope = releaseCoeff * envelope + (1.0f - releaseCoeff) * maxAbsInput;
        }
        
        // 3. Calculate gain reduction
        float gain = 1.0f;
        if (envelope > localThreshold) {
            float envDb = 20.0f * log10(envelope + 1e-6f);
            float threshDb = 20.0f * log10(localThreshold + 1e-6f);
            float excessDb = envDb - threshDb;
            
            float reductionDb = excessDb * (1.0f / localRatio - 1.0f);
            gain = pow(10.0f, reductionDb / 20.0f);
        }
        
        // 4. Apply Look-ahead Delay and Gain
        for (int ch = 0; ch < safeChannels; ++ch) {
            float inputSample = inputFrame[ch];

            // Write to delay buffer
            int writePos = (delayWriteIndex * numChannels) + ch;
            float delayedSample = delayBuffer[writePos];
            delayBuffer[writePos] = inputSample;
            
            // Output = Delayed * Gain
            float outputSample = delayedSample * gain;
            
            // Hard Clamping to prevent any overflow/distortion
            if (outputSample > 1.0f) outputSample = 1.0f;
            else if (outputSample < -1.0f) outputSample = -1.0f;

            buffer[i * numChannels + ch] = outputSample;
        }
        
        delayWriteIndex++;
        if (delayWriteIndex >= delayLength) delayWriteIndex = 0;
    }
}

void Limiter::setEnabled(bool enabled) {
    this->enabled.store(enabled, std::memory_order_relaxed);
    if (!enabled) {
        reset();
    }
}

void Limiter::reset() {
    envelope = 0.0f;
    std::fill(delayBuffer.begin(), delayBuffer.end(), 0.0f);
    delayWriteIndex = 0;
}

void Limiter::updateCoefficients(int sampleRate) {
    // Already handled in process() check
}