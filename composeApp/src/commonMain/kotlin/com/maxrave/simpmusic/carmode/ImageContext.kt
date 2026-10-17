package com.maxrave.simpmusic.carmode

import coil3.PlatformContext

/** The context Coil's singleton loader is keyed by, for prefetching covers outside the UI. */
expect fun imageContext(): PlatformContext
