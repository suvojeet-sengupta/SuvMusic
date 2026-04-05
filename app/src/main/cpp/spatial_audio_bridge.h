#ifndef SPATIAL_AUDIO_BRIDGE_H
#define SPATIAL_AUDIO_BRIDGE_H

// Forward declarations of classes if needed, or include headers
#include "limiter.h"
#include "biquad.h"

// Define interfaces to access the static engine components
class ParametricEQ;
class BassBoost;
class Virtualizer;
class Spatializer;
class Crossfeed;
class Limiter;

ParametricEQ& getEngineEqualizer();
BassBoost& getEngineBassBoost();
Virtualizer& getEngineVirtualizer();
Spatializer& getEngineSpatializer();
Crossfeed& getEngineCrossfeed();
Limiter& getEngineLimiter();

#endif // SPATIAL_AUDIO_BRIDGE_H
