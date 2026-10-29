package com.gk.englishtamildictionary

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.common.model.DownloadConditions
import kotlinx.coroutines.tasks.await

/**
 * ML Kit On-Device Translation, English -> Tamil.
 * The ~30 MB Tamil model is downloaded once (needs internet); afterwards everything runs offline.
 */
object TamilTranslator {
    private val translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.TAMIL)
            .build()
    )

    /** Returns true when the model is on the device and ready. */
    suspend fun ensureModel(): Boolean = try {
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
        true
    } catch (e: Exception) {
        false
    }

    suspend fun translate(text: String): String? = try {
        translator.translate(text).await()
    } catch (e: Exception) {
        null
    }
}
