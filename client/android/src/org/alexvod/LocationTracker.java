package org.alexvod;

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

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class LocationTracker implements LocationListener { 
  private static final Logger logger = LoggerFactory.getLogger(LocationTracker.class);
  private static final String TEST_ID = "test";
  private static final String TEST_SERVER = "bomjp.dyndns.org:46940";

  private String id;
  private int networkTimeout;
  private AsyncHttpFetcher httpFetcher;
  private String server;

  public LocationTracker(AsyncHttpFetcher httpFetcher) {
    id = TEST_ID;
    server = TEST_SERVER;
    networkTimeout = 30000;
    this.httpFetcher = httpFetcher;
  }

  @Override
  public void onLocationChanged(Location location) {
    double lat = location.getLatitude();
    double lng = location.getLongitude();
    logger.debug("Location update " + lat + "," + lng);

    sendLocationToServer(lat, lng);
  }

  private void sendLocationToServer(double lat, double lng) {
    final String url = "http://" + server + "/update?id=" + id + "&lat=" + lat + "&lng=" + lng;
    MHttpRequest req = new MHttpRequest();
    req.url = url;
    req.method = HttpFetcher.Method.POST;

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
