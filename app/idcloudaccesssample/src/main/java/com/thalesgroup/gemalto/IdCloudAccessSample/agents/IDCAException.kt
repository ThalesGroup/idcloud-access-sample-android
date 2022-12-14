package com.thalesgroup.gemalto.IdCloudAccessSample.agents

import com.thales.dis.mobile.idcloud.auth.exception.IdCloudClientException
import net.openid.appauth.AuthorizationException

class IDCAException : Exception {
    private var errorDescription: String? = null
    private var error: String? = null

    constructor(
        authorizationException: AuthorizationException
    ) : super("error=${ERROR_CODE_IDCA.UNKNOWN.name}&error_description=${authorizationException.errorDescription}") {
        this.errorDescription = authorizationException.errorDescription
        this.error = ERROR_CODE_IDCA.UNKNOWN.name
    }

    constructor(
        idCloudClientException: IdCloudClientException
    ) : super("error=${mapErrorCode(idCloudClientException.integerCode).name}&error_description=${idCloudClientException.error.message}") {
        this.errorDescription = idCloudClientException.error.message
        this.error = mapErrorCode(idCloudClientException.integerCode).name
    }

    constructor(
        error: String,
        errorDescription: String
    ) : super("error=$error&error_description=$errorDescription") {
        this.error = error
        this.errorDescription = errorDescription
    }

    companion object {
        fun mapErrorCode(integerCode: Int?): ERROR_CODE_IDCA {
            return when (integerCode) {
                IdCloudClientException.ErrorCode.USER_CANCELLED.code -> ERROR_CODE_IDCA.CANCELLED
                else -> {
                    ERROR_CODE_IDCA.SCA
                }
            }
        }
    }

    enum class ERROR_CODE_IDCA {
        UNKNOWN, CANCELLED, ACCESS_DENIED, SCA
    }

    fun getIDCAErrorDescription(): String? {
        return errorDescription
    }

    fun getIDCAError(): String? {
        return error
    }
}
