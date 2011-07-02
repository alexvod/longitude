package org.alexvod;

import org.ushmax.android.AndroidLogger;
import org.ushmax.android.SettingsHelper;
import org.ushmax.common.Factory;
import org.ushmax.common.ITaskDispatcher;
import org.ushmax.common.Logger;
import org.ushmax.common.LoggerFactory;
import org.ushmax.common.QueuedTaskDispatcher;
import org.ushmax.fetcher.AsyncHttpFetcher;
import org.ushmax.fetcher.HttpFetcher;
import org.ushmax.fetcher.HttpFetcherImpl;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class LongitudeApp extends Application {
  public String initializationError; 

  // Objects
  public SharedPreferences prefs;
  public ITaskDispatcher taskDispatcher;
  public HttpFetcher httpFetcher;
  public AsyncHttpFetcher asyncHttpFetcher;

  public LongitudeApp() {
    super();
    System.loadLibrary("nativeutils");
    LoggerFactory.setLoggerFactory(new Factory<Logger, Class<?>>() {
      public Logger create(Class<?> clazz) {
        return new AndroidLogger(clazz.getSimpleName());
      }});
  }

  @Override
  public void onCreate() {
    super.onCreate();
    create();
  }
  
  private void create() {
    prefs = PreferenceManager.getDefaultSharedPreferences(this);

    int numNetworkThreads = SettingsHelper.getIntPref(prefs, "num_network_threads", 4);
    taskDispatcher = new QueuedTaskDispatcher(numNetworkThreads);
    httpFetcher = new HttpFetcherImpl(null);
    asyncHttpFetcher = new AsyncHttpFetcher(httpFetcher, taskDispatcher);
    taskDispatcher.start();
  }
}
