package com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils

import android.content.Context
import com.google.android.material.textfield.TextInputEditText
import com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.R

object ValidationUtils {
    /**
     * Validates the content of a URL input field.
     *
     * @param context The application context, required to resolve string resources.
     * @param url The URL string to be validated.
     * @param urlEditText The TextInputEditText widget that will display the error message if validation fails.
     *
     * @return Returns 'true' if the URL passed validation, otherwise 'false'.
     */
    fun validateUrl(
        context: Context,
        url: String,
        urlEditText: TextInputEditText
    ): Boolean {
        if (url.isBlank()) {
            urlEditText.error = context.getString(R.string.empty_field)
            return false
        }

        if (!isHttpUrl(url)) {
            urlEditText.error = context.getString(R.string.field_incorrect_url)
            return false
        }

        urlEditText.error = null
        return true
    }

    /**
     * Validates the content of a brand guid input field.
     *
     * @param context The application context, required to resolve string resources.
     * @param xDrmBrandGuid The xDrmBrandGuid string to be validated.
     * @param xDrmBrandGuidEditText The TextInputEditText widget that will display the error message if validation fails.
     *
     * @return Returns 'true' if the brand guid passed validation, otherwise 'false'.
     */
    fun validateXDrmBrandGuid(
        context: Context,
        xDrmBrandGuid: String,
        xDrmBrandGuidEditText: TextInputEditText
    ): Boolean {
        if (xDrmBrandGuid.isBlank()) {
            xDrmBrandGuidEditText.error = context.getString(R.string.empty_field)
            return false
        }

        if (xDrmBrandGuid.length != 36) {
            xDrmBrandGuidEditText.error = context.getString(R.string.field_incorrect_length)
            return false
        }

        xDrmBrandGuidEditText.error = null
        return true
    }

    /**
     * Validates the content of a user token input field.
     *
     * @param context The application context, required to resolve string resources.
     * @param xDrmUserToken The xDrmUserToken string to be validated.
     * @param xDrmUserTokenEditText The TextInputEditText widget that will display the error message if validation fails.
     *
     * @return Returns 'true' if the user token passed validation, otherwise 'false'.
     */
    fun validateXDrmUserToken(
        context: Context,
        xDrmUserToken: String,
        xDrmUserTokenEditText: TextInputEditText
    ): Boolean {
        if (xDrmUserToken.isBlank()) {
            xDrmUserTokenEditText.error = context.getString(R.string.empty_field)
            return false
        }

        xDrmUserTokenEditText.error = null
        return true
    }

    /** Checks if the given URL string starts with a "http://" or "https://" prefix. */
    private fun isHttpUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }
}