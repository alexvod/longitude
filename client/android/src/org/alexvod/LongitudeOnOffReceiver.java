package org.alexvod;

import org.ushmax.common.Logger;
import org.ushmax.common.LoggerFactory;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class LongitudeOnOffReceiver extends BroadcastReceiver {
  private static final Logger logger = LoggerFactory.getLogger(LongitudeOnOffReceiver.class);
  private static final String MESSAGE_INTENT_NAMESPACE = "org.alexvod.msg";
  
  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    logger.info("Received intent " + action);
    if (action.equals(MESSAGE_INTENT_NAMESPACE + ".LONGITUDE_ON")) {
       Intent serviceIntent = new Intent(context, LongitudeService.class);
       context.startService(serviceIntent);
    }
    if (action.equals(MESSAGE_INTENT_NAMESPACE + ".LONGITUDE_OFF")) {
      Intent serviceIntent = new Intent(context, LongitudeService.class);
      context.stopService(serviceIntent);
    }
  }
}
