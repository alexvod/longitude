package org.alexvod;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class LongitudeActivity extends Activity {
  //private LongitudeService service;
  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    
    Button startButton = (Button)this.findViewById(R.id.start_button);
    startButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        startService();
      }
    });
    
    Button stopButton = (Button)this.findViewById(R.id.stop_button);
    stopButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        stopService();
      }
    });    
  }
  
  protected void startService() {
    Intent intent = new Intent(this, LongitudeService.class);
    startService(intent);
  }
  
  protected void stopService() {
    Intent intent = new Intent(this, LongitudeService.class);
    stopService(intent);
  }
}