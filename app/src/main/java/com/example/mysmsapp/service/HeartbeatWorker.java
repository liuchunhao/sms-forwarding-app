package com.example.mysmsapp.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.mysmsapp.MainActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;

public class HeartbeatWorker extends Worker {
    private static final String TAG = "HeartbeatWorker";
    private static final String HEARTBEAT_URL = "http://ec2-3-112-51-9.ap-northeast-1.compute.amazonaws.com:5000/api/v1/exness/heartbeat";

    public HeartbeatWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            // target URL
            URL url = new URL(HEARTBEAT_URL);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            /*  Set up POST request with JSON payload
             */

            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            // format timestamp and mobile number

            @SuppressLint("SimpleDateFormat") String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis());
            String mobile = MainActivity.instance.getMobileNumber();

            String heartbeatData = String.format("{\"timestamp\":\"%s\", \"mobile\": \"%s\"}", timestamp, mobile);
            OutputStream outputStream = urlConnection.getOutputStream();
            outputStream.write(heartbeatData.getBytes());
            outputStream.flush();
            outputStream.close();

            /* Set up GET request
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);
            */

            // check http response code
            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // read the response
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                display(String.format("%s%n%s", "Heartbeat sent successfully", response.toString()));

                Log.i(TAG, "doWork: " + response.toString());

                return Result.success();
            } else {
                return Result.retry();
            }
        } catch (Exception e) {

            display("Failed to send heartbeat" + e.getMessage());
            playAlertSoundAndVibrate();

            Log.e(TAG, "doWork: Failed to send heartbeat", e);
            return Result.retry();
        }
    }

    void playAlertSoundAndVibrate() {
        MainActivity.instance.playAlertSoundAndVibrate();
    }

    void display(String msg) {
        @SuppressLint("SimpleDateFormat") String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis());
        MainActivity.instance.setLoggingTextView(timestamp, "Heartbeat", msg);
    };
}
