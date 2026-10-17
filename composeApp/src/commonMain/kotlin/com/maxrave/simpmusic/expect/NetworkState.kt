package com.maxrave.simpmusic.expect

import kotlinx.coroutines.flow.Flow

/**
 * Emits once each time the device gets a working connection (on Android: a network the system has
 * validated, i.e. it actually reaches the internet, not merely associated). It never emits for the
 * connection that already exists when collection starts.
 *
 * Used to retry what failed while the phone had no signal — right after boot the radio is often
 * still coming up, so the first requests fail and nothing would try again on its own.
 */
expect fun networkBecameAvailable(): Flow<Unit>
