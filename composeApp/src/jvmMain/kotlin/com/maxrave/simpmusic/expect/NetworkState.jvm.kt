package com.maxrave.simpmusic.expect

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

// The JDK has no connectivity callback; callers fall back to their retry timer.
actual fun networkBecameAvailable(): Flow<Unit> = emptyFlow()
