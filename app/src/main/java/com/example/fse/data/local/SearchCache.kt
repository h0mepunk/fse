package com.example.fse.data.local

import com.example.fse.domain.model.Food
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory cache for API responses. TTL = 5 minutes.
 */
class SearchCache<T>(private val ttlMs: Long = 5 * 60 * 1000) {
    private data class Entry<U>(val data: U, val expiresAt: Long)

    private val cache = mutableMapOf<String, Entry<T>>()
    private val mutex = Mutex()

    suspend fun get(key: String): T? = mutex.withLock {
        cache[key]?.let { entry ->
            if (System.currentTimeMillis() < entry.expiresAt) entry.data else {
                cache.remove(key)
                null
            }
        }
    }

    suspend fun put(key: String, value: T) = mutex.withLock {
        cache[key] = Entry(value, System.currentTimeMillis() + ttlMs)
    }
}
