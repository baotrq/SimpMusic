package com.maxrave.simpmusic.expect

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.koin.mp.KoinPlatform.getKoin

actual fun networkBecameAvailable(): Flow<Unit> =
    callbackFlow {
        val connectivity = getKoin().get<Context>().getSystemService(ConnectivityManager::class.java)
        // Networks already validated when collection starts: their first callback is not news.
        val validated = mutableSetOf<Network>()
        var primed = false
        val callback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities,
                ) {
                    val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    if (!isValidated) {
                        validated.remove(network)
                        return
                    }
                    if (validated.add(network) && primed) trySend(Unit)
                }

                override fun onLost(network: Network) {
                    validated.remove(network)
                }
            }
        connectivity.activeNetwork?.let { active ->
            if (connectivity.getNetworkCapabilities(active)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true) {
                validated.add(active)
            }
        }
        primed = true
        connectivity.registerDefaultNetworkCallback(callback)
        awaitClose { connectivity.unregisterNetworkCallback(callback) }
    }
