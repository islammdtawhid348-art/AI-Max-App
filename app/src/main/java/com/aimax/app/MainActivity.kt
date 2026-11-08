package com.aimax.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var status: TextView
    private var rec: SpeechRecognizer? = null

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        tts = TextToSpeech(this, this)
        status = TextView(this).apply { text = "AI MAX প্রস্তুত"; textSize = 20f; gravity = Gravity.CENTER }
        val btn = Button(this).apply { text = "বলুন"; textSize = 24f; setOnClickListener { listen() } }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            addView(status)
            addView(btn)
        }
        setContentView(box)
    }

    override fun onInit(s: Int) { tts.language = Locale("bn", "BD") }

    private fun speak(t: String) {
        status.text = t
        tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, "m")
    }

    private fun listen() {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "বলুন")
        try {
            startActivityForResult(i, 100)
        } catch (e: Exception) {
            status.text = "ভয়েস সেবা পাওয়া যায়নি"
        }
    }

    override fun onActivityResult(rc: Int, res: Int, data: Intent?) {
        super.onActivityResult(rc, res, data)
        if (rc == 100 && res == RESULT_OK) {
            val t = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: ""
            status.text = "আপনি: $t"
            handle(t)
        }
    }

    private val skip = setOf("whatsapp", "sms", "message", "number", "to", "send",
        "হোয়াটসঅ্যাপে", "হোয়াটসঅ্যাপ", "হোয়াটসএপে", "হোয়াটসএপ", "এসএমএস", "মেসেজ",
        "নাম্বারে", "নাম্বার", "নম্বরে", "নম্বর", "এই", "করো", "করুন", "পাঠাও", "পাঠান",
        "লেখো", "লিখো", "দাও")

    private fun handle(raw: String) {
        var t = raw
        "০১২৩৪৫৬৭৮৯".forEachIndexed { i, c -> t = t.replace(c, '0' + i) }
        val low = t.lowercase()
        val m = Regex("\\+?\\d[\\d\\- ]{8,}\\d").find(t)
        val wa = low.contains("whatsapp") || low.contains("হোয়াটস")
        val sms = low.contains("sms") || low.contains("এসএমএস")
        if (m != null && (wa || sms)) {
            var num = m.value.filter { it.isDigit() }
            if (num.startsWith("01")) num = "88$num"
            val msg = t.removeRange(m.range).split(" ")
                .filter { it.isNotBlank() && it.lowercase() !in skip }.joinToString(" ")
            if (msg.isEmpty()) { speak("মেসেজে কী লিখবো?"); return }
            MaxService.until = System.currentTimeMillis() + 15000
            MaxService.pending = true
            val go = if (wa) Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$num?text=" + Uri.encode(msg)))
                     else Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$num")).putExtra("sms_body", msg)
            go.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            speak("মেসেজ পাঠাচ্ছি")
            try {
                if (wa) go.setPackage("com.whatsapp")
                startActivity(go)
            } catch (e: Exception) {
                go.setPackage(null)
                startActivity(go)
            }
        } else if (low.contains("messenger") || low.contains("মেসেঞ্জার")) {
            speak("মেসেঞ্জার খুলছি")
            val li = packageManager.getLaunchIntentForPackage("com.facebook.orca")
            if (li != null) startActivity(li)
            else startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://m.me")))
        } else {
            speak(when {
                low.contains("নাম") || low.contains("নাম কি") || low.contains("নাম কী") -> "আমার নাম AI MAX।"
                low.contains("কেমন আছ") -> "আমি ভালো আছি। আপনার কী খবর?"
                low.contains("সালাম") -> "ওয়ালাইকুমুস সালাম! কীভাবে সাহায্য করতে পারি?"
                low.contains("ধন্যবাদ") -> "আপনাকে স্বাগতম!"
                low.contains("তুমি কে") -> "আমি AI MAX, আপনার সহকারী।"
                else -> "বুঝতে পারিনি। শুনেছি: " + raw
            })
        }
    }
}
