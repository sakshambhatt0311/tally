package com.tally.app.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import java.util.concurrent.ConcurrentHashMap

/**
 * App-level "hot cache" for keyed Room flows. For each key it builds the source flow once and
 * shares it on the app scope, so the latest DB emission stays in memory even after the UI (and its
 * ViewModel) is destroyed. When the user navigates back into the same Circle a new ViewModel
 * re-collects and instantly replays the cached value — 0ms load, no empty-frame flash.
 *
 * [SharingStarted.WhileSubscribed] with a 10s timeout keeps it hot briefly after the last collector
 * leaves, then stops the upstream query so idle circles don't hold live DB cursors forever.
 */
internal class KeyedFlowCache<T>(
    private val scope: CoroutineScope,
    private val source: (String) -> Flow<T>,
) {
    private val cache = ConcurrentHashMap<String, Flow<T>>()

    operator fun get(key: String): Flow<T> = cache.getOrPut(key) {
        source(key)
            .flowOn(Dispatchers.Default) // heavy mapping (toSummary etc.) off the caller thread
            .shareIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 10_000), replay = 1)
    }
}
