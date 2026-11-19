package com.nexus.gamelauncher.ui

import java.util.concurrent.atomic.AtomicBoolean

internal class ColdStart {
    private val entered = AtomicBoolean()

    fun claim(): Boolean = entered.compareAndSet(false, true)
}
