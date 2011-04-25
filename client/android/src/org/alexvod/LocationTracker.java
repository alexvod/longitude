package org.alexvod;

import org.ushmax.common.Logger;
import org.ushmax.common.LoggerFactory;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class LocationTracker implements LocationListener { 
  private static final Logger logger = LoggerFactory.getLogger(LocationTracker.class);

  @Override
  public void onLocationChanged(Location location) {
    double lat = location.getLatitude();
    double lng = location.getLongitude();
    logger.debug("Location update " + lat + "," + lng);
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
