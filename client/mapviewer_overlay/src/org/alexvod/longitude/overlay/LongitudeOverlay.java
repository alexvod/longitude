/**
 * 
 */
package org.alexvod.longitude.overlay;

import org.ushmax.common.Logger;
import org.ushmax.common.LoggerFactory;
import org.ushmax.fetcher.AsyncHttpFetcher;
import org.ushmax.geometry.Point;
import org.ushmax.mapviewer.Overlay;
import org.ushmax.mapviewer.UiController;

import android.graphics.Canvas;

public class LongitudeOverlay implements Overlay {
  private static final Logger logger = LoggerFactory.getLogger(LongitudeOverlay.class);
  private static final String SERVER_URL = "http://bomjp.dyndns.org:46940/getloc?output=proto";
  private static final int HTTP_DEADLINE = 60000;
  private AsyncHttpFetcher httpFetcher;

  public LongitudeOverlay(AsyncHttpFetcher httpFetcher, UiController uiController) {
    this.httpFetcher = httpFetcher;
  }

  @Override
  public void draw(Canvas canvas, int zoom, Point origin, Point size) {
  }

  @Override
  public boolean onTap(Point where, Point origin, int zoom) {
    return false;
  }

  @Override
  public String name() {
    return "longitude";
  }

  @Override
  public void free() {
  }
}