package com.tally.app.di

import javax.inject.Qualifier

/** Marks the app-lifetime CoroutineScope — for fire-and-forget writes that must survive a VM/nav pop. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
