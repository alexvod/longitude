package org.alexvod.longitude;

import org.ushmax.common.Logger;
import org.ushmax.common.LoggerFactory;
import org.ushmax.mapviewer.UiModule;

import android.content.Context;
import android.preference.EditTextPreference;
import android.preference.PreferenceScreen;
import android.view.Menu;

public class LongitudeUiModule implements UiModule {
  private static final Logger logger = LoggerFactory.getLogger(LongitudeUiModule.class);

  public LongitudeUiModule() {}
  
  @Override
  public void onCreateMenu(Menu menu) {
    // No menu options.
  }

  @Override
  public void onSettingsMenu(PreferenceScreen screen, Context context) {
    EditTextPreference pref1 = new EditTextPreference(context);
    pref1.setSummary("Longitude server host:port");
    pref1.setKey("longitude_server");
    pref1.setTitle("Longitude server");
    pref1.setDefaultValue("");
    screen.addPreference(pref1);
    EditTextPreference pref2 = new EditTextPreference(context);
    pref2.setSummary("Longitude user");
    pref2.setKey("longitude_user");
    pref2.setTitle("Longitude user name");
    pref2.setDefaultValue("user");
    screen.addPreference(pref2);
    EditTextPreference pref3 = new EditTextPreference(context);
    pref3.setSummary("Longitude token");
    pref3.setKey("longitude_token");
    pref3.setTitle("Longitude authorization token");
    pref3.setDefaultValue("passsw0rd");
    screen.addPreference(pref3);
  }
}
