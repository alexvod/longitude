package org.ushmax.mapviewer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.alexvod.longitude.LongitudeOverlay;
import org.alexvod.longitude.LongitudeUiModule;
import org.ushmax.common.Factory;
import org.ushmax.common.Registry;
import org.ushmax.fetcher.AsyncHttpFetcher;
import org.ushmax.fetcher.HttpFetcher;
import org.ushmax.fetcher.HttpFetcherImpl;
import org.ushmax.mapviewer.longitude.KeyStoreData;

public class Plugin implements AbstractPlugin {
  private KeyStore loadKeyStore() {
    try {
      KeyStore trusted;
      trusted = KeyStore.getInstance("BKS");
      InputStream in = new ByteArrayInputStream(KeyStoreData.DATA);
      trusted.load(in, "123456".toCharArray());
      return trusted;
    } catch (KeyStoreException e) {
      throw new RuntimeException(e);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);      
    } catch (CertificateException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void onLoad(final MapViewerApp app) {
    Registry<Overlay, ActivityData> registry = app.overlayRegistry;
    registry.register("longitude", new Factory<Overlay, ActivityData>() {
      public Overlay create(ActivityData activityData) {
        HttpFetcher httpFetcher = new HttpFetcherImpl(loadKeyStore());
        AsyncHttpFetcher asyncHttpFetcher = new AsyncHttpFetcher(httpFetcher, app.taskDispatcher);
        return new LongitudeOverlay(app, app.getResources(), asyncHttpFetcher, activityData.uiController);
      }
    });
    
    Factory<UiModule, ActivityData> uiModuleFactory = new Factory<UiModule, ActivityData>() {
      @Override
      public UiModule create(ActivityData activityData) {
        return new LongitudeUiModule();
      }
    };
    app.uiModules.add(uiModuleFactory);
  }

  public void onUnLoad(MapViewerApp app) {
    Registry<Overlay, ActivityData> registry = app.overlayRegistry;
    registry.unregister("longitude");
  }
}
