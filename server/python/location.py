import urllib
import ioutils
import cgi
import time
import location_pb2

_START_PAGE = 'index.html'

class BadCoordsException(Exception):
  def __init__(self, error):
    self.error = error

class LocationManager(object):
  def __init__(self):
    self._locations = {}

  def _ValidateCoords(self, lat, lng, accuracy):
    if lat < -90 or lat > 90:
      raise BadCoordsException('Bad latitude: %.3f' % lat)
    if lng < -180 or lng > 180:
      raise BadCoordsException('Bad longitude: %.3f' % lng)
    if accuracy < 0 or accuracy > 10000:
      raise BadCoordsException('Bad accuracy: %.3f' % accuracy)

  def HandleUpdateRequest(self, args):
    name = unicode(args['id'], 'utf-8')
    lat = float(args['lat'])
    lng = float(args['lng'])
    accuracy = float(args.get('acc','0.0'))
    timestamp = long(args.get('time', '0.0'))
    now = time.time() * 1000
    if timestamp > now:
      timestamp = now
    self._ValidateCoords(lat, lng, accuracy)
    self._locations[name] = (lat, lng, accuracy, timestamp)
    return 'ok', 'text/plain'
    
  def HandleShowRequest(self, args):
    html = ioutils.ReadFile(_START_PAGE)
    html = html.replace('@@LOCATIONS@@', self._GetLocationsJson())
    return html, 'text/html'

  def HandleGetLocRequest(self, args):
    output = args.get('output', 'json')
    if output == 'proto':
      return self._GetLocationsProto(), 'application/octet-stream'
    return self._GetLocationsJson(), 'text/javascript'

  def _GetLocationsJson(self):
    result = []
    for name, location in self._locations.iteritems():
      # TODO: proper escaping here
      result.append('\'%s\': {\'lat\': %.5f, \'lng\': %.5f, \'accuracy\': %.5f, \'time\': %d}' % (cgi.escape(name.encode('utf-8')), location[0], location[1], location[2], location[3]))
    return '{' + ', '.join(result) + '}'

  def _GetLocationsProto(self):
    result = location_pb2.LocationInfo()
    for name, location in self._locations.iteritems():
      loc = result.location.add()
      loc.name = name
      loc.lat = location[0]
      loc.lng = location[1]
      loc.accuracy = location[2]
      loc.time = location[3]
    return result.SerializeToString()

