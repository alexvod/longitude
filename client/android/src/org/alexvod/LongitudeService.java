package org.alexvod;

import org.ushmax.android.ProxyListener;
import org.ushmax.android.SettingsHelper;
import org.ushmax.common.ITaskDispatcher;
import org.ushmax.common.Logger;
import org.ushmax.common.LoggerFactory;
import org.ushmax.common.QueuedTaskDispatcher;
import org.ushmax.fetcher.HttpFetcherImpl;
import org.ushmax.fetcher.AsyncHttpFetcher;
import org.ushmax.fetcher.HttpFetcher;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class LongitudeService extends Service {
  private Logger logger;
  private NotificationManager notificationManager;

  //Unique Identification Number for the Notification.
  // We use it on Notification start, and to cancel it.
  private int NOTIFICATION = 1234;

  private LocationTracker locationTracker;
  private ProxyListener gpsListener;
  private ProxyListener networkListener;
  private boolean isRunning;

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
    isRunning = false;
    logger = LoggerFactory.getLogger(LongitudeService.class);
    notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

    // Display a notification about us starting.  We put an icon in the status bar.
    showNotification();

    ITaskDispatcher taskDispatcher = new QueuedTaskDispatcher(4);
    HttpFetcher httpFetcher = new HttpFetcherImpl(null);
    AsyncHttpFetcher asyncHttpFetcher = new AsyncHttpFetcher(httpFetcher, taskDispatcher); 
    locationTracker = new LocationTracker(this, asyncHttpFetcher);
    taskDispatcher.start();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    enableLocationTracking();

    // We want this service to continue running until it is explicitly
    // stopped, so return sticky.
    return START_STICKY;
  }

  public void enableLocationTracking() {
    if (isRunning) {
      logger.debug("Already running");
      return;
    }

    logger.debug("Starting location tracker");
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    int minTime = SettingsHelper.getIntPref(prefs, "loc_update_time", 60000);
    int minDistance = SettingsHelper.getIntPref(prefs, "loc_update_distance", 10);
    boolean useNetwork = SettingsHelper.getBoolPref(prefs, "loc_use_network", true);
    boolean useGps = SettingsHelper.getBoolPref(prefs, "loc_use_gps", true);

    LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
    if (useGps) {
      gpsListener = new ProxyListener(locationTracker);
      locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, gpsListener);
      logger.debug("GPS bound");      
    }
    if (useNetwork) {
      networkListener = new ProxyListener(locationTracker);
      locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, networkListener);
      logger.debug("Network bound");
    }
    isRunning = true;
  }

  public void disableLocationTracking() {
    if (!isRunning) {
      logger.debug("Not running");
      return;
    }
    LocationManager mgr = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
    if (gpsListener != null) { 
      mgr.removeUpdates(gpsListener);
      gpsListener.unbind();
      gpsListener = null;
      logger.debug("GPS unbound");
    }
    if (networkListener != null) {
      mgr.removeUpdates(networkListener);
      networkListener.unbind();
      networkListener = null;
      logger.debug("Network unbound");
    }
    isRunning = false;
  }

  @Override
  public void onDestroy() {
    logger.debug("Stopping");
    disableLocationTracking();

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
