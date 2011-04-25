package org.alexvod;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class LongitudeService extends Service {
  private NotificationManager notificationManager;

  //Unique Identification Number for the Notification.
  // We use it on Notification start, and to cancel it.
  private int NOTIFICATION = 1234;


  /**
   * Class for clients to access.  Because we know this service always
   * runs in the same process as its clients, we don't need to deal with
   * IPC.
   */
  public class InProcessBinder extends Binder {
    LongitudeService getService() {
      return LongitudeService.this;
    }
  }

  @Override
  public void onCreate() {
    notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

    // Display a notification about us starting.  We put an icon in the status bar.
    showNotification();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.i("LocalService", "Received start id " + startId + ": " + intent);
    // We want this service to continue running until it is explicitly
    // stopped, so return sticky.
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    // Cancel the persistent notification.
    notificationManager.cancel(NOTIFICATION);

    // Tell the user we stopped.
    Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }

  // This is the object that receives interactions from clients.  See
  // RemoteService for a more complete example.
  private final IBinder binder = new InProcessBinder();

  /**
   * Show a notification while this service is running.
   */
  private void showNotification() {
    // In this sample, we'll use the same text for the ticker and the expanded notification
    // Set the icon, scrolling text and timestamp
    Notification notification = new Notification(R.drawable.icon, "Longitude is active", System.currentTimeMillis());

    // The PendingIntent to launch our activity if the user selects this notification
    PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
        new Intent(this, LongitudeActivity.class), 0);

    // Set the info for the views that show in the notification panel.
    notification.setLatestEventInfo(this, "Longitude", "Tracking your location", contentIntent);

    // Send the notification.
    notificationManager.notify(NOTIFICATION, notification);
  }
}
