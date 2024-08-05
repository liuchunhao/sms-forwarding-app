package com.example.mysmsapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Vibrator
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.mysmsapp.databinding.ActivityMainBinding
import com.example.mysmsapp.service.ApiService
import com.example.mysmsapp.service.HeartbeatWorker
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    companion object {
        lateinit var instance: MainActivity
    }

    /* How to get phone number of this device
        - https://stackoverflow.com/questions/2480288/programmatically-obtain-the-phone-number-of-the-android-phone

     */

    private var tMgr: TelephonyManager? = null
    private var mPhoneNumber: String? = null   // Phone number of this device (SIM card)
    private var apiEndpoint: String = "http://ec2-3-112-51-9.ap-northeast-1.compute.amazonaws.com:5000"

    private var buttonSettings: Button? = null
    private var buttonSend: Button? = null
    private var textView: TextView? = null
    private var textViewPhoneNo: TextView? = null
    private var textViewApiEndpoint: TextView? = null
    private var textViewLogging: TextView? = null

    /* Handler to update textview from another thread */
    private val handler = Handler(Looper.getMainLooper()){
        val bundle = it.data
        val from = bundle.getString("from")
        val message = bundle.getString("message")
        textView?.text = "Message: $message\nFrom: $from"
        true
    }

    /* Handler to update textview from another thread */
    private val loggingHandler = Handler(Looper.getMainLooper()){
        val bundle = it.data
        val timestamp = bundle.getString("timestamp")
        val action = bundle.getString("action")
        val message = bundle.getString("message")
        textViewLogging?.text = "> ${timestamp}|${action}:\n${message}"
        true
    }

    /* SMS */
    private lateinit var binding: ActivityMainBinding

    private val readPermission = this.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        Log.d("MainActivity", "Permission granted: $it")
    }

    /* SMS Receiver */
    private val smsReceiver = object : SMSReceiver(){ }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        instance = this

        Log.d("MainActivity", "onCreate")

        textView = findViewById(R.id.textView)
        textView?.text = getString(R.string.textview_sms_prompt_msg)

        textViewLogging = findViewById(R.id.textViewLogging)

        textViewPhoneNo = findViewById(R.id.textViewPhoneNo)

        textViewApiEndpoint = findViewById(R.id.textViewApiEndpoint)
        textViewApiEndpoint?.text = apiEndpoint

        buttonSettings = findViewById(R.id.buttonSettings);
        buttonSettings?.setOnClickListener {
            Intent (this, SettingsActivity::class.java).also {
                startActivity(it)
                Log.d("Settings", "Start SettingsActivity")
            }
        }

        buttonSend = findViewById(R.id.buttonSend);
        buttonSend?.setOnClickListener {
            sendPostSmsRequest(it, mPhoneNumber, "Your Exness verification code is: 9999999")
            // sendPostSmsRequest(it, "0958521505", "Your Binance verification code is: 7777777", "http://192.168.1.76:5000/api/v1/exness/sms")
        }

        /* SMS Permission */
        binding = ActivityMainBinding.inflate(layoutInflater)   // Inflate the layout? What does it mean? https://stackoverflow.com/questions/3482743/what-does-it-mean-to-inflate-a-view-from-an-xml-file
        readPermission.launch(                                  // Request the permissions
            arrayOf(
                android.Manifest.permission.RECEIVE_SMS,
                android.Manifest.permission.READ_SMS,

                android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.READ_PHONE_NUMBERS,
            )
        )

        /* SMSReceiver */
        val filter = IntentFilter("sms-received")           // what is this for ? https://developer.android.com/reference/android/content/IntentFilter
        registerReceiver(smsReceiver, filter, RECEIVER_EXPORTED)   // for security reason, it's a must to set the receiver as non-exported

        /* ApiService */
        val serviceIntent = Intent(this, ApiService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        /* Get the phone number of this device */
        this.getSystemService(Context.TELEPHONY_SERVICE)?.let {
            if (ActivityCompat.checkSelfPermission( this, Manifest.permission.READ_SMS ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission( this, Manifest.permission.READ_PHONE_NUMBERS ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission( this, Manifest.permission.READ_PHONE_STATE ) != PackageManager.PERMISSION_GRANTED ) {

                // TODO: Consider calling ActivityCompat#requestPermissions

                /*
                   here to request the missing permissions, and then overriding
                   public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
                   to handle the case where the user grants the permission. See the documentation for ActivityCompat#requestPermissions for more details.

                */
                Log.d("MainActivity", "No permission to read phone number")
                return
            }

            tMgr = it as TelephonyManager
            mPhoneNumber = tMgr?.line1Number
            textViewPhoneNo?.text = mPhoneNumber

            Log.d("MainActivity", "Phone Number: $mPhoneNumber")
        }

        /** Set up WorkManager & add job into WorkManager */
        val heartbeatRequest: WorkRequest =
            PeriodicWorkRequest.Builder(HeartbeatWorker::class.java, 15, TimeUnit.MINUTES)
                .build()

        WorkManager.getInstance(this).enqueue(heartbeatRequest)
    }

    fun getMobileNumber(): String? {
        return mPhoneNumber
    }

    override fun onDestroy() {
        /** Unregister BroadcastReceiver */
        unregisterReceiver(smsReceiver)

        /** Stop the ApiService when the Activity is destroyed */
        val serviceIntent = Intent(this, ApiService::class.java)
        stopService(serviceIntent)

        super.onDestroy()
    }

    private fun sendPostSmsRequest(it: View?, from: String?, message: String, endpoint: String = "http://ec2-3-112-51-9.ap-northeast-1.compute.amazonaws.com:5000/api/v1/exness/sms") {
        val client = OkHttpClient()
        val timestamp = java.time.LocalDateTime.now().toString()        // yyyy-mm-dd hh:mm:ss
        val json = """
            {
                "timestamp": "$timestamp",
                "mobile": "${from?: getMobileNumber()}",
                "msg": "$message"
            }
            """.trimIndent().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(endpoint)
            .post(json)
            .build()

        Log.i("MainActivity", "sendPostSmsRequest: $json")
        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                Log.e("MainActivity", "onFailure: ${e.message}")
                setTextView(endpoint, "Failed to send POST request: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                Log.d("MainActivity", "onResponse: $body")
                setTextView(endpoint, "Received POST response: $body")
            }

        })
    }

    /** Send GET request
    private fun sendPingRequest(view: View) {
        val endpoint = "http://ec2-3-112-51-9.ap-northeast-1.compute.amazonaws.com:5000/api/v1/exness/ping"
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(endpoint)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MainActivity", "onFailure: ${e.message}")
                setTextView(endpoint, "Failed to send GET request: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                Log.d("MainActivity", "onResponse: $body")
                setTextView(endpoint, "Received GET response: $body")
            }
        })
    }
     */

    /** set textview from another thread */
    fun setTextView(from: String, message: String) {
        handler.sendMessage(Message().apply {
            data = Bundle().apply {
                putString("from", from)
                putString("message", message)
            }
        })
        playAlertSoundAndVibrate()
    }

    /** set logging textview from another thread */
    fun setLoggingTextView(timestamp: String, action: String, message: String) {
        loggingHandler.sendMessage(Message().apply {
            data = Bundle().apply {
                putString("timestamp", timestamp)
                putString("action", action)
                putString("message", message)
            }
        })
    }

    /** Make vibration and sound notification */
    fun playAlertSoundAndVibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(1000)
        // disable vibration
        // vibrator.cancel()

        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(applicationContext, notification)
        ringtone.play()
    }

}