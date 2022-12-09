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
    val PUBLIC_KEY_MODULUS = // START_GITHUB
        byteArrayOf(
            0x00.toByte(), 0xa0.toByte(), 0x86.toByte(), 0x90.toByte(), 0xbe.toByte(), 0x3a.toByte(), 0x7d.toByte(), 0xfd.toByte(), 0x3d.toByte(), 0x84.toByte(),
            0x56.toByte(), 0x38.toByte(), 0x23.toByte(), 0x97.toByte(), 0xd4.toByte(), 0xb6.toByte(), 0x5f.toByte(), 0xeb.toByte(), 0x1e.toByte(), 0xc0.toByte(),
            0x17.toByte(), 0x5a.toByte(), 0xb3.toByte(), 0x08.toByte(), 0x92.toByte(), 0x3b.toByte(), 0x2a.toByte(), 0x2b.toByte(), 0x6c.toByte(), 0xf6.toByte(),
            0x71.toByte(), 0xd6.toByte(), 0x62.toByte(), 0x1c.toByte(), 0x7a.toByte(), 0x4f.toByte(), 0x96.toByte(), 0xf9.toByte(), 0x37.toByte(), 0xa0.toByte(),
            0x77.toByte(), 0xd6.toByte(), 0x24.toByte(), 0x27.toByte(), 0x84.toByte(), 0x98.toByte(), 0xfa.toByte(), 0x7c.toByte(), 0xb9.toByte(), 0x3c.toByte(),
            0xfd.toByte(), 0xc9.toByte(), 0x58.toByte(), 0xcd.toByte(), 0xb7.toByte(), 0x04.toByte(), 0x08.toByte(), 0xbb.toByte(), 0x0b.toByte(), 0x23.toByte(),
            0x8b.toByte(), 0x21.toByte(), 0xaa.toByte(), 0x4d.toByte(), 0x2c.toByte(), 0xfd.toByte(), 0x19.toByte(), 0xf6.toByte(), 0xa9.toByte(), 0xc9.toByte(),
            0x43.toByte(), 0xe0.toByte(), 0xe9.toByte(), 0x63.toByte(), 0xcc.toByte(), 0xa8.toByte(), 0x5e.toByte(), 0x8c.toByte(), 0xf4.toByte(), 0x57.toByte(),
            0x02.toByte(), 0x13.toByte(), 0x44.toByte(), 0x0b.toByte(), 0xfc.toByte(), 0x0d.toByte(), 0x5d.toByte(), 0x05.toByte(), 0xbf.toByte(), 0x70.toByte(),
            0xe2.toByte(), 0xac.toByte(), 0xad.toByte(), 0xe9.toByte(), 0x55.toByte(), 0x85.toByte(), 0x04.toByte(), 0x61.toByte(), 0xfc.toByte(), 0x67.toByte(),
            0x25.toByte(), 0xe8.toByte(), 0xd2.toByte(), 0x0f.toByte(), 0xba.toByte(), 0x0b.toByte(), 0x62.toByte(), 0x1a.toByte(), 0x1d.toByte(), 0x55.toByte(),
            0xa0.toByte(), 0x6c.toByte(), 0x08.toByte(), 0x83.toByte(), 0xde.toByte(), 0xd4.toByte(), 0xbe.toByte(), 0x39.toByte(), 0x95.toByte(), 0xe6.toByte(),
            0x7b.toByte(), 0xe6.toByte(), 0xc9.toByte(), 0x44.toByte(), 0x9b.toByte(), 0xf8.toByte(), 0x54.toByte(), 0xb8.toByte(), 0x4e.toByte(), 0xe3.toByte(),
            0x75.toByte(), 0xa6.toByte(), 0xaf.toByte(), 0xfa.toByte(), 0x89.toByte(), 0x39.toByte(), 0x3e.toByte(), 0xaf.toByte(), 0xfd.toByte(), 0x4e.toByte(),
            0xf7.toByte(), 0xd8.toByte(), 0x2f.toByte(), 0x80.toByte(), 0x0d.toByte(), 0xa9.toByte(), 0x7c.toByte(), 0xf7.toByte(), 0xa7.toByte(), 0x53.toByte(),
            0x1d.toByte(), 0x18.toByte(), 0x95.toByte(), 0x6a.toByte(), 0x35.toByte(), 0x98.toByte(), 0x48.toByte(), 0x24.toByte(), 0xcf.toByte(), 0x29.toByte(),
            0x52.toByte(), 0xd7.toByte(), 0x5f.toByte(), 0xe0.toByte(), 0x6b.toByte(), 0xce.toByte(), 0x61.toByte(), 0xe4.toByte(), 0x71.toByte(), 0x13.toByte(),
            0xd6.toByte(), 0x82.toByte(), 0xf3.toByte(), 0xd9.toByte(), 0x41.toByte(), 0x74.toByte(), 0x5f.toByte(), 0x5b.toByte(), 0x85.toByte(), 0xc6.toByte(),
            0x56.toByte(), 0xa6.toByte(), 0x1f.toByte(), 0x8b.toByte(), 0xd2.toByte(), 0xc4.toByte(), 0xa7.toByte(), 0x57.toByte(), 0x9c.toByte(), 0xed.toByte(),
            0x82.toByte(), 0xca.toByte(), 0x2f.toByte(), 0xd7.toByte(), 0x84.toByte(), 0x47.toByte(), 0x26.toByte(), 0x65.toByte(), 0x43.toByte(), 0xd9.toByte(),
            0x76.toByte(), 0x95.toByte(), 0xf5.toByte(), 0x20.toByte(), 0xd1.toByte(), 0x03.toByte(), 0xf4.toByte(), 0xeb.toByte(), 0x00.toByte(), 0x34.toByte(),
            0x19.toByte(), 0xca.toByte(), 0x40.toByte(), 0x40.toByte(), 0x34.toByte(), 0xe2.toByte(), 0xfb.toByte(), 0xbd.toByte(), 0xe3.toByte(), 0x64.toByte(),
            0x02.toByte(), 0xcb.toByte(), 0xe7.toByte(), 0x1b.toByte(), 0x87.toByte(), 0x69.toByte(), 0xac.toByte(), 0x3b.toByte(), 0x7a.toByte(), 0xae.toByte(),
            0x51.toByte(), 0x3d.toByte(), 0x4b.toByte(), 0x32.toByte(), 0x57.toByte(), 0x24.toByte(), 0xe2.toByte(), 0x03.toByte(), 0x34.toByte(), 0x71.toByte(),
            0x10.toByte(), 0xda.toByte(), 0x60.toByte(), 0x77.toByte(), 0x48.toByte(), 0x26.toByte(), 0xcb.toByte(), 0x3c.toByte(), 0x63.toByte(), 0x0b.toByte(),
            0xa9.toByte(), 0x49.toByte(), 0xa4.toByte(), 0x92.toByte(), 0x53.toByte(), 0x69.toByte(), 0x53.toByte()
        )
    // END_GITHUB

    val PUBLIC_KEY_EXPONENT = // START_GITHUB
        byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x01.toByte())
    // END_GITHUB
    // endregion

    // region IdCloud Risk
    const val ND_URL = ""
    const val ND_CLIENT_ID = ""
    const val RISK_URL = ""
    // endregion

    // region Attestation key
    const val ATTESTATION_KEY = ""
    // endregion
}