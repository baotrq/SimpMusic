package com.maxrave.simpmusic.carmode

import android.content.Context
import coil3.PlatformContext
import org.koin.mp.KoinPlatform.getKoin

actual fun imageContext(): PlatformContext = getKoin().get<Context>()
