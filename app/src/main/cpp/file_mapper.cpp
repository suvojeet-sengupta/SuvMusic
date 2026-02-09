#include <jni.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <vector>
#include <android/log.h>

#define TAG "NativeFileMapper"

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_suvojeet_suvmusic_player_NativeSpatialAudio_nExtractWaveform(JNIEnv *env, jobject thiz,
                                                                    jstring file_path, jint num_points) {
    const char *path = env->GetStringUTFChars(file_path, nullptr);
    
    int fd = open(path, O_RDONLY);
    if (fd < 0) {
        env->ReleaseStringUTFChars(file_path, path);
        return nullptr;
    }

    struct stat st;
    if (fstat(fd, &st) < 0) {
        close(fd);
        env->ReleaseStringUTFChars(file_path, path);
        return nullptr;
    }

    size_t size = st.st_size;
    if (size == 0) {
        close(fd);
        env->ReleaseStringUTFChars(file_path, path);
        return nullptr;
    }

    // MMAP the file!
    // This maps the entire file into memory without actually reading it.
    // The OS will page in data as we access it.
    void *addr = mmap(nullptr, size, PROT_READ, MAP_PRIVATE, fd, 0);
    if (addr == MAP_FAILED) {
        close(fd);
        env->ReleaseStringUTFChars(file_path, path);
        return nullptr;
    }

    // For demonstration, we assume it's a raw 16-bit PCM file (or we treat any file as bytes)
    // and extract "energy" levels by scanning chunks.
    int16_t *data = static_cast<int16_t*>(addr);
    size_t num_samples = size / sizeof(int16_t);
    
    std::vector<float> waveform(num_points, 0.0f);
    size_t samples_per_point = num_samples / num_points;

    if (samples_per_point > 0) {
        for (int i = 0; i < num_points; ++i) {
            float max_val = 0.0f;
            size_t start = i * samples_per_point;
            size_t end = (i + 1) * samples_per_point;
            
            // Scan chunk for peak
            // Performance: Since it's mmap-ed, the OS handles the IO efficiently.
            for (size_t j = start; j < end; j += 100) { // Sub-sample for speed
                float val = std::abs(data[j]) / 32768.0f;
                if (val > max_val) max_val = val;
            }
            waveform[i] = max_val;
        }
    }

    // Clean up mmap
    munmap(addr, size);
    close(fd);
    env->ReleaseStringUTFChars(file_path, path);

    // Return the float array to Java
    jfloatArray result = env->NewFloatArray(num_points);
    env->SetFloatArrayRegion(result, 0, num_points, waveform.data());
    return result;
}
