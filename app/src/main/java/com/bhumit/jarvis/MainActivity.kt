package com.bhumit.jarvis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.os.BatteryManager
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

                    android.os.Handler(android.os.Looper.getMainLooper())
                        .postDelayed({
                            if (!isFinishing) {
                                startListening()
                            }
                        }, 700)
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

        val text = command.lowercase(Locale.getDefault()).trim()

        val numbers = Regex("""\d+(?:\.\d+)?""")
            .findAll(text)
            .map { it.value.toDouble() }
            .toList()

        when {
            text.contains("hello") ||
            text.contains("hi jarvis") ||
            text.contains("hey jarvis") -> {
                respond("Hello, sir. How can I help you?")
            }

            text.contains("who are you") ||
            text.contains("what are you") ||
            text.contains("introduce yourself") -> {
                respond("I am JARVIS, your personal Android assistant.")
            }

            text.contains("what can you do") ||
            text.contains("your capabilities") -> {
                respond("I can recognize your voice, tell you the time and date, check your battery, perform basic calculations, and open Android settings.")
            }

            text.contains("battery") -> {
                val batteryManager =
                    getSystemService(BATTERY_SERVICE) as BatteryManager

                val battery = batteryManager.getIntProperty(
                    BatteryManager.BATTERY_PROPERTY_CAPACITY
                )

                respond("Your battery level is $battery percent.")
            }

            text.contains("time") -> {
                val time = SimpleDateFormat(
                    "h:mm a",
                    Locale.getDefault()
                ).format(Date())

                respond("The current time is $time.")
            }

            text.contains("date") ||
            text.contains("today") -> {
                val date = SimpleDateFormat(
                    "EEEE, d MMMM yyyy",
                    Locale.getDefault()
                ).format(Date())

                respond("Today is $date.")
            }

            (text.contains("plus") || text.contains("+")) && numbers.size >= 2 -> {
                val result = numbers[0] + numbers[1]
                respond("${formatNumber(numbers[0])} plus ${formatNumber(numbers[1])} equals ${formatNumber(result)}.")
            }

            (text.contains("minus") || text.contains("-")) && numbers.size >= 2 -> {
                val result = numbers[0] - numbers[1]
                respond("${formatNumber(numbers[0])} minus ${formatNumber(numbers[1])} equals ${formatNumber(result)}.")
            }

            (text.contains("times") ||
             text.contains("multiply") ||
             text.contains("multiplied") ||
             text.contains("*")) && numbers.size >= 2 -> {
                val result = numbers[0] * numbers[1]
                respond("${formatNumber(numbers[0])} multiplied by ${formatNumber(numbers[1])} equals ${formatNumber(result)}.")
            }

            (text.contains("divided") ||
             text.contains("divide") ||
             text.contains("/")) && numbers.size >= 2 -> {
                if (numbers[1] == 0.0) {
                    respond("I cannot divide by zero.")
                } else {
                    val result = numbers[0] / numbers[1]
                    respond("${formatNumber(numbers[0])} divided by ${formatNumber(numbers[1])} equals ${formatNumber(result)}.")
                }
            }

            text.contains("open wifi") ||
            text.contains("open wi-fi") ||
            text.contains("wifi settings") ||
            text.contains("wi-fi settings") -> {
                respond("Opening Wi-Fi settings.")
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }

            text.contains("open bluetooth") ||
            text.contains("bluetooth settings") -> {
                respond("Opening Bluetooth settings.")
                startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            }

            text.contains("open display") ||
            text.contains("display settings") -> {
                respond("Opening display settings.")
                startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
            }

            text.contains("open sound") ||
            text.contains("sound settings") -> {
                respond("Opening sound settings.")
                startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
            }

            text.contains("open settings") ||
            text == "settings" -> {
                respond("Opening settings.")
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }

            text.contains("thank you") ||
            text.contains("thanks") -> {
                respond("You're welcome, sir.")
            }

            text.contains("good morning") -> {
                respond("Good morning, sir. JARVIS is online and ready.")
            }

            text.contains("good evening") -> {
                respond("Good evening, sir. JARVIS is online and ready.")
            }

            else -> {
                respond("I heard you say $command. I don't know that command yet.")
            }
        }
    }

    private fun formatNumber(number: Double): String {
        return if (number % 1.0 == 0.0) {
            number.toLong().toString()
        } else {
            number.toString()
        }
    }

    private fun respond(text: String) {
        message.text = text

        speech.setOnUtteranceProgressListener(
            object : android.speech.tts.UtteranceProgressListener() {

                override fun onStart(utteranceId: String?) {
                }

                override fun onDone(utteranceId: String?) {
                    runOnUiThread {
                        android.os.Handler(android.os.Looper.getMainLooper())
                            .postDelayed({
                                if (!isFinishing) {
                                    startListening()
                                }
                            }, 500)
                    }
                }

                override fun onError(utteranceId: String?) {
                }
            }
        )

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
