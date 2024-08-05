package com.example.mysmsapp;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SMSReceiver extends BroadcastReceiver {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        assert bundle != null;
        Object[] smsObj = (Object[]) bundle.get("pdus");

        assert smsObj != null;
        for (Object obj : smsObj) {
            SmsMessage message = SmsMessage.createFromPdu((byte[]) obj);
            String from = message.getDisplayOriginatingAddress();
            String msg = message.getDisplayMessageBody();

            Log.i("SMSReceiver", "onReceive: " + from + " " + msg);

            /* Your Exness verification code is: 146394 */
            if (msg.toLowerCase().contains("verification code")) {
                display(context, from, msg);
                sendPostRequest(from, msg);
            }
        }
    }

    private void displaySMS(Context context, String mobNo, String msg) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("from", mobNo);
        intent.putExtra("msg", msg);
        context.startActivity(intent);
    }

    /**
     * This method is used to display the SMS on the MainActivity
     * @param context Context
     * @param from String
     * @param msg String
     */
    private void display(Context context, String from, String msg) {
        MainActivity inst = MainActivity.instance;
        inst.setTextView(from, msg);
    }

    private final OkHttpClient client = new OkHttpClient();

    /**
     *  you cannot access localhost from your mobile device, so you need to use ngrok to expose your localhost to the internet
     *  ngrok http 5000 -> https://2ac5-123-193-85-198.ngrok-free.app
     */

    private final static String URL = "http://ec2-3-112-51-9.ap-northeast-1.compute.amazonaws.com:5000/api/v1/exness/sms";
    // private final static String URL = "http://localhost:5000/api/v1/exness/sms";

    private void sendPostRequest(String from, String msg) {
        try {
            JSONObject jsonObject = new JSONObject();
            String timestamp = DateFormat.getDateTimeInstance().format(System.currentTimeMillis());
            jsonObject.put("timestamp", timestamp);
            jsonObject.put("mobile", from);
            jsonObject.put("msg", msg);

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonObject.toString());

            Request request = new Request.Builder()
                    .url(URL)
                    .post(requestBody)
                    .build();
            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if(response.isSuccessful()) {
                        // only allowed to read once from the response
                        String msg = Objects.requireNonNull(response.body()).string();
                        Log.e("SMSReceiver", "onResponse: " + msg);
                        // This is the response from the server after sending the POST request, show it in the MainActivity
                        display(null, URL, msg);
                    } else {
                        display(null, URL, "Failed to send the SMS");
                    }
                }

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("SMSReceiver", "onFailure: " + e.getMessage(), e);
                    String msg = e.getMessage();
                    display(null, URL, msg);
                }
            });
        } catch (Exception e) {
            Log.e("SMSReceiver", "sendPostRequest: " + e.getMessage(), e);
            display(null, URL, e.getMessage());
        }
    }

}