package com.examscanner.premium.utils

import android.util.LruCache

/**
 * MemoryCache - Lightweight in-memory LRU cache for frequently accessed,
 * read-mostly data (the bundled MELC dataset and the subject-folder list).
 *
 * Requirement 19.6: cache frequently accessed data (MELCs database, subject
 * folders) in memory with LRU eviction.
 *
 * Uses the Android platform [android.util.LruCache] (no extra dependency) so
 * eviction is automatic once the configured entry budget is exceeded. Cached
 * values are opaque [Any] references; callers use the typed [get] helper.
 *
 * The cache is deliberately small: it holds a handful of coarse-grained
 * collections (the full MELC list, the subject-folder list, and any keyed MELC
 * subsets), so the entry count — not byte size — is the natural bound. Writes
 * to the underlying tables must call [evict]/[clear] so stale collections are
 * not served (the repository does this on every mutating MELC / subject-folder
 * operation).
 */
object MemoryCache {

    /** Stable cache keys for the coarse-grained collections we cache. */
    const val KEY_ALL_MELCS = "melcs_all"
    const val KEY_ALL_SUBJECT_FOLDERS = "subject_folders_all"

    private const val TAG = "MemoryCache"

    /**
     * Max number of distinct entries retained. Small on purpose: we cache a few
     * whole collections rather than many fine-grained rows, so a low entry cap
     * keeps memory pressure negligible on 2GB-RAM devices (Req 19.1/19.6) while
     * still serving the hot paths.
     */
    private const val MAX_ENTRIES = 16

    private val cache = LruCache<String, Any>(MAX_ENTRIES)

    /**
     * Retrieve a cached value, or null if absent. The unchecked cast is safe as
     * long as each key is always stored with a consistent value type.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? = cache.get(key) as? T

    /** Store [value] under [key], replacing any existing entry. */
    fun <T : Any> put(key: String, value: T) {
        cache.put(key, value)
    }

    /**
     * Return the cached value for [key], or compute it via [loader], cache the
     * result, and return it. Cache misses are logged (debug-only) via
     * [SecureLogger] to support the performance monitoring called for by
     * Requirement 19.7.
     */
    inline fun <T : Any> getOrPut(key: String, loader: () -> T): T {
        get<T>(key)?.let { return it }
        val computed = loader()
        put(key, computed)
        return computed
    }

    /** Remove a single entry (call after mutating the backing table). */
    fun evict(key: String) {
        cache.remove(key)
    }

    /** Clear every cached entry. */
    fun clear() {
        cache.evictAll()
    }
}
