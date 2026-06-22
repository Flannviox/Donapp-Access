package com.grupo3.donapp_access.core.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

object VoiceAssistantManager : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this)
            prefs = context.applicationContext.getSharedPreferences("AccesibilidadPrefs", Context.MODE_PRIVATE)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locale = Locale("es", "ES")
            val result = tts?.setLanguage(locale)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("VoiceAssistant", "Idioma no soportado en este dispositivo")
            } else {
                isInitialized = true
            }
        } else {
            Log.e("VoiceAssistant", "Fallo al inicializar TextToSpeech")
        }
    }

    // Función principal para hablar
    fun speak(text: String) {
        // Revisamos si el usuario lo activó en Accesibilidad
        val isEnabled = prefs?.getBoolean("voice_assistant_enabled", false) ?: false

        if (!isEnabled || !isInitialized) return

        // Detenemos cualquier audio anterior para que no se crucen las voces
        tts?.stop()

        // Leemos el volumen configurado (de 0.0 a 1.0), por defecto 1.0 (máximo)
        val volume = prefs?.getFloat("voice_volume", 1.0f) ?: 1.0f

        val params = Bundle()
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)

        // QUEUE_FLUSH corta lo que esté diciendo y empieza a decir lo nuevo
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, null)
    }

    // Por si necesitamos callarlo a la fuerza
    fun stop() {
        tts?.stop()
    }
}