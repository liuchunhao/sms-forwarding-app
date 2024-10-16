package com.example.mysmsapp.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.mysmsapp.R

class SmsHandlingService : Service() {

    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "SMS_HANDLING_CHANNEL",
                "SMS Handling Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val phoneNumber = intent?.getStringExtra("phoneNumber")
        val messageBody = intent?.getStringExtra("messageBody")

        val notification: Notification = NotificationCompat.Builder(this, "SMS_HANDLING_CHANNEL")
            .setContentTitle("Handling SMS")
            .setContentText("SMS from $phoneNumber: $messageBody")
            .setSmallIcon(R.drawable.ic_sms)
            .build()
        startForeground(1, notification)

        // Handle the SMS message here
        // For example, send it to a server or log it

        // Stop the service once the SMS is handled
        stopForeground(true)
        stopSelf()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {

        // The onBind() method is used to bind the service to the activity.
        // This method is called when the activity is connected to the service.
        // The onBind() method returns an IBinder object that the client can use to communicate with the service.
        // If the service is not meant to be bound, the onBind() method should return null.


        return null
    }
}
