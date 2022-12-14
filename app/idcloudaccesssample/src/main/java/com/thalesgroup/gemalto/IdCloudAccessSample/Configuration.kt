package com.thalesgroup.gemalto.IdCloudAccessSample

object Configuration {
    // region IDP
    const val IDP_URL = ""
    const val REDIRECT_URL = ""
    const val CLIENT_ID = ""
    const val CLIENT_SECRET = ""
    // endregion

    // region Protector FIDO
    const val MS_URL = ""
    const val TENANT_ID = ""
    const val ATTESTATION_KEY = ""
    val PUBLIC_KEY_MODULUS = null
    val PUBLIC_KEY_EXPONENT = null
    // endregion

    // region IdCloud Risk
    const val ND_URL = ""
    const val ND_CLIENT_ID = ""
    const val RISK_URL = ""
    // endregion
}
