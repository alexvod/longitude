package org.alexvod;

import java.util.ArrayList;

import org.ushmax.android.SettingsHelper;
import org.ushmax.common.BufferAllocator;
import org.ushmax.common.ByteArraySlice;
import org.ushmax.common.Callback;
import org.ushmax.common.Logger;
import org.ushmax.common.LoggerFactory;
import org.ushmax.common.Pair;
import org.ushmax.fetcher.AsyncHttpFetcher;
import org.ushmax.fetcher.HttpFetcher;
import org.ushmax.fetcher.HttpFetcher.MHttpRequest;
import org.ushmax.fetcher.HttpFetcher.NetworkException;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class LocationTracker implements LocationListener { 
  private static final Logger logger = LoggerFactory.getLogger(LocationTracker.class);
  
  private int networkTimeout;
  private AsyncHttpFetcher httpFetcher;
  private String server;
  private String clientId;
  private String authToken;

  public LocationTracker(Context context, AsyncHttpFetcher httpFetcher) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    server = SettingsHelper.getStringPref(prefs, "longitude_server", "host:port");
    clientId = SettingsHelper.getStringPref(prefs, "longitude_user", "user");
    authToken = SettingsHelper.getStringPref(prefs, "longitude_token", "passw0rd");
    networkTimeout = 30000;
    this.httpFetcher = httpFetcher;
  }

  @Override
  public void onLocationChanged(Location location) {
    double lat = location.getLatitude();
    double lng = location.getLongitude();
    double accuracy = location.getAccuracy();
    long timestamp = location.getTime();
    logger.debug("Location update " + lat + "," + lng + ":" + accuracy);

    sendLocationToServer(lat, lng, accuracy, timestamp);
  }

  private void sendLocationToServer(double lat, double lng, double accuracy, long timestamp) {
    final String url = "https://" + server + "/update?lat=" + lat + "&lng=" + lng + "&acc=" + accuracy + "&time=" + timestamp;
    MHttpRequest req = new MHttpRequest();
    req.url = url;
    req.method = HttpFetcher.Method.POST;
    req.cookies = new ArrayList<Pair<String, String>>();
    req.cookies.add(Pair.newInstance("client", clientId));
    req.cookies.add(Pair.newInstance("token", authToken));

    Callback<Pair<ByteArraySlice, NetworkException>> callback = 
      new Callback<Pair<ByteArraySlice, NetworkException>>() {
      @Override
      public void run(Pair<ByteArraySlice, NetworkException> result) {
        onNetworkReply(url, result);
      }};  
    httpFetcher.fetch(req, callback, networkTimeout);
  }

  protected void onNetworkReply(String url, Pair<ByteArraySlice, NetworkException> result) {
    final ByteArraySlice data = result.first;
    NetworkException exc = result.second;
    if (data != null) {
      logger.debug("Received server reply: " + data.toUtf8());
      BufferAllocator.free(data, "LocationTracker.onServerReply");
      return;
    }
    // Some error occurred.
    logger.debug("Network error occured during loading " + url + " from network " + exc.toString());
    Throwable cause = exc.getCause();
    if (cause != null) {
      logger.debug("  caused by " + cause);
      exc.printStackTrace();
    }
  }

  @Override
  public void onProviderDisabled(String provider) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onProviderEnabled(String provider) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {
    // TODO Auto-generated method stub

  }
}
