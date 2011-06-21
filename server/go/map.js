var map = null;
var iconBase = 'http://maps.google.com/mapfiles/ms/micons/';
var allMarkers = {};

function initialize() {
  map = new GMap2(document.getElementById("map_canvas"));
  map.setCenter(new GLatLng(37.4, -122.08), 15);
  map.addControl(new GSmallMapControl());
  map.addControl(new GMapTypeControl());
  map.enableScrollWheelZoom();

  showLocations();
  // A hack to avoid forever-loading page.
  setTimeout("pollForUpdates()", 500);
}

function formatTimestamp(timestamp) {
  var date = new Date(timestamp);
  return date.toString();
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
  marker.text = name + ' at ' + formatTimestamp(coords.time);
  GEvent.addListener(marker, "click", function() {
      var htmlText = marker.text;
      marker.openInfoWindow(htmlText);
    });
  map.addOverlay(marker);
  allMarkers[name] = marker;
}

function showLocations() {
  for (name in locations) {
    addNewMarker(name, locations[name]);
  }
}

function updateLocations(newLocations) {
  for (name in allMarkers) {
    var marker = allMarkers[name];
    if (!(name in newLocations)) {
      map.removeOverlay(marker);
      delete allMarkers[name];
      continue;
    }
    var coords = newLocations[name];
    marker.text = name + ' at ' + formatTimestamp(coords.time);
    marker.setLatLng(new GLatLng(coords.lat, coords.lng));
  }
  for (name in newLocations) {
    if (!(name in allMarkers)) {
      addNewMarker(name, newLocations[name]);
    }
  }
}

function handler() {
  // Ignore intermediate state.
  if (this.readyState != 4) {
    return;
  }
  var status;
  if (this.status == 200) {
    newLocations = eval("(" + this.responseText + ")");
    updateLocations(newLocations)
  }
  pollForUpdates()
}

function pollForUpdates() {
  var client = new XMLHttpRequest();
  client.onreadystatechange = handler;
  client.open("GET", "/poll", true)
  client.send();
}
