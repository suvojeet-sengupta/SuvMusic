#include <jni.h>
#include <string>
#include <cstring>

/**
 * Native key derivation for SecureConfig.
 * Moving key logic to native code makes it significantly harder to reverse-engineer
 * compared to Java/Kotlin bytecode which can be trivially decompiled.
 */

// Obfuscated key components — split across multiple arrays and operations
// to resist simple string extraction from the .so binary.
static const uint8_t kSeed1[] = {0x63, 0x69, 0x73, 0x75, 0x6D};       // "cisum" reversed fragment
static const uint8_t kSeed2[] = {0x76, 0x75, 0x73, 0x74, 0x65};       // "vuste" reversed fragment
static const uint8_t kSeed3[] = {0x6A, 0x6F, 0x76, 0x75, 0x73, 0x53}; // "jovusS" reversed fragment

static std::string deriveKeyNative() {
    // Reconstruct the key through multiple transformations
    // This is equivalent to the old logic but much harder to extract
    std::string result;
    result.reserve(16);

    // Assemble from fragments in reverse order
    for (int i = 0; i < 5; i++) result += static_cast<char>(kSeed1[i]);
    for (int i = 0; i < 5; i++) result += static_cast<char>(kSeed2[i]);
    for (int i = 0; i < 6; i++) result += static_cast<char>(kSeed3[i]);

    // Take first 16 chars
    if (result.length() > 16) {
        result = result.substr(0, 16);
    }

    return result;
}

// JioSaavn DES key derivation
static const uint8_t kDesBase[] = {66, 71, 66, 67, 69, 68, 72, 64};

static std::string deriveDesKeyNative() {
    std::string result;
    result.reserve(8);
    for (int i = 0; i < 8; i++) {
        result += static_cast<char>(kDesBase[i] - 15);
    }
    return result;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_suvojeet_suvmusic_util_SecureConfig_nDeriveKey(JNIEnv *env, jobject thiz) {
    std::string key = deriveKeyNative();
    return env->NewStringUTF(key.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_suvojeet_suvmusic_util_SecureConfig_nDeriveDesKey(JNIEnv *env, jobject thiz) {
    std::string key = deriveDesKeyNative();
    return env->NewStringUTF(key.c_str());
}
