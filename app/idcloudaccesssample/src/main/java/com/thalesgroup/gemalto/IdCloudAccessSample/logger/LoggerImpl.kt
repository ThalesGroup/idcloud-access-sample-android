package com.thalesgroup.gemalto.IdCloudAccessSample.logger

import java.util.ArrayList

open class LoggerImpl : Logger {
    private val items: MutableList<String?>

    init {
        items = ArrayList()
    }

    override fun log(text: String?) {
        items.add(text)
    }

    override val logs: List<String?>
        get() = items

    fun clear() = items.clear()
}
