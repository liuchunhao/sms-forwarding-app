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
import java.util.HashMap;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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
            String mobNo = message.getDisplayOriginatingAddress();
            String msg = message.getDisplayMessageBody();
            Log.d("SMSReceiver", "onReceive: " + mobNo + " " + msg);

            if(msg.contains("Your Exness verification code is:")) {
                /* Your Exness verification code is: 146394 */
                String[] arr = msg.split(":");
                String verificationCode = arr[1].trim();
                display(context, mobNo, msg);
                sendPostRequest(mobNo, msg, verificationCode);
            }
        }
    }

    private void displaySMS(Context context, String mobNo, String msg) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("mobNo", mobNo);
        intent.putExtra("msg", msg);
        context.startActivity(intent);
    }

    /**
     * This method is used to display the SMS on the MainActivity
     * @param context Context
     * @param mobNo String
     * @param msg String
     */
    private void display(Context context, String mobNo, String msg) {
        MainActivity inst = MainActivity.instance;
        inst.setTextView(mobNo, msg);
    }

    private final OkHttpClient client = new OkHttpClient();

    /**
     *  you cannot access localhost from your mobile device, so you need to use ngrok to expose your localhost to the internet
     *  ngrok http 5000 -> https://2ac5-123-193-85-198.ngrok-free.app
     */
    private final static String URL = "https://2ac5-123-193-85-198.ngrok-free.app/sms";

    private void sendPostRequest(String from, String msg, String verificationCode) {
        JSONObject jsonObject = new JSONObject();
        String timestamp = DateFormat.getDateTimeInstance().format(System.currentTimeMillis());
        try {
            jsonObject.put("timestamp", timestamp);
            jsonObject.put("from", from);
            jsonObject.put("msg", msg);
            JSONObject data = new JSONObject();
            data.put("verificationCode", verificationCode);
            jsonObject.put("data", data);
        } catch (Exception e) {
            Log.e("SMSReceiver", "sendPostRequest: " + e.getMessage(), e);
        }

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonObject.toString());

        Request request = new Request.Builder()
                .url(URL)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                } else {
                    Log.d("SMSReceiver", "onResponse: " + Objects.requireNonNull(response.body()).string());
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SMSReceiver", "onFailure: " + e.getMessage(), e);
            }
        });
    }

}