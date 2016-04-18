package com.ldm2468.kmlafood;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class FoodProvider extends AppWidgetProvider {
    public static final String ACTION_LEFT = "l", ACTION_RIGHT = "r",
            ACTION_BREAKFAST = "B", ACTION_LUNCH = "L", ACTION_DINNER = "D";
    public static final long ONE_DAY = 60 * 60 * 24;

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        SharedPreferences preferences = context.getSharedPreferences("time", 0);
        //reset time
        preferences.edit().putBoolean("auto", true).putLong("time", System.currentTimeMillis() / 1000L)
                .putString("food", "auto").apply();
        new GetFoodTask().execute(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        SharedPreferences preferences = context.getSharedPreferences("time", 0);
        switch(intent.getAction()) {
            case ACTION_LEFT:
                //previous day
                preferences.edit().putBoolean("auto", false)
                        .putLong("time", preferences.getLong("time", System.currentTimeMillis() / 1000L) - ONE_DAY)
                        .apply();
                new GetFoodTask().execute(context);
                break;
            case ACTION_RIGHT:
                //next day
                preferences.edit().putBoolean("auto", false)
                        .putLong("time", preferences.getLong("time", System.currentTimeMillis() / 1000L) + ONE_DAY)
                        .apply();
                new GetFoodTask().execute(context);
                break;
            case ACTION_BREAKFAST:
                preferences.edit().putString("food", "breakfast").apply();
                new GetFoodTask().execute(context);
                break;
            case ACTION_LUNCH:
                preferences.edit().putString("food", "lunch").apply();
                new GetFoodTask().execute(context);
                break;
            case ACTION_DINNER:
                preferences.edit().putString("food", "dinner").apply();
                new GetFoodTask().execute(context);
                break;
        }
    }
}
