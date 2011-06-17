package org.alexvod;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class LongitudeActivity extends Activity {
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
  
  private void startService() {
    Intent intent = new Intent(this, LongitudeService.class);
    startService(intent);
  }
  
  private void stopService() {
    Intent intent = new Intent(this, LongitudeService.class);
    stopService(intent);
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu, menu);
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.settings:
      showSettingsDialog();
      return true;
    default:
      return super.onOptionsItemSelected(item);      
    }
  }
  
  private void showSettingsDialog() {
    Intent intent = new Intent(this, SettingsActivity.class);
    startActivityForResult(intent, 0);
  }
}