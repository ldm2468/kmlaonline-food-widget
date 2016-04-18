package com.ldm2468.kmlafood;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class GetFoodTask extends AsyncTask<Context, Void, JSONObject> {
    private Context context = null;
    private AppWidgetManager appWidgetManager;
    private int[] appWidgetIds;
    private RemoteViews remoteViews;

    private void updateVariables() {
        appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context.getApplicationContext(), FoodProvider.class);
        appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.widget_layout);
    }

    @Override
    protected JSONObject doInBackground(Context... contexts) {
        context = contexts[0];
        updateVariables();

        SharedPreferences preferences = context.getSharedPreferences("time", 0);

        long time = preferences.getBoolean("auto", true) ? System.currentTimeMillis() / 1000L :
                preferences.getLong("time", System.currentTimeMillis() / 1000L);
        Calendar today = Calendar.getInstance();
        today.setTimeInMillis(time * 1000L);
        //get next day's menu after 10:00 P.M.
        if (today.get(Calendar.HOUR_OF_DAY) >= 22) {
            today.add(Calendar.DATE, 1);
        }

        //check cache
        SimpleDateFormat format = new SimpleDateFormat("yyyy/M/d (EEE)", Locale.getDefault());
        String timeStr = format.format(today.getTime());
        if (preferences.contains(timeStr)) {
            JSONObject food = null;
            try {
                food = new JSONObject(preferences.getString(timeStr, ""));
                Log.i("KMLA", "Read cache successfully!");
            } catch (JSONException e) {
                Log.e("KMLA", "?", e);
            }
            return food;
        } else {
            Log.i("KMLA", "No cache for " + timeStr);
        }

        //set loading text
        remoteViews.setTextViewText(R.id.textView, "loading...");
        remoteViews.setTextViewText(R.id.date, "loading...");
        for (int appWidgetId : appWidgetIds) appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

        //get kmlaonline url
        URL foodApi = null;
        try {
            foodApi = new URL("https://kmlaonline.net/api/food?time=" + time);
        } catch (MalformedURLException e) {
            Log.e("KMLA", "?", e);
        }
        if (foodApi == null) return null;

        //fetch json from server
        JSONObject food = null;
        try {
            //open connection
            HttpURLConnection connection = (HttpURLConnection)
                    foodApi.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-length", "0");
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            connection.connect();

            //get string from stream
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
            br.close();
            connection.disconnect();

            //try parsing json
            food = new JSONObject(sb.toString());
        } catch (IOException | JSONException e) {
            Log.e("KMLA", "?", e);
        }
        Log.i("KMLA", "Got food!");
        return food;
    }

    @Override
    protected void onPostExecute(JSONObject jsonObject) {
        String food = "입력되지 않음";
        SharedPreferences preferences = context.getSharedPreferences("time", 0);
        try {
            //get date
            Calendar cal = Calendar.getInstance();
            cal.set(jsonObject.getInt("year"), jsonObject.getInt("month") - 1, jsonObject.getInt("date"));
            SimpleDateFormat format = new SimpleDateFormat("yyyy/M/d (EEE)", Locale.getDefault());
            String date = format.format(cal.getTime());

            //cache if all data present
            if (!preferences.contains(date)) {
                if (jsonObject.has("breakfast") && jsonObject.has("lunch") && jsonObject.has("dinner")) {
                    preferences.edit().putString(date, jsonObject.toString()).apply();
                    Log.i("KMLA", "Added cache " + date);
                } else {
                    Log.i("KMLA", "Insufficient info for cache " + date);
                }
            }

            //set ui date
            remoteViews.setTextViewText(R.id.date, date);

            //set food time
            String foodTime = preferences.getString("food", "auto");
            if (foodTime.equals("auto")) {
                foodTime = jsonObject.getString("food");
            }
            setFoodButton(foodTime);

            //get food from json object
            food = jsonObject.getString(foodTime);
            Log.i("KMLA", food);
        } catch (JSONException e) {
            Log.w("KMLA", "No food T.T", e);
        }
        remoteViews.setTextViewText(R.id.textView, food);
        updateIntents();
    }

    private void updateIntents() {
        Intent intent = new Intent(context, FoodProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.refresh, pendingIntent);

        intent = new Intent(context, FoodProvider.class);
        intent.setAction(FoodProvider.ACTION_LEFT);
        pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.left, pendingIntent);

        intent = new Intent(context, FoodProvider.class);
        intent.setAction(FoodProvider.ACTION_RIGHT);
        pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.right, pendingIntent);

        intent = new Intent(context, FoodProvider.class);
        intent.setAction(FoodProvider.ACTION_BREAKFAST);
        pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.breakfast, pendingIntent);

        intent = new Intent(context, FoodProvider.class);
        intent.setAction(FoodProvider.ACTION_LUNCH);
        pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.lunch, pendingIntent);

        intent = new Intent(context, FoodProvider.class);
        intent.setAction(FoodProvider.ACTION_DINNER);
        pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.dinner, pendingIntent);

        for (int appWidgetId : appWidgetIds) appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    private void setFoodButton(String food) {
        remoteViews.setTextViewText(R.id.breakfast, context.getText(R.string.breakfast));
        remoteViews.setTextViewText(R.id.lunch, context.getText(R.string.lunch));
        remoteViews.setTextViewText(R.id.dinner, context.getText(R.string.dinner));
        switch (food) {
            case "breakfast":
                remoteViews.setTextViewText(R.id.breakfast, context.getText(R.string.sbreakfast));
                break;
            case "lunch":
                remoteViews.setTextViewText(R.id.lunch, context.getText(R.string.slunch));
                break;
            case "dinner":
                remoteViews.setTextViewText(R.id.dinner, context.getText(R.string.sdinner));
                break;
        }
    }
}
