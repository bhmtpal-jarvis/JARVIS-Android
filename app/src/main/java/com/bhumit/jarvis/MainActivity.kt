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
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var message: TextView
    private lateinit var core: TextView
    private lateinit var status: TextView
    private lateinit var speech: TextToSpeech
    private var recognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speech = TextToSpeech(this, this)

        val root = LinearLayout(this)
        root.setBackgroundColor(android.graphics.Color.BLACK)
        root.setPadding(32, 48, 32, 48).apply {
            root.orientation = LinearLayout.VERTICAL
            root.gravity = Gravity.CENTER
            root.setPadding(40, 60, 40, 40)
            root.setBackgroundColor(Color.BLACK)
        }

        val title = TextView(this).apply {
            text = "JARVIS"
            textSize = 42f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.CYAN)
            root.gravity = Gravity.CENTER
        }

        status = TextView(this).apply {
            text = "● SYSTEM ONLINE"
            textSize = 18f
            setTextColor(Color.GREEN)
            root.gravity = Gravity.CENTER
        }

        message = TextView(this).apply {
            text = "Good evening, sir.\nI am ready."
            textSize = 20f
            setTextColor(Color.WHITE)
            root.gravity = Gravity.CENTER
            setPadding(0, 60, 0, 60)
        }

        val talkButton = Button(this).apply {
            text = "🎙  TALK TO JARVIS"
            textSize = 18f
            setOnClickListener {
                startListening()
            }
        }

        core = TextView(this)
        core.text = "◉"
        core.textSize = 72f
        core.gravity = android.view.Gravity.CENTER
        core.setTextColor(android.graphics.Color.CYAN)
        core.setPadding(0, 40, 0, 40)

        root.addView(title)
        root.addView(core)
        root.addView(status)
        root.addView(message)
        root.addView(talkButton)

        setContentView(root)
    }

    fun setCoreState(state: String) {
        core.clearAnimation()
    
        val scale = if (state == "LISTENING") 1.25f
                    else if (state == "SPEAKING") 1.20f
                    else 1.10f
    
        val pulse = android.view.animation.ScaleAnimation(
            1.0f, scale,
            1.0f, scale,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
        )
    
        pulse.duration = if (state == "LISTENING") 350
                         else if (state == "SPEAKING") 450
                         else 900
    
        pulse.repeatMode = android.view.animation.Animation.REVERSE
        pulse.repeatCount = android.view.animation.Animation.INFINITE
        core.startAnimation(pulse)
    }
    

    private fun getWeather() {
        status.text = "CHECKING WEATHER..."
        setCoreState("LISTENING")

        Thread {
            try {
                val place = java.net.URLEncoder.encode("Dhaneriya, Rajasthan", "UTF-8")

                val geoUrl = URL("https://geocoding-api.open-meteo.com/v1/search?name=$place&count=5&countryCode=IN&language=en&format=json")
                val geoConnection = geoUrl.openConnection() as HttpURLConnection
                geoConnection.requestMethod = "GET"

                val geoText = geoConnection.inputStream.bufferedReader().use { it.readText() }
                val geoJson = JSONObject(geoText)
                val results = geoJson.getJSONArray("results")

                var latitude = 0.0
                var longitude = 0.0
                var found = false

                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    if (item.optString("admin1").equals("Rajasthan", ignoreCase = true)) {
                        latitude = item.getDouble("latitude")
                        longitude = item.getDouble("longitude")
                        found = true
                        break
                    }
                }

                if (!found) throw Exception("Location not found")

                val weatherUrl = URL("https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,precipitation&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,rain_sum,sunrise,sunset,uv_index_max&timezone=auto&forecast_days=1")

                val weatherConnection = weatherUrl.openConnection() as HttpURLConnection
                weatherConnection.requestMethod = "GET"

                val weatherText = weatherConnection.inputStream.bufferedReader().use { it.readText() }
                val weatherJson = JSONObject(weatherText)

                val current = weatherJson.getJSONObject("current")
                val daily = weatherJson.getJSONObject("daily")

                val temperature = current.getDouble("temperature_2m")
                val feelsLike = current.getDouble("apparent_temperature")
                val humidity = current.getInt("relative_humidity_2m")
                val weatherCode = current.getInt("weather_code")
                val wind = current.getDouble("wind_speed_10m")
                val rainProbability = daily.getJSONArray("precipitation_probability_max").getInt(0)
                val maxTemp = daily.getJSONArray("temperature_2m_max").getDouble(0)
                val minTemp = daily.getJSONArray("temperature_2m_min").getDouble(0)
                val rain = daily.getJSONArray("rain_sum").getDouble(0)

                val description = when (weatherCode) {
                    0 -> "clear sky"
                    1, 2 -> "partly cloudy"
                    3 -> "overcast"
                    45, 48 -> "foggy"
                    51, 53, 55, 56, 57 -> "drizzle"
                    61, 63, 65, 66, 67 -> "rainy"
                    71, 73, 75, 77 -> "snowy"
                    80, 81, 82 -> "rain showers"
                    85, 86 -> "snow showers"
                    95, 96, 99 -> "a thunderstorm"
                    else -> "mixed conditions"
                }

                val rainText = if (rain > 0.0) {
                    "Expected rainfall is %.1f millimeters.".format(Locale.US, rain)
                } else {
                    "No measurable rainfall is expected."
                }

                val answer = "Sir, the current weather in Dhaneriya is %.0f degrees Celsius with %s. It feels like %.0f degrees. Humidity is %d percent and winds are around %.0f kilometers per hour. Today's high is %.0f degrees and low is %.0f degrees. There is a %d percent chance of rain today. %s".format(Locale.US, temperature, description, feelsLike, humidity, wind, maxTemp, minTemp, rainProbability, rainText)

                runOnUiThread {
                    respond(answer)
                }

            } catch (e: Exception) {
                runOnUiThread {
                    respond("Weather error: ${e.message ?: "unknown error"}")
                }
            }
        }.start()
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
                status.text = "PROCESSING..."
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

        setCoreState("LISTENING")
        status.text = "LISTENING..."
        recognizer?.startListening(intent)
    }

    private fun handleCommand(command: String) {

        val text = command.lowercase(Locale.getDefault()).trim()

        val numbers = Regex("""\d+(?:\.\d+)?""")
            .findAll(text)
            .map { it.value.toDouble() }
            .toList()

        when {
            text.contains("weather") -> { getWeather() }

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

        setCoreState("SPEAKING")
        status.text = "SPEAKING..."

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
