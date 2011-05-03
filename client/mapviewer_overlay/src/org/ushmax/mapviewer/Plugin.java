package org.ushmax.mapviewer;

import org.alexvod.longitude.LongitudeOverlay;
import org.alexvod.longitude.LongitudeUiModule;
import org.ushmax.common.Factory;
import org.ushmax.common.Registry;

public class Plugin implements AbstractPlugin {

  public void onLoad(final MapViewerApp app) {
    Registry<Overlay, ActivityData> registry = app.overlayRegistry;
    registry.register("longitude", new Factory<Overlay, ActivityData>() {
      public Overlay create(ActivityData activityData) {
        return new LongitudeOverlay(app, app.getResources(), app.asyncHttpFetcher, activityData.uiController);
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
