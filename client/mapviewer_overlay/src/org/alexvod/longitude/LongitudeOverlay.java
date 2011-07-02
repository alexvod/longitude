/**
 * 
 */
package org.alexvod.longitude;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.alexvod.longitude.Proto.Location;
import org.alexvod.longitude.Proto.LocationInfo;
import org.ushmax.android.SettingsHelper;
import org.ushmax.common.BufferAllocator;
import org.ushmax.common.ByteArraySlice;
import org.ushmax.common.Logger;
import org.ushmax.common.LoggerFactory;
import org.ushmax.common.Pair;
import org.ushmax.common.WaitCallback;
import org.ushmax.fetcher.AsyncHttpFetcher;
import org.ushmax.fetcher.HttpFetcher;
import org.ushmax.fetcher.HttpFetcher.MHttpRequest;
import org.ushmax.fetcher.HttpFetcher.NetworkException;
import org.ushmax.geometry.FastMercator;
import org.ushmax.geometry.MercatorReference;
import org.ushmax.geometry.Point;
import org.ushmax.mapviewer.Overlay;
import org.ushmax.mapviewer.R;
import org.ushmax.mapviewer.UiController;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.preference.PreferenceManager;

import com.google.protobuf.InvalidProtocolBufferException;

public class LongitudeOverlay implements Overlay {
  private static final Logger logger = LoggerFactory.getLogger(LongitudeOverlay.class);
  private static final int GET_LOC_TIMEOUT = 30000;
  private static final int POLL_TIMEOUT = 30 * 60;
  private AsyncHttpFetcher httpFetcher;
  private UiController uiController;
  private Paint paint, iconPaint;
  private Thread updaterThread;
  private volatile boolean quit;
  private Bitmap icon;
  private LocationInfo locationInfo; // protected by this
  private int iconAnchorX;
  private int iconAnchorY;
  private int iconSizeX;
  private int iconSizeY;
  private Context context;
  private static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

  public LongitudeOverlay(Context context, Resources resources, AsyncHttpFetcher httpFetcher, UiController uiController) {
    this.context = context;
    this.httpFetcher = httpFetcher;
    this.uiController = uiController;
    iconPaint = new Paint();
    paint = new Paint();
    paint.setColor(Color.argb(32, 0, 0, 255));
    paint.setStyle(Style.FILL_AND_STROKE);
    icon = BitmapFactory.decodeResource(resources, R.drawable.red_dot);
    iconSizeX = icon.getWidth();
    iconSizeY = icon.getHeight();
    iconAnchorX = iconSizeX / 2;
    iconAnchorY = iconSizeY;

    quit = false;
    updaterThread = new Thread(new Runnable() { 
      @Override
      public void run() {
        updaterThreadLoop();
      } });
    updaterThread.start();
  }

  private void updaterThreadLoop() {
    boolean hasData = false;
    while (!quit) {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      String serverAddress = SettingsHelper.getStringPref(prefs, "longitude_server", "host:port");
      String clientId = SettingsHelper.getStringPref(prefs, "longitude_user", "user");
      String authToken = SettingsHelper.getStringPref(prefs, "longitude_token", "passw0rd");
      
      // Use /getloc for the first request, /poll for updates.
      String url = "https://" + serverAddress;
      if (hasData) {
        url = url + "/poll?output=proto&timeout=" + POLL_TIMEOUT;
      } else {
        url = url + "/getloc?output=proto";
      }

      MHttpRequest req = new MHttpRequest();
      req.method = HttpFetcher.Method.GET;
      req.url = url;
      req.cookies = new ArrayList<Pair<String, String>>();
      req.cookies.add(Pair.newInstance("client", clientId));
      req.cookies.add(Pair.newInstance("token", authToken));
      
      int deadline = hasData ? (POLL_TIMEOUT + 60) : GET_LOC_TIMEOUT;
      WaitCallback<Pair<ByteArraySlice, NetworkException>> callback = new WaitCallback<Pair<ByteArraySlice, NetworkException>>();
      httpFetcher.fetch(req, callback, deadline * 1000);
      Pair<ByteArraySlice, NetworkException> result;
      try {
        result = callback.waitForCompletionWithInterrupt();
      } catch (InterruptedException e) {
        continue;
      }
      ByteArraySlice data = result.first;
      if (data == null) {
        logger.error("Error occured during fetching location: " + result.second + " caused by " + result.second.getCause());
        // Likely a network error. Wait a bit.
        try {
          Thread.sleep(30 * 1000);
        } catch (InterruptedException e) {
          break;
        }
        continue;
      }
      try {
        LocationInfo info = LocationInfo.newBuilder().mergeFrom(data.data, data.start, data.count).build();
        synchronized (this) {
          locationInfo = info;
          hasData = true;
        }
        uiController.invalidate();
      } catch (InvalidProtocolBufferException e) {
        logger.error("Received broken repsonse from server: " + e);
      } finally {
        BufferAllocator.free(data, "LongitudeOverlay.onReceiveUpdate");
      }
    }
    logger.debug("Exiting updater thread");
  }

  @Override
  public void draw(Canvas canvas, int zoom, Point origin, Point size) {
    LocationInfo info;
    synchronized (this) {
      info = locationInfo;
    }
    if (info == null) {
      return;
    }
    int zoomShift = 20 - zoom;    
    for (int i = 0; i < info.getLocationCount(); i++) {
      Location location = info.getLocation(i);
      int y = FastMercator.projectLat((int)(location.getLat() * 1e+7));
      int x = FastMercator.projectLng((int)(location.getLng() * 1e+7));
      int accuracy = (int) (location.getAccuracy() * 
          MercatorReference.metersToPixels((float)location.getLat(), (float)location.getLng(), 20));
      int radius = accuracy >> zoomShift;
    x = (x >> zoomShift) - origin.x;
    y = (y >> zoomShift) - origin.y;
    if (radius > 3) {
      canvas.drawCircle(x, y, radius, paint);
    }
    int pt_x = x - iconAnchorX;
    int pt_y = y - iconAnchorY;
    if ((pt_x < -iconSizeX) ||
        (pt_y < -iconSizeY) ||
        (pt_x > size.x) ||
        (pt_y > size.y)) {
      continue;
    }
    canvas.drawBitmap(icon, pt_x, pt_y, iconPaint);
    }
  }

  @Override
  public boolean onTap(Point tapPoint, Point origin, int zoom) {
    LocationInfo info;
    synchronized (this) {
      info = locationInfo;
    }
    if (info == null) {
      return false;
    }
    int zoomShift = 20 - zoom;
    final int maxDist = 40;
    final int numLocations = info.getLocationCount();
    for (int i = 0; i < numLocations; i++) {
      Location location = info.getLocation(i);
      int y = FastMercator.projectLat((int)(location.getLat() * 1e+7));
      int x = FastMercator.projectLng((int)(location.getLng() * 1e+7));
      int dx = (x >> zoomShift) - iconAnchorX + iconSizeX / 2 - (origin.x + tapPoint.x);
      int dy = (y >> zoomShift) - iconAnchorY + iconSizeY / 2 - (origin.y + tapPoint.y);
      int dist = Math.abs(dx) + Math.abs(dy);
      if (dist < maxDist) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(location.getTime());
        String text = location.getName() + " at  " + dateFormat.format(cal.getTime());
        uiController.displayMessage(text);
        return true;
      }
    }
    return false;
  }

  @Override
  public String name() {
    return "longitude";
  }

  @Override
  public void free() {
    quit = true;
    updaterThread.interrupt();
  }
}