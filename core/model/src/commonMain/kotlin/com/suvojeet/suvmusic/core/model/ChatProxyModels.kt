package com.suvojeet.suvmusic.core.model

/**
 * Catalog of model identifiers exposed by the Chat Proxy worker.
 *
 * Originally lived in `app/.../ai/AIClients.kt`; lifted to :core:model so
 * the AI Settings screen (now in composeApp/commonMain) can render the
 * picker without depending on :app's AI runtime stack (OkHttp, Gson,
 * coroutines context). The runtime client in :app continues to import
 * these constants and call [resolve] when issuing requests.
 */
object ChatProxyModels {
    val ALL = listOf(
        "gpt-5.2",
        "gpt-5.1",
        "gpt-5",
        "anthropic/claude-sonnet-4",
        "mercury-coder",
        "Olmo-3.1-32B-Instruct",
        "chatgpt-4o-latest",
        "google/gemini-2.5-pro-preview-05-06",
        "x-ai/grok-4",
        "deepseek-ai/deepseek-v3.2",
        "deepseek-ai/deepseek-v3.1-terminus",
        "deepseek-ai/deepseek-R1-0528",
        "o1-preview",
        "o3-mini",
        "qwen/qwen3.5-397b-a17b",
        "qwen/qwen3-coder-480b-a35b-instruct",
        "moonshotai/kimi-k2.5",
        "moonshotai/kimi-k2-thinking",
        "moonshotai/kimi-k2-instruct-0905",
        "openai/gpt-oss-120b",
        "openai/gpt-oss-20b",
        "meta/llama-3.1-405b-instruct",
        "meta/llama-4-maverick-17b-128e-instruct",
        "meta/llama-4-scout-17b-16e-instruct",
        "meta-llama-3.3-70b-instruct",
        "meta-llama-3.1-8b-instruct",
        "google/gemma-3-27b-it",
        "nvidia/nemotron-3-nano-30b-a3b",
        "qwen/qwq-32b",
        "qwen/qwen3-235b-a22b",
        "minimaxai/minimax-m2",
        "accounts/fireworks/models/glm-4p7",
        "meta-llama/Llama-3.1-8B-Instruct",
        "mistralai/mistral-large-3-675b-instruct-2512",
        "mistralai/magistral-small-2506",
        "mistralai/mistral-small-3.1-24b-instruct-2503",
        "mistralai/ministral-14b-instruct-2512"
    )

    const val RANDOM = "__RANDOM__"

    /**
     * Resolves the effective model name.
     * If [selected] is [RANDOM], picks a random model from [ALL].
     * Otherwise returns [selected] as-is.
     */
    fun resolve(selected: String): String {
        return if (selected == RANDOM) ALL.random() else selected
    }

    /**
     * Returns [ALL] with "Random" prepended as the first option.
     */
    fun withRandomOption(): List<String> {
        return listOf(RANDOM) + ALL
    }

    /**
     * Display name for a model key.
     */
    fun displayName(key: String): String {
        return if (key == RANDOM) "Random (Auto)" else key
    }
}
