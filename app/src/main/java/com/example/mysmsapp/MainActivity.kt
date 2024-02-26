package com.example.mysmsapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity () , View.OnClickListener {

    companion object {
        lateinit var instance: MainActivity
    }

    private var button: Button? = null
    private var textView: TextView? = null

    /**
        private lateinit var binding: ActivityMainBinding
        private val readPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    */

    /**
        private val smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d("MainActivity", "onReceive sms-received intent")

                if (intent?.action == "sms-received") {
                    val smsContent = intent.getStringExtra("smsContent")
                    textView?.text = "Received SMS: $smsContent"
                }
            }
        }
    */

    private val smsReceiver = object : SMSReceiver() {

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        instance = this

        Log.d("MainActivity", "onCreate")

        textView = findViewById(R.id.textView)
        textView?.text = "Hello World!"

        button = findViewById(R.id.buttonResend);
        button?.setOnClickListener {

            // sendMessage(it)
            // it -> onClick(it)
            // onClick(it)
            // textView?.text = "Hello World!"

            Intent (this, SettingsActivity::class.java).also {
                startActivity(it)
                Log.d("Settings", "Start SettingsActivity")
            }
        }

        /**
            binding = ActivityMainBinding.inflate(layoutInflater)

            readPermission.launch(
                arrayOf(
                    android.Manifest.permission.RECEIVE_SMS,
                    android.Manifest.permission.READ_SMS
                )
            )
        */

        /**
            // registering BroadcastReceiver
            val filter = IntentFilter("sms-received")
            registerReceiver(smsReceiver, filter, RECEIVER_EXPORTED) // for security reason, it's a must to set the receiver as non-exported
        */
    }

    override fun onDestroy() {
        // 解除註冊 BroadcastReceiver
        unregisterReceiver(smsReceiver)
        super.onDestroy()
    }

    override fun onClick(v: View?) {
        textView?.text = "Hello World!"
    }

    fun sendMessage(it: View?) {
        println("sendMessage")
    }

    fun setTextView(mobNo: String, message: String) {
        textView?.text = "Received SMS: $message from $mobNo"
    }

}