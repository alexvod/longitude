var map = null;
var current_marker = null;
var icon_base = 'http://maps.google.com/mapfiles/ms/micons/';
var known_icons = ["red-dot.png", "blue-dot.png", "green-dot.png", "yellow-dot.png", "purple-dot.png"];

function initialize() {
  map = new GMap2(document.getElementById("map_canvas"));
  map.setCenter(new GLatLng(37.4, -122.08), 15);
  map.addControl(new GSmallMapControl());
  map.addControl(new GMapTypeControl());
  map.enableScrollWheelZoom();

  showLocations();
}

function addNewMarker(placemark) {
  var myicon = new GIcon();
  myicon.image = icon_base + "red-dot.png";
  myicon.iconSize = new GSize(32, 32);
  myicon.iconAnchor = new GPoint(16, 32);
  myicon.infoWindowAnchor = new GPoint(16, 32);
  var marker = new GMarker(new GLatLng(placemark.lat, placemark.lng), {"icon": myicon});
  marker.placemark = placemark;
  current_marker = marker;
  map.addOverlay(marker);
}

function showLocations() {
  for (name in locations) {
    addNewMarker(locations[name]);
  }
}


