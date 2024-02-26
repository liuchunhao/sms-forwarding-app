package com.example.mysmsapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast


class MySMSReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.

        val TAG: String = MySMSReceiver::class.java.simpleName
        val pdu_type = "pdus"       // protocol data unit: the PDUs which contain the SMS messages

        // Get the SMS message.
        val bundle = intent.extras
        val msgs: Array<SmsMessage?>
        var strMessage = ""
        val format = bundle!!.getString("format")

        // Retrieve the SMS message received.
        val pdus = bundle[pdu_type] as Array<*>?
        if (pdus != null) {
            msgs = Array<SmsMessage?>(pdus.size) { null }   // Initialize array of SmsMessage objects
            for (i in pdus.indices) {
                // Check Android version and use appropriate createFromPdu.
                // If Android version M or newer:
                msgs[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray, format)

                // Build the message to show.
                strMessage += "SMS from ${msgs[i]?.originatingAddress}";
                strMessage += " :" + msgs[i]!!.messageBody + "\n";
                // Log and display the SMS message.
                Log.d(TAG, "onReceive: " + strMessage);
                Toast.makeText(context, strMessage, Toast.LENGTH_LONG).show();
            }
        }
    }
}