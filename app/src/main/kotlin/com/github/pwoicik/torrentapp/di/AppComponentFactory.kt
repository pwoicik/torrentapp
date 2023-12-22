package com.github.pwoicik.torrentapp.di

import android.app.Activity
import android.app.Service
import android.content.Intent
import androidx.core.app.AppComponentFactory
import com.github.pwoicik.torrentapp.App
import kotlin.reflect.KClass

@Suppress("unused")
class AppComponentFactory : AppComponentFactory() {
    private lateinit var activities: Map<KClass<out Activity>, () -> Activity>
    private lateinit var services: Map<KClass<out Service>, () -> Service>

    override fun instantiateApplicationCompat(cl: ClassLoader, className: String) =
        (super.instantiateApplicationCompat(cl, className) as App)
            .also {
                activities = it.appComponent.activities
                services = it.appComponent.services
            }

    override fun instantiateActivityCompat(
        cl: ClassLoader,
        className: String,
        intent: Intent?,
    ) = activities[cl.loadClass(className)!!.kotlin]!!()

    override fun instantiateServiceCompat(
        cl: ClassLoader,
        className: String,
        intent: Intent?,
    ) = services[cl.loadClass(className)!!.kotlin]?.invoke()
        ?: super.instantiateServiceCompat(cl, className, intent)
}
