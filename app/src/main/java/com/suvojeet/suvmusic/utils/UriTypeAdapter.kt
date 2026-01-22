package com.suvojeet.suvmusic.utils

import android.net.Uri
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.IOException

/**
 * Gson TypeAdapter for serializing and deserializing Android Uri objects.
 */
class UriTypeAdapter : TypeAdapter<Uri>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Uri?) {
        out.value(value?.toString())
    }

    @Throws(IOException::class)
    override fun read(input: JsonReader): Uri? {
        val token = input.peek()
        if (token == com.google.gson.stream.JsonToken.NULL) {
            input.nextNull()
            return null
        }
        if (token == com.google.gson.stream.JsonToken.STRING) {
            val uriString = input.nextString()
            return if (uriString == null) null else Uri.parse(uriString)
        }
        if (token == com.google.gson.stream.JsonToken.BEGIN_OBJECT) {
            // Handle legacy encoded Uri objects by skipping them
            input.skipValue() 
            // Return empty Uri to indicate broken/legacy data - logic in repository updates this
            return Uri.EMPTY 
        }
        return null
    }
}
