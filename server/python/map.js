var map = null;
var iconBase = 'http://maps.google.com/mapfiles/ms/micons/';
var refreshInterval = 5000; // ms
var allMarkers = {};

function initialize() {
  map = new GMap2(document.getElementById("map_canvas"));
  map.setCenter(new GLatLng(37.4, -122.08), 15);
  map.addControl(new GSmallMapControl());
  map.addControl(new GMapTypeControl());
  map.enableScrollWheelZoom();

  showLocations();
  setTimeout("refreshLocations()", refreshInterval);
}

function addNewMarker(name, coords) {
  var myicon = new GIcon();
  myicon.image = iconBase + "red-dot.png";
  myicon.iconSize = new GSize(32, 32);
  myicon.iconAnchor = new GPoint(16, 32);
  myicon.infoWindowAnchor = new GPoint(16, 32);
  var marker = new GMarker(new GLatLng(coords.lat, coords.lng), {"icon": myicon});
  marker.coords = coords;
  current_marker = marker;
  map.addOverlay(marker);
  allMarkers[name] = marker;
}

function showLocations() {
  for (name in locations) {
    addNewMarker(name, locations[name]);
  }
}

function updateLocations(newLocations) {
  for (name in locations) {
    var marker = allMarkers[name];
    if (!(name in newLocations)) {
      map.removeOverlay(marker);
      delete allMarkers[name];
      continue;
    }
    var coords = newLocations[name];
    marker.setLatLng(new GLatLng(coords.lat, coords.lng));
  }
  for (name in newLocations) {
    if (!(name in locations)) {
      addNewMarker(name, newLocations[name]);
    }
  }
}

function refreshLocations() {
  var url = "/getloc";
  GDownloadUrl(url, function(data, responseCode) {
      if (responseCode != 200) {
          //alert('Server error: ' + data);
        return;
      }
      var newLocations = eval('(' + data + ')');
      updateLocations(newLocations);
    });
  setTimeout("refreshLocations()", refreshInterval);
}
