package no.boostai.sdk.ChatBackend

import okhttp3.CertificatePinner

object BoostCertificatePinner {
    fun getCertificatePinner(): CertificatePinner {
        // Pin against AWS Root CAs
        // https://square.github.io/okhttp/3.x/okhttp/okhttp3/CertificatePinner.html
        // https://weidianhuang.medium.com/how-to-calculate-certificate-pin-for-okhttp-1fba86b2c5f1
        return CertificatePinner.Builder()
            .add("*.boost.ai", "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=") // Amazon Root CA 1
            .add("*.boost.ai", "sha256/f0KW/FtqTjs108NpYj42SrGvOB2PpxIVM8nWxjPqJGE=") // Amazon Root CA 2
            .add("*.boost.ai", "sha256/NqvDJlas/GRcYbcWE8S/IceH9cq77kg0jVhZeAPXq8k=") // Amazon Root CA 3
            .add("*.boost.ai", "sha256/9+ze1cZgR9KO1kZrVDxA4HQ6voHRCSVNz4RdTCx4U8U=") // Amazon Root CA 4
            .add("*.boost.ai", "sha256/KwccWaCgrnaw6tsrrSO61FgLacNgG2MMLq8GE6+oP5I=") // Starfield Services Root Certificate Authority - G2
            .build()

    }

}