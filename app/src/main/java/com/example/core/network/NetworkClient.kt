package com.example.core.network

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Singleton configuration for secure network operations.
 * Implements strict Certificate Pinning to protect transactions and exchanges from MITM attacks.
 */
object NetworkClient {

    /**
     * Builds and returns a highly secure OkHttpClient configured with active and backup SHA-256 certificate pins
     * for open.er-api.com and api.exchangerate-api.com.
     */
    val secureOkHttpClient: OkHttpClient by lazy {
        val certificatePinner = CertificatePinner.Builder()
            // Pins for open.er-api.com (including backup intermediates and root authorities)
            .add("open.er-api.com", "sha256/mE74mQCv869gN68Lscz07/IInA67kscgG71f34k4p9U=")
            .add("open.er-api.com", "sha256/FEz75URHg4EE6ykhb6SstGgE8s98Et9S39X9I8Ld+08=")
            .add("open.er-api.com", "sha256/Y9m3XBo2+AId3bnfCgOfvB8V9kyLv7HAL699h1gIp8Y=")
            
            // Pins for api.exchangerate-api.com
            .add("api.exchangerate-api.com", "sha256/mE74mQCv869gN68Lscz07/IInA67kscgG71f34k4p9U=")
            .add("api.exchangerate-api.com", "sha256/FEz75URHg4EE6ykhb6SstGgE8s98Et9S39X9I8Ld+08=")
            .add("api.exchangerate-api.com", "sha256/Y9m3XBo2+AId3bnfCgOfvB8V9kyLv7HAL699h1gIp8Y=")
            .build()

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
