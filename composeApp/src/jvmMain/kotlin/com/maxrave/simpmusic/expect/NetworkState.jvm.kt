package com.maxrave.simpmusic.expect

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// The JDK has no connectivity callback, so this never emits and callers fall back to their retry
// timer. It must also never complete: emptyFlow() completes at once, and `first()` on it threw
// NoSuchElementException on the UI thread, which quit the app whenever Home failed to load offline.
actual fun networkBecameAvailable(): Flow<Unit> = flow { awaitCancellation() }
