#ifndef BIQUAD_H
#define BIQUAD_H

#include <cmath>
#include <vector>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

enum FilterType {
    LOW_SHELF,
    PEAKING,
    HIGH_SHELF
};

class Biquad {
public:
    Biquad() : type(PEAKING), frequency(1000.0f), q(1.0f), gainDb(0.0f), sampleRate(44100) {
        reset();
    }

    void setParams(FilterType type, float frequency, float q, float gainDb, int sampleRate) {
        this->type = type;
        this->frequency = frequency;
        this->q = q;
        this->gainDb = gainDb;
        this->sampleRate = sampleRate;
        calculateCoefficients();
    }
    
    void updateGain(float gainDb) {
        if (std::abs(this->gainDb - gainDb) < 0.01f) return;
        this->gainDb = gainDb;
        calculateCoefficients();
    }

    void process(float* buffer, int numFrames, int numChannels) {
        for (int i = 0; i < numFrames; ++i) {
            for (int ch = 0; ch < numChannels; ++ch) {
                float in = buffer[i * numChannels + ch];
                
                // Direct Form I
                float out = b0 * in + b1 * x1[ch] + b2 * x2[ch] - a1 * y1[ch] - a2 * y2[ch];
                
                // Shift states
                x2[ch] = x1[ch];
                x1[ch] = in;
                y2[ch] = y1[ch];
                y1[ch] = out;
                
                buffer[i * numChannels + ch] = out;
            }
        }
    }
    
    void reset() {
        for (int i = 0; i < 8; ++i) {
            x1[i] = 0.0f; x2[i] = 0.0f;
            y1[i] = 0.0f; y2[i] = 0.0f;
        }
        calculateCoefficients();
    }

private:
    FilterType type;
    float frequency;
    float q;
    float gainDb;
    int sampleRate;

    // Coefficients
    float b0, b1, b2, a0, a1, a2;
    
    // State variables (support up to 8 channels)
    float x1[8], x2[8], y1[8], y2[8];

    void calculateCoefficients() {
        float A = pow(10.0f, gainDb / 40.0f);
        float w0 = 2.0f * M_PI * frequency / sampleRate;
        float alpha = sin(w0) / (2.0f * q);
        float cosW0 = cos(w0);

        switch (type) {
            case LOW_SHELF:
                b0 = A * ((A + 1.0f) - (A - 1.0f) * cosW0 + 2.0f * sqrt(A) * alpha);
                b1 = 2.0f * A * ((A - 1.0f) - (A + 1.0f) * cosW0);
                b2 = A * ((A + 1.0f) - (A - 1.0f) * cosW0 - 2.0f * sqrt(A) * alpha);
                a0 = (A + 1.0f) + (A - 1.0f) * cosW0 + 2.0f * sqrt(A) * alpha;
                a1 = -2.0f * ((A - 1.0f) + (A + 1.0f) * cosW0);
                a2 = (A + 1.0f) + (A - 1.0f) * cosW0 - 2.0f * sqrt(A) * alpha;
                break;

            case HIGH_SHELF:
                b0 = A * ((A + 1.0f) + (A - 1.0f) * cosW0 + 2.0f * sqrt(A) * alpha);
                b1 = -2.0f * A * ((A - 1.0f) + (A + 1.0f) * cosW0);
                b2 = A * ((A + 1.0f) + (A - 1.0f) * cosW0 - 2.0f * sqrt(A) * alpha);
                a0 = (A + 1.0f) - (A - 1.0f) * cosW0 + 2.0f * sqrt(A) * alpha;
                a1 = 2.0f * ((A - 1.0f) - (A + 1.0f) * cosW0);
                a2 = (A + 1.0f) - (A - 1.0f) * cosW0 - 2.0f * sqrt(A) * alpha;
                break;

            case PEAKING:
            default:
                b0 = 1.0f + alpha * A;
                b1 = -2.0f * cosW0;
                b2 = 1.0f - alpha * A;
                a0 = 1.0f + alpha / A;
                a1 = -2.0f * cosW0;
                a2 = 1.0f - alpha / A;
                break;
        }

        // Normalize
        b0 /= a0;
        b1 /= a0;
        b2 /= a0;
        a1 /= a0;
        a2 /= a0;
    }
};

#endif // BIQUAD_H