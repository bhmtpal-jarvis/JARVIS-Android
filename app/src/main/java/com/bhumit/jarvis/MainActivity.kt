package com.bhumit.jarvis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var message: TextView
    private lateinit var speech: TextToSpeech
    private var recognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speech = TextToSpeech(this, this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 60, 40, 40)
            setBackgroundColor(Color.BLACK)
        }

        val title = TextView(this).apply {
            text = "JARVIS"
            textSize = 42f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
        }

        val status = TextView(this).apply {
            text = "● SYSTEM ONLINE"
            textSize = 18f
            setTextColor(Color.GREEN)
            gravity = Gravity.CENTER
        }

        message = TextView(this).apply {
            text = "Good evening, sir.\nI am ready."
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 60, 0, 60)
        }

        val talkButton = Button(this).apply {
            text = "🎙  TALK TO JARVIS"
            textSize = 18f
            setOnClickListener {
                startListening()
            }
        }

        root.addView(title)
        root.addView(status)
        root.addView(message)
        root.addView(talkButton)

        setContentView(root)
    }

    private fun startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            message.text = "Speech recognition is not available."
            return
        }

        message.text = "Listening..."

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)

        recognizer?.setRecognitionListener(
            object : android.speech.RecognitionListener {

                override fun onResults(results: Bundle?) {
                    val command = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.lowercase(Locale.getDefault())

                    if (command != null) {
                        message.text = "You said:\n$command"
                        handleCommand(command)
                    }
                }

                override fun onError(error: Int) {
                    message.text = "I couldn't understand that. Try again."
                }

                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            }
        )

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
            )
        }

        recognizer?.startListening(intent)
    }

    private fun handleCommand(command: String) {

        when {
            command.contains("hello") ||
            command.contains("hi jarvis") -> {
                respond("Hello, sir. How can I help you?")
            }

            command.contains("who are you") -> {
                respond("I am JARVIS, your personal Android assistant.")
            }

            command.contains("how are you") -> {
                respond("All systems are operational, sir.")
            }

            command.contains("time") -> {
                val time = SimpleDateFormat(
                    "h:mm a",
                    Locale.getDefault()
                ).format(Date())

                respond("The current time is $time.")
            }

            command.contains("open settings") -> {
                respond("Opening settings.")
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }

            command.contains("thank you") ||
            command.contains("thanks") -> {
                respond("You're welcome, sir.")
            }

            else -> {
                respond(
                    "I heard you say $command. " +
                    "I don't know that command yet."
                )
            }
        }
    }

    private fun respond(text: String) {
        message.text = text
        speech.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "JARVIS_REPLY"
        )
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            speech.language = Locale.getDefault()
        }
    }

    override fun onDestroy() {
        recognizer?.destroy()
        speech.stop()
        speech.shutdown()
        super.onDestroy()
    }
}
