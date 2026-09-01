package com.lagradost.cloudstream3.plugins

import android.content.Context
import com.lagradost.cloudstream3.MainAPI

interface BasePlugin

abstract class Plugin : BasePlugin {
    open fun load(context: Context) {}
    open fun registerMainAPI(api: MainAPI) {}
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CloudstreamPlugin
