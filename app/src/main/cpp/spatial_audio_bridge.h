#ifndef SPATIAL_AUDIO_BRIDGE_H
#define SPATIAL_AUDIO_BRIDGE_H

#include "audio_engine_components.h"

ParametricEQ& getEngineEqualizer();
BassBoost& getEngineBassBoost();
Virtualizer& getEngineVirtualizer();
Spatializer& getEngineSpatializer();
Crossfeed& getEngineCrossfeed();
Limiter& getEngineLimiter();

#endif // SPATIAL_AUDIO_BRIDGE_H
