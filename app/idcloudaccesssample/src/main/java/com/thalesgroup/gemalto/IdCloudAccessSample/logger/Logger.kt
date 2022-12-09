package com.thalesgroup.gemalto.IdCloudAccessSample.logger

interface Logger {
    fun log(text: String?)
    val logs: List<String?>?
}
