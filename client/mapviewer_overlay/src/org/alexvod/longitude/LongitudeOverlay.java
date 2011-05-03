/**
 * 
 */
package org.alexvod.longitude;

import org.alexvod.longitude.Proto.Location;
import org.alexvod.longitude.Proto.LocationInfo;
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
  private static final int HTTP_DEADLINE = 30000;
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
    while (!quit) {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      String serverAddress = SettingsHelper.getStringPref(prefs, "longitude_server", "host:port");
      int updateInterval = SettingsHelper.getIntPref(prefs, "longitude_update", 60000);
      String url = "http://" + serverAddress + "/getloc?output=proto";
      MHttpRequest req = new MHttpRequest();
      req.method = HttpFetcher.Method.GET;
      req.url = url;
      httpFetcher.fetch(req,
          new Callback<Pair<ByteArraySlice, NetworkException>>() {
            @Override
            public void run(Pair<ByteArraySlice, NetworkException> result) {
              onReceiveUpdate(result);
            }}, HTTP_DEADLINE);
      try {
        Thread.sleep(updateInterval);
      } catch (InterruptedException e) {
        break;
      }
    }
    logger.debug("Exiting updater thread");
  }

  private void onReceiveUpdate(Pair<ByteArraySlice, NetworkException> result) {
    ByteArraySlice data = result.first;
    if (data == null) {
      logger.error("Error occured during fetching location: " + result.second);
      return;
    }
    try {
      LocationInfo info = LocationInfo.newBuilder().mergeFrom(data.data, data.start, data.count).build();
      synchronized (this) {
        locationInfo = info;
      }
      uiController.invalidate();
    } catch (InvalidProtocolBufferException e) {
      logger.error("Received broken repsonse from server: " + e);
    } finally {
      BufferAllocator.free(data, "LongitudeOverlay.onReceiveUpdate");
    }
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
        uiController.displayMessage(location.getName());
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