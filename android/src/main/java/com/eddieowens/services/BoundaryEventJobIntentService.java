package com.eddieowens.services;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import android.util.Log;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import androidx.core.app.NotificationCompat;
import android.app.TaskStackBuilder;
import android.app.PendingIntent;
import android.graphics.Color;




import com.eddieowens.RNBoundaryModule;
import com.eddieowens.errors.GeofenceErrorMessages;
import com.facebook.react.HeadlessJsTaskService;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import android.os.Handler;
import android.app.Notification;
import android.os.Build;

import java.util.ArrayList;

import static com.eddieowens.RNBoundaryModule.TAG;

public class BoundaryEventJobIntentService extends JobIntentService {

    private static final String CHANNEL_ID = "channel_01";

    public static final String ON_ENTER = "onEnter";
    public static final String ON_EXIT = "onExit";

    final Handler handler = new Handler();

    public BoundaryEventJobIntentService() {
        super();
    }

    boolean workSuccessful = true;

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.i(TAG, "Handling geofencing event");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        Log.i(TAG, "Geofence transition: " + geofencingEvent.getGeofenceTransition());
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Error in handling geofence " + GeofenceErrorMessages.getErrorString(geofencingEvent.getErrorCode()));
            return;
        }
        switch (geofencingEvent.getGeofenceTransition()) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                Log.i(TAG, "Enter geofence event detected. Sending event.");
                final ArrayList<String> enteredGeofences = new ArrayList<>();
                for (Geofence geofence : geofencingEvent.getTriggeringGeofences()) {
                    enteredGeofences.add(geofence.getRequestId());
                }
                try{
                    sendEvent(this.getApplicationContext(), ON_ENTER, enteredGeofences);
                }catch(Exception error){
                    workSuccessful = true;
                    Log.e("BoundaryEventJobIntentService", "error with context");
                }
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                Log.i(TAG, "Exit geofence event detected. Sending event.");
                final ArrayList<String> exitingGeofences = new ArrayList<>();
                for (Geofence geofence : geofencingEvent.getTriggeringGeofences()) {
                    exitingGeofences.add(geofence.getRequestId());
                }
                try{
                    sendEvent(this.getApplicationContext(), ON_EXIT, exitingGeofences);
                }catch(Exception error){
                    workSuccessful = true;
                    Log.e("BoundaryEventJobIntentService", "error with context");
                }
                break;
        }
    }

    private void sendEvent(Context context, String event, ArrayList<String> params) {
        if(workSuccessful) {
            workSuccessful= false;
            final Intent intent = new Intent(RNBoundaryModule.GEOFENCE_DATA_TO_EMIT);
            intent.putExtra("event", event);
            intent.putExtra("params", params);

            Bundle bundle = new Bundle();
            bundle.putString("event", event);
            bundle.putStringArrayList("ids", intent.getStringArrayListExtra("params"));

            Intent headlessBoundaryIntent = new Intent(context, BoundaryEventHeadlessTaskService.class);
            headlessBoundaryIntent.putExtras(bundle);
            context.startService(headlessBoundaryIntent);
            HeadlessJsTaskService.acquireWakeLockNow(context);

            handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            sendNotification();
                        }
                        workSuccessful = true;
                    }
            }, 6000);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sendNotification();
        }
    }

    private void sendNotification() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Dispathcland")
            .setContentText("Geofancing run in background")
            // .setContentIntent(notificationPendingIntent)
            .build();

        startForeground(0, notification);
    }
}