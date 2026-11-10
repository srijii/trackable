package com.example.util

/**
 * Helpers for cleaning up API keys pasted into the app.
 *
 * Mobile clipboards commonly add surrounding quotes, a "Bearer " prefix, an
 * environment-variable name ("GROQ_API_KEY=gsk_..."), zero-width characters or
 * line breaks. Sending any of those to a provider results in a misleading
 * "invalid API key" error, so every key is normalized before use and storage.
 */
object ApiKeyUtils {

    private val envVarPrefix = Regex("^[A-Z][A-Z0-9_]*=(.+)$")
    private val authorizationPrefix = Regex("^(?i)authorization\\s*:\\s*")
    private val bearerPrefix = Regex("^(?i)bearer\\s+")

    /** Returns the key with clipboard noise removed. Safe to call repeatedly. */
    fun normalize(raw: String): String {
        var key = raw
            .replace("\uFEFF", "")
            .filterNot { it == '\u200B' || it == '\u200C' || it == '\u200D' || it == '\u2060' }
            .replace('\u00A0', ' ')
            .trim()

        // Strip wrapping quotes first so values like "\"Bearer gsk_...\"" are handled.
        key = stripWrappingQuotes(key)
        key = key.replace(authorizationPrefix, "")
        key = key.replace(bearerPrefix, "")
        key = envVarPrefix.find(key)?.groupValues?.get(1)?.trim() ?: key
        key = stripWrappingQuotes(key)

        // API keys never contain whitespace.
        return key.filterNot { it.isWhitespace() }
    }

    private fun stripWrappingQuotes(value: String): String = value
        .trim()
        .trim('"', '\'', '`')
        .trim()

    /**
     * Checks whether a value can plausibly be a Groq API key.
     * Returns null when it looks fine, otherwise a user-facing explanation.
     */
    fun groqKeyIssue(raw: String): String? {
        val key = normalize(raw)
        if (key.isBlank()) return null
        return when {
            key.contains('•') || key.contains('…') || key.contains("...") ->
                "This key looks masked or truncated. Copy the full key from console.groq.com/keys."
            key.length < 30 ->
                "This key looks too short. Groq keys start with gsk_ and are usually 50+ characters."
            key.startsWith("gsk_") -> null
            key.startsWith("sk-") ->
                "That looks like an OpenAI key (sk-...), not a Groq key. Groq keys start with gsk_."
            key.startsWith("AIza") ->
                "That looks like a Google API key, not a Groq key. Groq keys start with gsk_."
            key.startsWith("nvapi-") ->
                "That looks like an NVIDIA key, not a Groq key. Groq keys start with gsk_."
            else ->
                "Groq API keys start with gsk_. Make sure you copied the full key from console.groq.com/keys."
        }
    }

    /** Turns Groq's HTTP error into an actionable message. */
    fun groqHttpErrorMessage(code: Int, detail: String? = null): String {
        val message = when (code) {
            400 -> "Groq rejected the request (HTTP 400). Check the model name and try again."
            401 -> "Groq rejected the API key (HTTP 401). It is invalid, revoked, or was copied incorrectly. " +
                "Copy the full key from console.groq.com/keys — it starts with gsk_ — then try again."
            403 -> "Groq denied access (HTTP 403). Check that the key is active and has access to the selected model."
            404 -> "Groq could not find the selected model (HTTP 404). Check the model name in Settings."
            413 -> "The photo is too large for Groq (HTTP 413). Try a smaller image."
            429 -> "Groq's rate limit was reached (HTTP 429). Wait a moment and retry."
            in 500..599 -> "Groq is temporarily unavailable (HTTP $code). Try again shortly."
            else -> "Groq request failed (HTTP $code)."
        }
        return if (detail.isNullOrBlank()) message else "$message Details: ${detail.take(300)}"
    }
}
