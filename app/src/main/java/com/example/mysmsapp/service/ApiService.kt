package com.example.mysmsapp.service

// ApiService.kt
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mysmsapp.MainActivity
import com.example.mysmsapp.R
import okhttp3.*
import java.io.IOException

class ApiService : Service() {

    private val TAG = "ApiService"
    private val BASE_URL =
        "http://ec2-3-112-51-9.ap-northeast-1.compute.amazonaws.com:5000/api/v1/ping"
    private val HANDLER_DELAY = 30 * 1000L // 30 seconds
    private val REQUEST_CODE = 123
    private val CHANNEL_ID = "ApiServiceChannel"

    private lateinit var handler: Handler
    private lateinit var client: OkHttpClient
    private lateinit var ringtone: Ringtone

    override fun onCreate() {
        super.onCreate()
        handler = Handler(Looper.getMainLooper())
        client = OkHttpClient()
        val notification = createNotification()
        startForeground(REQUEST_CODE, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isNetworkAvailable()) {
                    Log.d(TAG, "Network available, sending POST request")
                    sendPostRequest()
                } else {
                    Log.d(TAG, "Network not available")
                    playAlertSound()
                }
                handler.postDelayed(this, HANDLER_DELAY)
            }
        }, HANDLER_DELAY)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotification(): android.app.Notification {
        val channelId = createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            REQUEST_CODE,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Api Service")
            .setContentText("Running")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel(): String {
        val channelId = "ApiServiceChannel"
        val channelName = "API Service Channel"
        val notificationChannel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
        return channelId
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities =
            connectivityManager.getNetworkCapabilities(network)
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }

    private fun sendPostRequest() {
        val requestBody = FormBody.Builder()
            .add("key1", "value1")
            .add("key2", "value2")
            .build()

        val request = Request.Builder()
            .url(BASE_URL)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "POST request failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d(TAG, "POST request successful")
                } else {
                    Log.e(TAG, "POST request failed: ${response.code}")
                }
            }
        })
    }

    private fun playAlertSound() {
        val alertSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

    }

}

