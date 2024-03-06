package com.example.mysmsapp

import android.Manifest
import android.content.BroadcastReceiver
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
import com.example.mysmsapp.databinding.ActivityMainBinding
import com.example.mysmsapp.service.ApiService
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class MainActivity : AppCompatActivity () , View.OnClickListener {

    companion object {
        lateinit var instance: MainActivity
    }

    // how to get phone number of this device
    // https://stackoverflow.com/questions/2480288/programmatically-obtain-the-phone-number-of-the-android-phone

    var tMgr: TelephonyManager? = null
    var mPhoneNumber: String? = null

    private var buttonSettings: Button? = null
    private var buttonSend: Button? = null
    private var textView: TextView? = null

    // use handler to update textview from another thread
    private val handler = Handler(Looper.getMainLooper()){
        val bundle = it.data
        val from = bundle.getString("from")
        val message = bundle.getString("message")
        textView?.text = "Message: $message\nFrom: $from"
        true
    }

    /* SMS */
    private lateinit var binding: ActivityMainBinding
    private val readPermission = this.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    /* SMS Receiver */
    private val smsReceiver = object : SMSReceiver(){ }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        instance = this

        Log.d("MainActivity", "onCreate")

        textView = findViewById(R.id.textView)
        textView?.text = "SMS Content will be displayed here."

        buttonSettings = findViewById(R.id.buttonSettings);
        buttonSettings?.setOnClickListener {
            Intent (this, SettingsActivity::class.java).also {
                startActivity(it)
                Log.d("Settings", "Start SettingsActivity")
            }
        }

        buttonSend = findViewById(R.id.buttonSend);
        buttonSend?.setOnClickListener {
            sendPostSmsRequest(it, "0958521505", "Your Exness verification code is: 9999999")
        }

        /* SMS */
        binding = ActivityMainBinding.inflate(layoutInflater)   // Inflate the layout
        readPermission.launch(                                  // Request the permissions
            arrayOf(
                android.Manifest.permission.RECEIVE_SMS,
                android.Manifest.permission.READ_SMS
            )
        )

        /* registering SMSReceiver */
        val filter = IntentFilter("sms-received")
        registerReceiver(smsReceiver, filter, RECEIVER_EXPORTED)  // for security reason, it's a must to set the receiver as non-exported


        /* Start the ApiService */
        val serviceIntent = Intent(this, ApiService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        /* Get the phone number of this device */
        this.getSystemService(Context.TELEPHONY_SERVICE)?.let {
            if (ActivityCompat.checkSelfPermission( this, Manifest.permission.READ_SMS ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission( this, Manifest.permission.READ_PHONE_NUMBERS ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission( this, Manifest.permission.READ_PHONE_STATE ) != PackageManager.PERMISSION_GRANTED ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Log.d("MainActivity", "No permission to read phone number")
                return
            }
            tMgr = it as TelephonyManager
            mPhoneNumber = tMgr?.line1Number

            Log.d("MainActivity", "Phone Number: $mPhoneNumber")
        }
    }

    override fun onDestroy() {
        /* unregister BroadcastReceiver */
        unregisterReceiver(smsReceiver)
        super.onDestroy()

        /* Stop the ApiService when the Activity is destroyed */
        val serviceIntent = Intent(this, ApiService::class.java)
        stopService(serviceIntent)
    }

    override fun onClick(v: View?) {
        textView?.text = "Hello World!"
    }

    private fun sendPostSmsRequest(it: View?, from: String, message: String) {
        val endpoint = "http://ec2-3-112-51-9.ap-northeast-1.compute.amazonaws.com:5000/api/v1/exness/sms"
        val client = OkHttpClient()
        // yyyy-mm-dd hh:mm:ss
        val timestamp = java.time.LocalDateTime.now().toString()
        val json = """
            {
                "timestamp": "$timestamp",
                "mobile": "$from",
                "msg": "$message"
            }
            """.trimIndent()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

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

    /** set textview from another thread */
    fun setTextView(from: String, message: String) {
        handler.sendMessage(Message().apply {
            data = Bundle().apply {
                putString("from", from)
                putString("message", message)
            }
        })
        playAlertSound()
    }

    /* make vibration and specific sound notification */
    private fun playAlertSound() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(1000)
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(applicationContext, notification)
        ringtone.play()
    }

}